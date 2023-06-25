package com.it.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.mapper.OaProcessTemplateMapper;
import com.it.model.process.ProcessTemplate;
import com.it.model.process.ProcessType;
import com.it.service.OaProcessService;
import com.it.service.OaProcessTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.service.OaProcessTypeService;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 审批模板 服务实现类
 * </p>
 *
 * @author 杨振华
 * @since 2023-06-24
 */
@Service
public class OaProcessTemplateServiceImpl extends ServiceImpl<OaProcessTemplateMapper, ProcessTemplate> implements OaProcessTemplateService {

    @Resource
    private OaProcessTypeService processTypeService;

    @Resource
    private OaProcessService processService;

    @Resource
    private OaProcessTemplateMapper processTemplateMapper;

    @Override
    public IPage<ProcessTemplate> selectPage(Page<ProcessTemplate> pageParam) {
        // 调用mapper方法分页查询
        Page<ProcessTemplate> processTemplatePage = baseMapper.selectPage(pageParam, null);

        // 第一步查询返回分页数据，从分页数据回去列表list集合
        List<ProcessTemplate> processTemplateList = processTemplatePage.getRecords();

        // 遍历list集合，得到每一个对象的审批类型id
        for (ProcessTemplate processTemplate : processTemplateList) {
            Long processTypeId = processTemplate.getProcessTypeId();
            // 根据审批类型id，查询获取对应名称
            LambdaQueryWrapper<ProcessType> processTypeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            processTypeLambdaQueryWrapper
                    .eq(ProcessType::getId,processTypeId);
            ProcessType processType = processTypeService.getOne(processTypeLambdaQueryWrapper);
            if (processType == null)continue;
            // 完成最终封装
            processTemplate.setProcessTypeName(processType.getName());
        }

        return processTemplatePage;
    }

    @Override
    public void publish(Long id) {
        ProcessTemplate processTemplate = this.getById(id);
        processTemplate.setStatus(1);
        baseMapper.updateById(processTemplate);
        //TODO 后续完善流程部署定义
        if (!ObjectUtils.isEmpty(processTemplate.getProcessDefinitionPath())){
            processService.deployByZip(processTemplate.getProcessDefinitionPath());
        }
    }
}
