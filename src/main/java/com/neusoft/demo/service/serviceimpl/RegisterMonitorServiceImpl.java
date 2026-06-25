package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.mapper.DepartmentMapper;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.mapper.RegisterOrderMapper;
import com.neusoft.demo.service.RegisterMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RegisterMonitorServiceImpl implements RegisterMonitorService {

    @Autowired
    private RegisterOrderMapper registerOrderMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public Long todayCount() {
        QueryWrapper<RegisterOrder> wrapper = new QueryWrapper<>();
        wrapper.apply("DATE(create_time) = CURDATE()");

        return registerOrderMapper.selectCount(wrapper);
    }

    @Override
    public List<Map<String, Object>> deptRank() {
        return registerOrderMapper.deptRank();
    }

    @Override
    public List<Map<String, Object>> doctorLoad() {
        return registerOrderMapper.doctorLoad();
    }
}