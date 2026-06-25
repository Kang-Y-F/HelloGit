package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.Department;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping("/list")
    public Result<?> list() {

        return Result.success(
                departmentService.listAll()
        );
    }

    /** 新增科室 */
    @PostMapping("/add")
    public String add(@RequestBody Department department) {
        departmentService.add(department);
        return "添加成功";
    }

    /** 修改科室名称 */
    @PutMapping("/update/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name) {
        departmentService.updateName(id, name);
        return "修改成功";
    }

    /** 删除科室 */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        departmentService.delete(id);
        return "删除成功";
    }

    /** 查询科室下医生 */
    @GetMapping("/{deptId}/doctors")
    public List<Doctor> doctors(@PathVariable Long deptId) {
        return departmentService.getDoctorsByDept(deptId);
    }
}
