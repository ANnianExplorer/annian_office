package com.it.service;

import com.it.model.system.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;
import com.it.vo.system.AssginRoleVo;

import java.util.Map;

public interface SysRoleService extends IService<SysRole> {

    Map<String,Object> findRoleByUserId(Long userId);

    void doAssign(AssginRoleVo assginRoleVo);
}