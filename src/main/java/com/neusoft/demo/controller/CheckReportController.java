package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.mapper.CheckReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/check-report")
public class CheckReportController {

    @Autowired
    private CheckReportMapper checkReportMapper;

    /** 患者所有影像报告（含CT伪影分割结果） */
    @GetMapping("/patient/{patientId}")
    public Result<?> listByPatient(@PathVariable Long patientId) {
        return Result.success(checkReportMapper.selectByPatientId(patientId));
    }

    /** 单条影像报告详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        var report = checkReportMapper.selectById(id);
        if (report == null) return Result.fail("报告不存在");
        return Result.success(report);
    }
}
