package com.it.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.custom.LoginUserInfoHelper;
import com.it.mapper.OaProcessTemplateMapper;
import com.it.model.process.Process;
import com.it.mapper.OaProcessMapper;
import com.it.model.process.ProcessRecord;
import com.it.model.process.ProcessTemplate;
import com.it.model.system.SysUser;
import com.it.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.vo.process.ApprovalVo;
import com.it.vo.process.ProcessFormVo;
import com.it.vo.process.ProcessQueryVo;
import com.it.vo.process.ProcessVo;
import com.sun.glass.ui.monocle.LinuxArch;
import org.activiti.bpmn.model.*;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.ConnectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * <p>
 * 审批类型 服务实现类
 * </p>
 *
 * @author 杨振华
 * @since 2023-06-25
 */
@Service
public class OaProcessServiceImpl extends ServiceImpl<OaProcessMapper, Process> implements OaProcessService {
    @Resource
    private RepositoryService repositoryService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private OaProcessTemplateService processTemplateService;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private HistoryService historyService;

    @Resource
    private OaProcessRecordService processRecordService;

    @Resource
    private MessageService messageService;

    @Override
    public IPage<ProcessVo> selectPage(Page<ProcessVo> pageParam, ProcessQueryVo processQueryVo) {
        IPage<ProcessVo> pageModel = baseMapper.selectPage(pageParam, processQueryVo);
        return pageModel;
    }

