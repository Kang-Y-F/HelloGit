package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.demo.entity.Department;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.mapper.DepartmentMapper;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Override
    public List<Department> listAll() {

        return departmentMapper.selectList(null);
    }

    @Override
    public void add(Department department) {
        departmentMapper.insert(department);
    }

    @Override
    public void updateName(Long id, String name) {
        Department d = new Department();
        d.setId(id);
        d.setName(name);
        departmentMapper.updateById(d);
    }

    @Override
    public void delete(Long id) {
        departmentMapper.deleteById(id);
    }

    @Override
    public List<Doctor> getDoctorsByDept(Long deptId) {
        return doctorMapper.selectList(
                new QueryWrapper<Doctor>().eq("dept_id", deptId)
        );
    }
}