package com.neusoft.demo.controller;

import com.neusoft.demo.entity.RegisterExceptionLog;
import com.neusoft.demo.service.RegisterExceptionLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/register-exception")
public class RegisterExceptionController {

    @Autowired
    private RegisterExceptionLogService service;

    /** 异常列表 */
    @GetMapping("/list")
    public List<RegisterExceptionLog> list() {
        return service.list();
    }
}
