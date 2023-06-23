package com.it.filter;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.JwtHelper;
import com.it.ResponseUtil;
import com.it.Result;
import com.it.ResultCodeEnum;
import com.it.custom.CustomUser;
import com.it.vo.system.LoginVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 过滤器
 *
 * @author 杨振华
 * @since 2023/6/23
 */
public class TokenLoginFilter extends UsernamePasswordAuthenticationFilter {

    private RedisTemplate redisTemplate;

    /**
     * 登录令牌过滤器
     *
     * @param authenticationManager 身份验证管理器
     */// 构造方法
    public TokenLoginFilter(AuthenticationManager authenticationManager,RedisTemplate redisTemplate){
        this.setAuthenticationManager(authenticationManager);
        this.setPostOnly(false);
        // 指定登录接口及提交方式，可以指定任意路径
        this.setRequiresAuthenticationRequestMatcher(
                new AntPathRequestMatcher("/admin/system/index/login","POST")
        );
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试验证
     *
     * @param req 要求事情
     * @param res res
     * @return {@link Authentication}
     * @throws AuthenticationException 身份验证异常
     */// 登录认证过程
    // 获取输入的用户名密码，调用方法认证
    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res) throws AuthenticationException {
        try {
            // 获取输入的用户信息
            LoginVo loginVo = new ObjectMapper().readValue(req.getInputStream(), LoginVo.class);
            // 封装对象
            UsernamePasswordAuthenticationToken authenticationToken
                    = new UsernamePasswordAuthenticationToken(loginVo.getUsername(), loginVo.getPassword());
            // 调用方法
            return this.getAuthenticationManager().authenticate(authenticationToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 身份验证成功
     *
     * @param request  请求
     * @param response 响应
     * @param chain    链
     * @param auth     身份验证
     * @throws IOException      ioexception
     * @throws ServletException servlet异常
     */// 认证成功
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {
        // 获取当前用户对象
        CustomUser customUser = (CustomUser) auth.getPrincipal();
        // 生成token
        String token = JwtHelper.createToken(customUser.getSysUser().getId(), customUser.getSysUser().getUsername());
        // 获取当前用户的权限数据，放到redis中
        // key 名称 value 权限
        redisTemplate.opsForValue().set(customUser.getUsername(), JSON.toJSONString(customUser.getAuthorities()));

        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        ResponseUtil.out(response, Result.ok(map));
    }

    /**
     * 不成功身份验证
     *
     * @param request  请求
     * @param response 响应
     * @param e        e
     * @throws IOException      ioexception
     * @throws ServletException servlet异常
     */// 认证失败
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException e) throws IOException, ServletException {
        /*if(e.getCause() instanceof RuntimeException) {
            ResponseUtil.out(response, Result.build(null, 204, e.getMessage()));
        } else {
            ResponseUtil.out(response, Result.build(null, ResultCodeEnum.LOGIN_AUTH));
        }*/
        ResponseUtil.out(response, Result.build(null, ResultCodeEnum.LOGIN_AUTH));
    }
}
