package com.it.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.it.model.system.SysUser;

import java.util.Map;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author 杨振华
 * @since 2023-06-22
 */
public interface SysUserService extends IService<SysUser> {
    void updateStatus(Long id,Integer status);

    SysUser getUserByName(String username);

    Map<String, Object> getCurrentUser();
}
