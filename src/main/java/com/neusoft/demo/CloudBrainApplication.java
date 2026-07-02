package com.neusoft.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.neusoft.demo.mapper")
@SpringBootApplication
public class CloudBrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudBrainApplication.class, args);
    }
}
