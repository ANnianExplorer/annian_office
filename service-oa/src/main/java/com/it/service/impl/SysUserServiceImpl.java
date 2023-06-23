package com.it.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.it.mapper.SysUserMapper;
import com.it.model.system.SysUser;
import com.it.service.SysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author 杨振华
 * @since 2023-06-22
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public void updateStatus(Long id, Integer status) {
        // 1.根据用户id查询
        SysUser sysUser = baseMapper.selectById(id);

        // 2.设置要修改的状态值
        sysUser.setStatus(status);

        // 3.调用方法修改
        baseMapper.updateById(sysUser);
    }

    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return {@link SysUser}
     */
    @Override
    public SysUser getUserByName(String username) {
        LambdaQueryWrapper<SysUser> sysUserLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysUserLambdaQueryWrapper
                .eq(SysUser::getUsername,username);
        return baseMapper.selectOne(sysUserLambdaQueryWrapper);
    }
}

