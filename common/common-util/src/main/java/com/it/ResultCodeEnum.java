package com.it;

import lombok.Getter;

/**
 * 枚举结果代码
 * 统一返回结果状态信息类
 *
 * @author 杨振华
 * @date 2023/06/20
 */
@Getter
public enum ResultCodeEnum {

    SUCCESS(200,"成功"),
    FAIL(201, "失败"),
    SERVICE_ERROR(2012, "服务异常"),
    DATA_ERROR(204, "数据异常"),
    ACCOUNT_STOP(205,"密码错误"),

    LOGIN_AUTH(208, "认证失败，未登录"),
    PERMISSION(209, "没有权限")
    ;

    private Integer code;

    private String message;

    private ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}