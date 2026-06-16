package com.neusoft.demo.config;

import com.neusoft.demo.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig
        implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(
            InterceptorRegistry registry) {

        registry.addInterceptor(jwtInterceptor)

                .addPathPatterns("/**")

                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/doctor/login",
                        "/admin/login",
                        // "/doctor/add",
                        "/admin/add",
                        "/error"
                );
    }
}