package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.service.RegisterOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register-order")
public class RegisterOrderController {

    @Autowired
    private RegisterOrderService registerOrderService;

    /** 挂号单详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        var order = registerOrderService.getById(id);
        if (order == null) return Result.fail("挂号单不存在");
        return Result.success(order);
    }

    /** 患者所有挂号记录 */
    @GetMapping("/patient/{patientId}")
    public Result<?> listByPatient(@PathVariable Long patientId) {
        return Result.success(registerOrderService.listByPatient(patientId));
    }
}
