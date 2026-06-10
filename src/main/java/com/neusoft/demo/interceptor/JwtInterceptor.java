package com.neusoft.demo.interceptor;

import com.neusoft.demo.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor
        implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String token =
                request.getHeader("token");

        if (token == null || token.isEmpty()) {

            response.setStatus(401);

            response.getWriter()
                    .write("Token Missing");

            return false;
        }

        if (!JwtUtil.validateToken(token)) {

            response.setStatus(401);

            response.getWriter()
                    .write("Token Invalid");

            return false;
        }

        return true;
    }
}