package com.it.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.it.model.system.SysMenu;
import com.it.vo.system.AssginMenuVo;

import java.util.List;

/**
 * <p>
 * 菜单表 服务类
 * </p>
 *
 * @author 杨振华
 * @since 2023-06-22
 */
public interface SysMenuService extends IService<SysMenu> {
    List<SysMenu> findNodes();
    void removeByMenuId(Long Id);
    List<SysMenu> findMenuByRoleId(Long roleId);
    void doAssign(AssginMenuVo assignMenuVo);
}
