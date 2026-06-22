package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.entity.Department;
import com.neusoft.demo.mapper.DepartmentMapper;
import com.neusoft.demo.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public List<Department> listAll() {

        return departmentMapper.selectList(null);
    }
}