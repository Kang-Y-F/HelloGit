package com.neusoft.demo.service;

import com.neusoft.demo.entity.RegisterFeeRule;

import java.util.List;

public interface RegisterFeeRuleService {

    /**
     * 查询挂号收费规则
     */
    List<RegisterFeeRule> list();

    /**
     * 修改挂号收费规则
     */
    void update(RegisterFeeRule rule);


}