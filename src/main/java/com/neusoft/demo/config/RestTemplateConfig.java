// ══ 如果你的 Spring Boot 项目里还没有 RestTemplate Bean ══
// 在任意 @Configuration 类里加，或者新建一个：

package com.neusoft.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);    // 连接超时5秒
        factory.setReadTimeout(240000);     // 读取超时拉长到240秒，给大模型推理留出时间
        return new RestTemplate(factory);
    }
}
