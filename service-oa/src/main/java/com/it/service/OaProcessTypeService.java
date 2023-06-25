package com.it.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.it.model.process.ProcessType;

import java.util.List;

/**
 * <p>
 * 审批类型 服务类
 * </p>
 *
 * @author 杨振华
 * @since 2023-06-24
 */
public interface OaProcessTypeService extends IService<ProcessType> {

    List<ProcessType> findProcessType();
}
