package com.it.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.it.JwtHelper;
import com.it.Result;
import com.it.model.system.SysUser;
import com.it.service.SysUserService;
import com.it.vo.wechat.BindPhoneVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Api("微信网页授权接口")
@Controller
@RequestMapping("/admin/wechat")
@Slf4j
@CrossOrigin // 跨域
public class WechatController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private WxMpService wxMpService;

    @Value("${wechat.userInfoUrl}")
    private String userInfoUrl;

    @ApiOperation("微信网页授权")
    @GetMapping("/authorize")
    public String authorize(@RequestParam("returnUrl") String returnUrl, HttpServletRequest request) {
        // 授权路径   固定值[授权类型]   授权成功后的路径[guiguoa -- #]
        String authorizationUrl = null;
        try {
            authorizationUrl = wxMpService.getOAuth2Service()
                    .buildAuthorizationUrl(
                            userInfoUrl,
                            WxConsts.OAuth2Scope.SNSAPI_USERINFO,
                            URLEncoder.encode(returnUrl.replace("guiguoa", "#"),"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        /**
         * //由于授权回调成功后，要返回原地址路径，
         * 原地址路径带“#”号，当前returnUrl获取带“#”的url获取不全，
         * 因此前端把“#”号替换为“guiguoa”了，这里要还原一下
         */
        log.info("【微信网页授权】获取code,redirectURL={}", authorizationUrl);
        return "redirect:" + authorizationUrl;
    }

    @ApiOperation("用户信息")
    @GetMapping("/userInfo")
    public String userInfo(@RequestParam("code") String code,
                           @RequestParam("state") String returnUrl) throws Exception {
        log.info("【微信网页授权】code={}", code);
        log.info("【微信网页授权】state={}", returnUrl);
        // 获取accessToken
        WxOAuth2AccessToken accessToken = wxMpService.getOAuth2Service().getAccessToken(code);
        // 获取openId
        String openId = accessToken.getOpenId();
        log.info("【微信网页授权】openId={}", openId);
        // 获取微信用户信息
        WxOAuth2UserInfo wxMpUser = wxMpService.getOAuth2Service().getUserInfo(accessToken,null);
        log.info("【微信网页授权】wxMpUser={}", JSON.toJSONString(wxMpUser));

        // 根据用户id查用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getOpenId,openId);
        SysUser sysUser = sysUserService.getOne(wrapper);

        String token = "";

        if (sysUser != null) {
            token = JwtHelper.createToken(sysUser.getId(),sysUser.getUsername());
        }
        if(returnUrl.indexOf("?") == -1) {
            return "redirect:" + returnUrl + "?token=" + token + "&openId=" + openId;
        } else {
            return "redirect:" + returnUrl + "&token=" + token + "&openId=" + openId;
        }
    }

    @ApiOperation(value = "微信账号绑定手机")
    @PostMapping("bindPhone")
    @ResponseBody
    public Result bindPhone(@RequestBody BindPhoneVo bindPhoneVo) {
        // 1.根据手机号查询数据库
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPhone,bindPhoneVo.getPhone());
        SysUser sysUser = sysUserService.getOne(wrapper);

        // 2.如果存在更新openId
        if (sysUser != null) {
            sysUser.setOpenId(bindPhoneVo.getOpenId());
            sysUserService.updateById(sysUser);

            String token = JwtHelper.createToken(sysUser.getId(),sysUser.getUsername());
            return Result.ok(token);
        }else {
            return Result.fail("手机号不存在，请联系管理员修改");
        }
    }
}