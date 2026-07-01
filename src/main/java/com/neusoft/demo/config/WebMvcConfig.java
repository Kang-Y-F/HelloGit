package com.neusoft.demo.config;

import com.neusoft.demo.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    /** 全局跨域（Vue3 开发联调需要） */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /** JWT 拦截器白名单（与原文件完全一致，只加了跨域方法） */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/doctor/login",
                        "/health",          // ← 加这行
                        "/report/ct/**",    // ← 加这行
                        "/report/lab/**",    // ← 加这行
                        "/lab-report/**",
                        "/admin/login",
                        "/admin/add",
                        "/error",
                        "/upload/**",      // 上传接口
                        "/uploads/**"      // 静态资源（上传的文件）
                );
    }

    /** 静态资源映射：让上传的文件可以通过HTTP访问 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
    }
}
