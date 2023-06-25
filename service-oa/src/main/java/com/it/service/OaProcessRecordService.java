package com.it.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.it.model.process.ProcessRecord;

/**
 * <p>
 * 审批记录 服务类
 * </p>
 *
 * @author 杨振华
 * @since 2023-06-25
 */
public interface OaProcessRecordService extends IService<ProcessRecord> {

    void record(Long processId,Integer status,String description);

}
