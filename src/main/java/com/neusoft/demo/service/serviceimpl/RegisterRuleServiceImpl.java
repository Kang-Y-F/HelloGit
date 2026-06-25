package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.mapper.RegisterRuleMapper;
import com.neusoft.demo.service.RegisterRuleService;
import com.neusoft.demo.entity.RegisterRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegisterRuleServiceImpl implements RegisterRuleService {

    @Autowired
    private RegisterRuleMapper registerRuleMapper;

    /**
     * 获取挂号规则（默认取第一条）
     */
    @Override
    public RegisterRule getRule() {
        List<RegisterRule> list = registerRuleMapper.selectList(null);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 更新规则（默认更新第一条）
     */
    @Override
    public void updateRule(RegisterRule rule) {

        List<RegisterRule> list = registerRuleMapper.selectList(null);

        if (list.isEmpty()) {
            registerRuleMapper.insert(rule);
        } else {
            rule.setId(list.get(0).getId());
            registerRuleMapper.updateById(rule);
        }
    }
}
