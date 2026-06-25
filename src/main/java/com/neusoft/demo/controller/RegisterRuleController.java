package com.neusoft.demo.controller;

import com.neusoft.demo.entity.RegisterRule;
import com.neusoft.demo.service.RegisterRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register-rule")
public class RegisterRuleController {

    @Autowired
    private RegisterRuleService registerRuleService;

    /** 获取挂号规则 */
    @GetMapping
    public RegisterRule getRule() {
        return registerRuleService.getRule();
    }

    /** 更新挂号规则 */
    @PutMapping
    public String update(@RequestBody RegisterRule rule) {
        registerRuleService.updateRule(rule);
        return "更新成功";
    }
}