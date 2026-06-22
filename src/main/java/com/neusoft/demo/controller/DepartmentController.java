package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping("/list")
    public Result<?> list() {

        return Result.success(
                departmentService.listAll()
        );
    }
}
