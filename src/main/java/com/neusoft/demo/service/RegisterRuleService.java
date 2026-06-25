package com.neusoft.demo.service;

import com.neusoft.demo.entity.RegisterRule;

public interface RegisterRuleService {
    /** 获取挂号规则（系统唯一配置）*/
    RegisterRule getRule();

    /** 更新挂号规则 */
    void updateRule(RegisterRule rule);
}
