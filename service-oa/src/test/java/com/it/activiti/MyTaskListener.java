package com.it.activiti;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

/**
 * @author 杨振华
 * @since 2023/6/24
 */
public class MyTaskListener implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
        if (delegateTask.getName().equals("经理审批")){
            // 分配任务
            delegateTask.setAssignee("jack");
        }else if (delegateTask.getName().equals("人事审批")){
            delegateTask.setAssignee("tom");
        }
    }
}
