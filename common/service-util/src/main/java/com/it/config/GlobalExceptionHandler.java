package com.it.config;

import com.it.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理类
 *
 * @author 杨振华
 * @date 2023/06/20
 */
@ControllerAdvice
public class GlobalExceptionHandler {


    // 全局异常处理
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result error(Exception e){
        e.printStackTrace();
        return Result.fail().message("执行全局异常");
    }

    // 特定异常处理
    // @ExceptionHandler(Exception.class)，里面的Exception换成特定的就行

    // 自定义异常处理
    @ExceptionHandler(ANnianException.class)
    @ResponseBody
    public Result error(ANnianException e){
        e.printStackTrace();
        return Result.fail().message(e.getMessage()).code(e.getCode());
    }
}