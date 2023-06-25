package com.it.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.MD5;
import com.it.Result;
import com.it.model.system.SysUser;
import com.it.service.SysUserService;
import com.it.vo.system.SysUserQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author 杨振华
 * @since 2023-06-22
 */
@Api(tags = "用户管理接口")
@RestController
@RequestMapping(value = "/admin/system/sysUser")
@CrossOrigin  //跨域
public class SysUserController {
    @Resource
    private SysUserService sysUserService;

    @PreAuthorize("hasAuthority('bnt.sysUser.list')")
    @ApiOperation("用户条件分页查询")
    @GetMapping("{page}/{limit}")
    public Result index(@PathVariable Long page,
                        @PathVariable Long limit,
                        SysUserQueryVo sysUserQueryVo){
        // 创建page对象
        Page<SysUser> pageInfo = new Page<>(page, limit);
        // 封装条件
        LambdaQueryWrapper<SysUser> sysUserLambdaQueryWrapper = new LambdaQueryWrapper<>();
        String username = sysUserQueryVo.getKeyword();
        String createTimeBegin = sysUserQueryVo.getCreateTimeBegin();
        String createTimeEnd = sysUserQueryVo.getCreateTimeEnd();
        if (!ObjectUtils.isEmpty(username)){
            sysUserLambdaQueryWrapper
                    .like(SysUser::getUsername,username);
        }
        if (!ObjectUtils.isEmpty(createTimeBegin)){
            sysUserLambdaQueryWrapper
                    .ge(SysUser::getCreateTime,createTimeBegin);
        }
        if (!ObjectUtils.isEmpty(createTimeEnd)){
            sysUserLambdaQueryWrapper
                    .le(SysUser::getCreateTime,createTimeEnd);
        }

        // 调用mp的方法
        Page<SysUser> sysUserPage = sysUserService.page(pageInfo, sysUserLambdaQueryWrapper);

        return Result.ok(sysUserPage);
    }

    @PreAuthorize("hasAuthority('bnt.sysUser.list')")
    @ApiOperation(value = "获取用户")
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        return Result.ok(user);
    }

    @PreAuthorize("hasAuthority('bnt.sysUser.add')")
    @ApiOperation(value = "保存用户")
    @PostMapping("save")
    public Result save(@RequestBody SysUser user) {
        // 对密码进行加密
        String newPassword = MD5.encrypt(user.getPassword());
        user.setPassword(newPassword);
        sysUserService.save(user);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('bnt.sysUser.update')")
    @ApiOperation(value = "更新用户")
    @PutMapping("update")
    public Result updateById(@RequestBody SysUser user) {
        sysUserService.updateById(user);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('bnt.sysUser.remove')")
    @ApiOperation(value = "删除用户")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        sysUserService.removeById(id);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('bnt.sysUser.status')")
    @ApiOperation(value = "更新状态")
    @GetMapping("updateStatus/{id}/{status}")
    public Result updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        sysUserService.updateStatus(id, status);
        return Result.ok();
    }

    /**
     * 员工端获取当前用户基本信息
     * @return
     */
    @ApiOperation(value = "员工端获取当前用户基本信息")
    @GetMapping("/getCurrentUser")
    public Result getCurrentUser() {
        Map<String,Object> map = sysUserService.getCurrentUser();
        return Result.ok(map);
    }
}

