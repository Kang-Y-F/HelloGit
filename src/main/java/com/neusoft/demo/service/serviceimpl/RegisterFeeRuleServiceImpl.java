package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.entity.RegisterFeeRule;
import com.neusoft.demo.mapper.RegisterFeeRuleMapper;
import com.neusoft.demo.service.RegisterFeeRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegisterFeeRuleServiceImpl implements RegisterFeeRuleService {
    @Autowired
    private RegisterFeeRuleMapper mapper;

    /**
     * 查询挂号收费规则
     */
    @Override
    public List<RegisterFeeRule> list(){
        return mapper.selectList(null);
    }

    /**
     * 修改挂号收费规则
     */
    @Override
    public void update(RegisterFeeRule rule){
        mapper.updateById(rule);
    }

}