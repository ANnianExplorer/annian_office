package com.it.service.impl;

import com.it.mapper.SysUserMapper;
import com.it.model.system.SysUser;
import com.it.service.SysUserRoleService;
import com.it.service.SysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.vo.system.AssginRoleVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

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
}
