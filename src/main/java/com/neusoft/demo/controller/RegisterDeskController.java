package com.neusoft.demo.controller;
import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.mapper.RegisterOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.RegisterCreateDTO;
import com.neusoft.demo.entity.Department;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.entity.PmiPatient;
import com.neusoft.demo.entity.Schedule;
import com.neusoft.demo.mapper.DepartmentMapper;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.mapper.PmiPatientMapper;
import com.neusoft.demo.mapper.ScheduleMapper;
import com.neusoft.demo.service.RegisterDeskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 挂号台：挂号员代为挂号
 */
@RestController
@RequestMapping("/register-desk")
public class RegisterDeskController {

    @Autowired private DepartmentMapper      departmentMapper;
    @Autowired private DoctorMapper          doctorMapper;
    @Autowired private ScheduleMapper        scheduleMapper;
    @Autowired private PmiPatientMapper      pmiPatientMapper;
    @Autowired private RegisterDeskService   registerDeskService;
    @Autowired private RegisterOrderMapper registerOrderMapper;
    
    /** 1. 查询所有科室 */
    @GetMapping("/departments")
    public Result<?> departments() {
        return Result.success(departmentMapper.selectList(null));
    }

    /** 2. 按科室查医生（只查role=doctor） */
    @GetMapping("/doctors")
    public Result<?> doctorsByDept(@RequestParam Long deptId) {
        return Result.success(doctorMapper.selectList(
                new LambdaQueryWrapper<Doctor>()
                        .eq(Doctor::getDeptId, deptId)
                        .eq(Doctor::getStatus, 1)
                        .eq(Doctor::getRole, "doctor")
        ));
    }

    /** 3. 查医生未来可挂号的排班 */
    @GetMapping("/schedules")
    public Result<?> availableSchedules(@RequestParam Long doctorId) {
        List<Schedule> list = scheduleMapper.selectList(
                new LambdaQueryWrapper<Schedule>()
                        .eq(Schedule::getDoctorId, doctorId)
                        .ge(Schedule::getWorkDate, LocalDate.now())
                        .orderByAsc(Schedule::getWorkDate)
                        .last("LIMIT 14")
        );
        list.removeIf(s -> s.getCurrentNum() != null && s.getMaxNum() != null
                && s.getCurrentNum() >= s.getMaxNum());
        return Result.success(list);
    }

    /** 4. 搜索患者（按姓名/手机号/身份证号） */
    @GetMapping("/patients/search")
    public Result<?> searchPatient(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return Result.success(List.of());
        String kw = keyword.trim();
        List<PmiPatient> list = pmiPatientMapper.selectList(
                new LambdaQueryWrapper<PmiPatient>()
                        .like(PmiPatient::getName, kw)
                        .or().like(PmiPatient::getPhone, kw)
                        .or().like(PmiPatient::getIdCard, kw)
                        .last("LIMIT 10")
        );
        return Result.success(list);
    }

    /** 5. 新建患者档案 */
    @PostMapping("/patients/create")
    public Result<?> createPatient(@RequestBody PmiPatient body) {
        if (body.getName() == null || body.getName().trim().isEmpty())
            return Result.fail("姓名必填");
        if (body.getPhone() == null || body.getPhone().trim().isEmpty())
            return Result.fail("手机号必填");

        // 手机号已存在则返回已有
        PmiPatient exist = pmiPatientMapper.selectOne(
                new LambdaQueryWrapper<PmiPatient>().eq(PmiPatient::getPhone, body.getPhone())
        );
        if (exist != null) return Result.success(exist);

        pmiPatientMapper.insert(body);
        return Result.success(body);
    }

    /** 6. 提交挂号 */
    @PostMapping("/create")
    public Result<?> createRegisterOrder(@RequestBody RegisterCreateDTO dto) {
        try {
            return Result.success(registerDeskService.createRegisterOrder(dto));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 查询所有挂号记录（挂号员看） */
    @GetMapping("/records/all")
    public Result<?> allRecords() {
        return Result.success(
                registerOrderMapper.selectList(
                        new LambdaQueryWrapper<RegisterOrder>()
                                .orderByDesc(RegisterOrder::getCreateTime)
                                .last("LIMIT 200")
                )
        );
    }
}
