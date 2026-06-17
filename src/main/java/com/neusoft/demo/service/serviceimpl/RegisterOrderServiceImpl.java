package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.mapper.RegisterOrderMapper;
import com.neusoft.demo.service.RegisterOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegisterOrderServiceImpl implements RegisterOrderService {

    @Autowired
    private RegisterOrderMapper registerOrderMapper;

    @Override
    public RegisterOrder getById(Long id) {
        return registerOrderMapper.selectById(id);
    }

    @Override
    public List<RegisterOrder> listByPatient(Long patientId) {
        return registerOrderMapper.selectList(
                new LambdaQueryWrapper<RegisterOrder>()
                        .eq(RegisterOrder::getUserId, patientId)
                        .orderByDesc(RegisterOrder::getCreateTime)
        );
    }
}
