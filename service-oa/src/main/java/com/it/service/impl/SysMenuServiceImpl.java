package com.it.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.it.config.ANnianException;
import com.it.mapper.SysMenuMapper;
import com.it.model.system.SysMenu;
import com.it.model.system.SysRoleMenu;
import com.it.service.SysMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.service.SysRoleMenuService;
import com.it.util.MenuHelper;
import com.it.vo.system.AssginMenuVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单表 服务实现类
 * </p>
 *
 * @author 杨振华
 * @since 2023-06-22
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {
    @Resource
    private SysRoleMenuService sysRoleMenuService;

    @Override
    public List<SysMenu> findNodes() {
        // 1.查询所有菜单数据
        List<SysMenu> sysMenus = baseMapper.selectList(null);
        // 2.构建树形结构
        List<SysMenu> sysMenuList = MenuHelper.buildTree(sysMenus);
        return sysMenuList;
    }

    @Override
    public void removeByMenuId(Long Id) {
        // 1.判断当前菜单有没有子菜单
        LambdaQueryWrapper<SysMenu> sysMenuLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysMenuLambdaQueryWrapper
                .eq(SysMenu::getParentId,Id);
        Integer count = baseMapper.selectCount(sysMenuLambdaQueryWrapper);
        if (count > 0){
            throw new ANnianException(201,"菜单不能删除！");
        }
        baseMapper.deleteById(Id);
    }

    @Override
    public List<SysMenu> findMenuByRoleId(Long roleId) {
        // 1.查询所有的菜单  状态可用
        LambdaQueryWrapper<SysMenu> sysMenuLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysMenuLambdaQueryWrapper
                .eq(SysMenu::getStatus,1);
        List<SysMenu> sysMenuList = baseMapper.selectList(sysMenuLambdaQueryWrapper);

        // 2.根据角色id 查询角色对应的菜单id
        LambdaQueryWrapper<SysRoleMenu> sysMenuRoleWrapper = new LambdaQueryWrapper<>();
        sysMenuRoleWrapper
                .eq(SysRoleMenu::getRoleId,roleId);
        List<SysRoleMenu> sysRoleMenus = sysRoleMenuService.list(sysMenuRoleWrapper);

        // 3.根据获取的菜单id得到菜单对象
        // 3.1集合比较，相同则封装
        List<Long> menuIdList = sysRoleMenus.stream().map(c -> c.getMenuId()).collect(Collectors.toList());
        sysMenuList.stream().forEach(item -> {
            if (menuIdList.contains(item.getId())){
                item.setSelect(true);
            }else {
                item.setSelect(false);
            }
        });

        // 4.返回规定树形格式的集合
        List<SysMenu> list = MenuHelper.buildTree(sysMenuList);
        return list;
    }

    @Override
    public void doAssign(AssginMenuVo assignMenuVo) {
        // 1.根据角色id删除菜单角色表，分配数据
        LambdaQueryWrapper<SysRoleMenu> sysRoleMenuLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysRoleMenuLambdaQueryWrapper
                .eq(SysRoleMenu::getRoleId,assignMenuVo.getRoleId());
        sysRoleMenuService.remove(sysRoleMenuLambdaQueryWrapper);

        // 2.从参数中获取角色新分配的菜单id列表，
        // 进行遍历，把每个id数据添加菜单角色表
        for (Long menuId : assignMenuVo.getMenuIdList()) {
            if (StringUtils.isEmpty(menuId)) continue;
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setMenuId(menuId);
            roleMenu.setRoleId(assignMenuVo.getRoleId());
            sysRoleMenuService.save(roleMenu);
        }
    }
}
