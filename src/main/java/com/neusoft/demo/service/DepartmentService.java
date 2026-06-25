package com.neusoft.demo.service;

import com.neusoft.demo.entity.Department;
import com.neusoft.demo.entity.Doctor;

import java.util.List;

public interface DepartmentService {

    /** 查询所有科室 */
    List<Department> listAll();
    /** 新增科室 */
    void add(Department department);

    /** 修改科室名称 */
    void updateName(Long id, String name);

    /** 删除科室 */
    void delete(Long id);

    /** 查询科室下医生 */
    List<Doctor> getDoctorsByDept(Long deptId);
}