    @Override
    public void deployByZip(String deployPath) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(deployPath);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        // 部署
        Deployment deployment = repositoryService
                .createDeployment()
                .addZipInputStream(zipInputStream)
                .deploy();
        System.out.println(deployment.getId());
        System.out.println(deployment.getName());
    }

    /**
     * 启动流程审批
     * @param processFormVo
     */
    @Override
    public void start(ProcessFormVo processFormVo) {
        // 1.根据当前用户id获取用户信息
        SysUser sysUser = sysUserService.getById(LoginUserInfoHelper.getUserId());
        // 2.根据审批模版id把模版信息查询
        ProcessTemplate processTemplate = processTemplateService.getById(processFormVo.getProcessTemplateId());
        // 3.保存提交审批信息到业务表
        Process process = new Process();
        BeanUtils.copyProperties(processFormVo,process);
        // 其他值
        String workNo = System.currentTimeMillis() + "";
        process.setProcessCode(workNo);
        process.setUserId(LoginUserInfoHelper.getUserId());
        process.setFormValues(processFormVo.getFormValues());
        process.setTitle(sysUser.getName() + "发起" + processTemplate.getName() + "申请");
        process.setStatus(1);
        baseMapper.insert(process);

        // 4.启动流程实例RuntimeService
        // 4.1流程定义key
        String processDefinitionKey = processTemplate.getProcessDefinitionKey();
        // 4.2业务key
        String businessKey = String.valueOf(process.getId());
        // 4.3流程参数，from表单json数据，转为map
        String formValues = processFormVo.getFormValues();
        // formData
        JSONObject jsonObject = JSON.parseObject(formValues);
        JSONObject formData = jsonObject.getJSONObject("formData");
        Map<String,Object> map = new HashMap<>();
        for(Map.Entry<String,Object> entry: formData.entrySet()){
            map.put(entry.getKey(),entry.getValue());
        }
        Map<String,Object> mapVo = new HashMap<>();
        mapVo.put("data",map);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, mapVo);

        // 5.查询下一个审批人是谁
        // 审批人可能有多个
        List<Task> taskList = this.getCurrentTaskList(processInstance.getId());
        List<String> nameList = new ArrayList<>();
        for (Task task : taskList) {
            // 审批人
            String assignee = task.getAssignee();
            SysUser user = sysUserService.getUserByName(assignee);
            String userName = user.getName();
            nameList.add(userName);
            //TODO 6.推送消息
            messageService.pushPendingMessage(process.getId(), user.getId(), task.getId());

        }
        process.setProcessInstanceId(processInstance.getId());
        process.setDescription("等待" + StringUtils.join(nameList.toArray(),",") + "审批");
        // 7.业务和流程进行最终关联,更新oa_process
        baseMapper.updateById(process);

        // 记录操作审批信息记录
        processRecordService.record(process.getId(),1,"发起申请");
    }


    /**
     * 查询待处理任务列表
     * @param pageParam
     * @return
     */
    @Override
    public IPage<ProcessVo> findPending(Page<Process> pageParam) {
        // 1.封装查询的条件，根据当前登录的用户名称
        TaskQuery taskQuery = taskService.createTaskQuery()
                .taskAssignee(LoginUserInfoHelper.getUsername())
                .orderByTaskCreateTime().desc();

        // 2.调用方法进行分页条件查询，返回list，待办任务集合
        // listPage: 开始位置   每页记录数
        int begin = (int) ((pageParam.getCurrent() - 1) * (pageParam.getSize()));
        int size = (int) pageParam.getSize();
        List<Task> taskList = taskQuery.listPage(begin, size);
        long totalCount = taskQuery.count();

        // 3.封装返回List<Task> 到 List<ProcessVo>中
        List<ProcessVo> processVoList = new ArrayList<>();
        for (Task task : taskList) {
            // 从task里面获取流程实例id
            String processInstanceId = task.getProcessInstanceId();
            // 根据id获取实例
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            // 从实例中获取业务key，就是processId
            String businessKey = processInstance.getBusinessKey();
            if (businessKey == null) {
                continue;
            }
            // 根据业务key获取process对象
            Process process = baseMapper.selectById(Long.parseLong(businessKey));

            // 复制到processVO
            ProcessVo processVo = new ProcessVo();
            BeanUtils.copyProperties(process,processVo);
            processVo.setTaskId(task.getId());

            // 封装返回List<Task> 到 List<ProcessVo>中
            processVoList.add(processVo);
        }

        // 4.封装返回的IPage对象
        IPage<ProcessVo> page = new Page<>(pageParam.getCurrent(),pageParam.getSize(),totalCount);
        page.setRecords(processVoList);
        return page;
    }

    /**
     * 查看审批详情
     * @param id
     * @return
     */
    @Override
    public Map<String, Object> show(Long id) {
        // 1.根据流程id获取流程信息Process
        Process process = baseMapper.selectById(id);
        // 2.根据流程id获取记录信息
        LambdaQueryWrapper<ProcessRecord> processRecordLambdaQueryWrapper = new LambdaQueryWrapper<>();
        processRecordLambdaQueryWrapper
                .eq(ProcessRecord::getProcessId,id);
        List<ProcessRecord> processRecordList = processRecordService.list(processRecordLambdaQueryWrapper);

        // 3.根据模版id获取模版信息
        ProcessTemplate processTemplate = processTemplateService.getById(process.getProcessTemplateId());

        // 4.判断当前用户是否可以进行审批
        // 可以看到信息的不一定可以审批，审批之后不再审批
        boolean isApprove = false;// 判断条件
        List<Task> currentTaskList = this.getCurrentTaskList(process.getProcessInstanceId());
        for (Task task : currentTaskList) {
            String username = LoginUserInfoHelper.getUsername();
            // 判断任务当前审批人是否是当前用户
            if (task.getAssignee().equals(username)){
                isApprove = true;
            }
        }

        // 5.数据封装返回
        Map<String, Object> map = new HashMap<>();
        map.put("process", process);
        map.put("processRecordList", processRecordList);
        map.put("processTemplate", processTemplate);
        map.put("isApprove", isApprove);
        return map;
    }

    /**
     * 审批
     * @param approvalVo
     */
    @Override
    public void approve(ApprovalVo approvalVo) {
        // 1.从approvalVo中获取任务id，根据任务id获取流程变量
        String taskId = approvalVo.getTaskId();
        Map<String, Object> variables = taskService.getVariables(taskId);
        for (Map.Entry<String, Object> entry: variables.entrySet()){
            System.out.println(entry.getKey() + "---" + entry.getValue());
        }

        // 2.判断审批状态值
        // 2.1 =1 审批通过
        if (approvalVo.getStatus() == 1){
            Map<String, Object> variable = new HashMap<>();
            taskService.complete(taskId,variable);
        }else {
            // 2.2 =-1 审批驳回
            this.endTask(taskId);
        }
        // 3.记录审批相关信息
        String description = approvalVo.getStatus() == 1 ? "已经通过" : "驳回";
        processRecordService.record(approvalVo.getProcessId(),
                approvalVo.getStatus(),description);

        // 4.查询下一个审批人，更新process
        Process process = baseMapper.selectById(approvalVo.getProcessId());
        List<Task> taskList = this.getCurrentTaskList(process.getProcessInstanceId());
        if (!CollectionUtils.isEmpty(taskList)) {
            List<String> assigneeList = new ArrayList<>();
            for (Task task : taskList) {
                String assignee = task.getAssignee();
                SysUser sysUser = sysUserService.getUserByName(assignee);
                assigneeList.add(sysUser.getName());
                // TODO 公众号推送
                messageService.pushProcessedMessage(process.getId(), sysUser.getId(), Integer.valueOf(task.getId()));
            }
            process.setStatus(1);
            process.setDescription("等待" + StringUtils.join(assigneeList.toArray(),",") + "审批");
        }else {
            if (approvalVo.getStatus() == 1) {
                process.setDescription("审批完成 (通过)");
                process.setStatus(2);
            }else {
                process.setDescription("审批完成 (驳回)");
                process.setStatus(-1);
            }
        }
        baseMapper.updateById(process);
    }

    /**
     * 查询已处理
     * @return
     */
    /*@Override
    public IPage<ProcessVo> findProcessed(Page<Process> pageParam) {
        // 封装查询条件
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(LoginUserInfoHelper.getUsername())
                .finished().orderByTaskCreateTime().desc();

        // 调研方法条件分页查询，返回list
        int begin = (int) ((pageParam.getCurrent() - 1)*(pageParam.getSize()));
        int size = (int) pageParam.getSize();
        long totalCount = query.count();
        List<HistoricTaskInstance> taskInstances = query.listPage(begin, size);

        List<ProcessVo> processVoList = new ArrayList<>();
        // 变量返回list，封装ProcessVo
        for (HistoricTaskInstance taskInstance : taskInstances) {
            String processInstanceId = taskInstance.getProcessInstanceId();
            LambdaQueryWrapper<Process> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Process::getProcessInstanceId,processInstanceId);
            Process process = baseMapper.selectOne(wrapper);
            // process ---> processVo
            ProcessVo processVo = new ProcessVo();
            BeanUtils.copyProperties(process, processVo);

            processVoList.add(processVo);
        }

        // IPage封装数据
        IPage<ProcessVo> pageModel = new Page<>(pageParam.getCurrent(),pageParam.getSize(),totalCount);

        return pageModel;
    }*/
    @Override
    public IPage<ProcessVo> findProcessed(Page<Process> pageParam) {
        // 根据当前人的ID查询
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().taskAssignee(LoginUserInfoHelper.getUsername()).finished().orderByTaskCreateTime().desc();
        List<HistoricTaskInstance> list = query.listPage((int) ((pageParam.getCurrent() - 1) * pageParam.getSize()), (int) pageParam.getSize());
        long totalCount = query.count();

        List<ProcessVo> processList = new ArrayList<>();
        for (HistoricTaskInstance item : list) {
            String processInstanceId = item.getProcessInstanceId();
            Process process = this.getOne(new LambdaQueryWrapper<Process>().eq(Process::getProcessInstanceId, processInstanceId));
            ProcessVo processVo = new ProcessVo();
            BeanUtils.copyProperties(process, processVo);
            processVo.setTaskId("0");
            processList.add(processVo);
        }
        IPage<ProcessVo> page = new Page<ProcessVo>(pageParam.getCurrent(), pageParam.getSize(), totalCount);
        page.setRecords(processList);
        return page;
    }

    /**
     * 已发起
     * @param pageParam
     * @return
     */
    @Override
    public IPage<ProcessVo> findStarted(Page<ProcessVo> pageParam) {
        ProcessQueryVo processQueryVo = new ProcessQueryVo();
        processQueryVo.setUserId(LoginUserInfoHelper.getUserId());
        IPage<ProcessVo> page = baseMapper.selectPage(pageParam, processQueryVo);
        for (ProcessVo item : page.getRecords()) {
            item.setTaskId("0");
        }
        return page;
    }

    /**
     * 结束流程任务
     * @param taskId
     */
    private void endTask(String taskId) {
        // 1.获取任务
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        // 2.获取流程定义模型BpmnModel
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        // 3.获取结束流向节点
        List<EndEvent> endEventList = bpmnModel.getMainProcess().findFlowElementsOfType(EndEvent.class);
        if (CollectionUtils.isEmpty(endEventList)){
            return;
        }
        FlowNode endFlowNode = (FlowNode) endEventList.get(0);

        // 4.获取当前流向节点
        FlowNode currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());

        //  临时保存当前活动的原始方向
        List originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());

        // 5.清理当前流动方法
        currentFlowNode.getOutgoingFlows().clear();

        // 6.创建新的流向
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlow");
        newSequenceFlow.setSourceFlowElement(currentFlowNode);
        newSequenceFlow.setTargetFlowElement(endFlowNode);

        // 7.当前节点指向新方向
        List newSequenceFlowList  = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);
        currentFlowNode.setOutgoingFlows(newSequenceFlowList);
        // 8.完成
        taskService.complete(taskId);
    }

    /**
     * 当前任务列表
     * @param id
     * @return
     */
    private List<Task> getCurrentTaskList(String id) {
        List<Task> list = taskService.createTaskQuery().processInstanceId(id).list();
        return list;
    }
}
