package com.it.service.impl;

import com.it.ResultCodeEnum;
import com.it.config.ANnianException;
import com.it.custom.CustomUser;
import com.it.custom.UserDetailsService;
import com.it.model.system.SysUser;
import com.it.service.SysMenuService;
import com.it.service.SysUserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 杨振华
 * @since 2023/6/23
 *
 * 接口在spring-security的custom文件夹下的UserDetailsService
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysMenuService sysMenuService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserService.getUserByName(username);
        if(null == sysUser) {
            throw new UsernameNotFoundException("用户名不存在！");
        }

        if(sysUser.getStatus() == 0) {
            throw new ANnianException(ResultCodeEnum.ACCOUNT_STOP);
        }
        List<String> userPermsList = sysMenuService.findUserPermsByUserId(sysUser.getId());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String perm : userPermsList) {
            authorities.add(new SimpleGrantedAuthority(perm.trim()));
        }
        return new CustomUser(sysUser, authorities);
    }
}
