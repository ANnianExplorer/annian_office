package com.it.service;

import com.it.model.wechat.Menu;
import com.baomidou.mybatisplus.extension.service.IService;
import com.it.vo.wechat.MenuVo;

import java.util.List;

/**
 * <p>
 * 菜单 服务类
 * </p>
 *
 * @author 杨振华
 * @since 2023-06-26
 */
public interface WechatMenuService extends IService<Menu> {

    List<MenuVo> findMenuInfo();

    void syncMenu();

    void removeMenu();
}
