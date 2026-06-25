package com.neusoft.demo.controller;

import com.neusoft.demo.entity.RegisterFeeRule;
import com.neusoft.demo.service.RegisterFeeRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/register-fee-rule")
public class RegisterFeeRuleController {

    @Autowired
    private RegisterFeeRuleService service;

    /**
     * 查询挂号收费规则
     */
    @GetMapping("/list")
    public List<RegisterFeeRule> list(){
        return service.list();
    }

    /**
     * 修改挂号收费规则
     */
    @PutMapping("/update")
    public String update(@RequestBody RegisterFeeRule rule){
        service.update(rule);
        return "修改成功";
    }

}