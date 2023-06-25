package com.it.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.it.mapper.OaProcessTypeMapper;
import com.it.model.process.ProcessTemplate;
import com.it.model.process.ProcessType;
import com.it.service.OaProcessTemplateService;
import com.it.service.OaProcessTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 审批类型 服务实现类
 * </p>
 *
 * @author 杨振华
 * @since 2023-06-24
 */
@Service
public class OaProcessTypeServiceImpl extends ServiceImpl<OaProcessTypeMapper, ProcessType> implements OaProcessTypeService {

    @Resource
    private OaProcessTypeService processTypeService;

    @Resource
    private OaProcessTemplateService processTemplateService;

    @Override
    public List<ProcessType> findProcessType() {
        // 1.查询所有审批分类
        List<ProcessType> processTypeList = baseMapper.selectList(null);
        // 2.遍历得到每个审批分类，根据id查询对应的审批模版
        for (ProcessType processType : processTypeList) {
            Long processTypeId = processType.getId();

            LambdaQueryWrapper<ProcessTemplate> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper
                    .eq(ProcessTemplate::getProcessTypeId, processTypeId);
            List<ProcessTemplate> processTemplateList = processTemplateService.list(queryWrapper);

            // 3.分类id查询对应审批模版数据list封装到每个审批分类对象里面
            processType.setProcessTemplateList(processTemplateList);
        }
        return processTypeList;
    }
}
