package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.CheckReport;
import com.neusoft.demo.entity.LabReport;
import com.neusoft.demo.mapper.CheckReportMapper;
import com.neusoft.demo.mapper.LabReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired private CheckReportMapper checkReportMapper;
    @Autowired private LabReportMapper   labReportMapper;

    /** 患者所有CT影像报告 */
    @GetMapping("/ct/{patientId}")
    public Result<?> ctReports(@PathVariable Long patientId) {
        return Result.success(checkReportMapper.selectByPatientId(patientId));
    }

    /** 患者所有检验报告 */
    @GetMapping("/lab/{patientId}")
    public Result<?> labReports(@PathVariable Long patientId) {
        return Result.success(
                labReportMapper.selectList(
                        new LambdaQueryWrapper<LabReport>()
                                .eq(LabReport::getOrderId, patientId)  // 按需调整
                                .orderByDesc(LabReport::getCreateTime)
                )
        );
    }
}
