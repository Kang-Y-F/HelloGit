package com.neusoft.demo.exception;

import com.neusoft.demo.common.Result;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExpiredJwtException.class)
    public Result<?> handleExpiredJwt(ExpiredJwtException e) {
        return Result.fail(401, "登录已过期，请重新登录");
    }

    @ExceptionHandler(JwtException.class)
    public Result<?> handleInvalidJwt(JwtException e) {
        return Result.fail(401, "登录状态无效，请重新登录");
    }

    // 兜底：其他未捕获异常也别裸抛500堆栈给前端
    @ExceptionHandler(Exception.class)
    public Result<?> handleOther(Exception e) {
        return Result.fail(500, "服务器异常：" + e.getMessage());
    }
}