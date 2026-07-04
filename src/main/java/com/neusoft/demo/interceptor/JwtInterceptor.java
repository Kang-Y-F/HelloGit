package com.neusoft.demo.interceptor;

import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private DoctorMapper doctorMapper;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String token = request.getHeader("token");

        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("Token Missing");
            return false;
        }

        if (!JwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.getWriter().write("Token Invalid");
            return false;
        }

        Claims claims = JwtUtil.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        String role = claims.get("role", String.class);

        // 新增：医生角色的每次请求都实时校验账号状态
        if ("doctor".equals(role)) {
            Doctor doctor = doctorMapper.selectById(userId);
            if (doctor == null || (doctor.getStatus() != null && doctor.getStatus() == 0)) {
                response.setStatus(401);
                response.getWriter().write("账号已被禁用，请重新登录或联系管理员");
                return false;
            }
        }

        request.setAttribute("userId", userId);

        return true;
    }
}