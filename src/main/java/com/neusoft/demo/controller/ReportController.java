package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.CheckReport;
import com.neusoft.demo.entity.LabReport;
import com.neusoft.demo.mapper.CheckReportMapper;
import com.neusoft.demo.mapper.LabReportMapper;
import com.neusoft.demo.mapper.PmiPatientMapper;
import com.neusoft.demo.entity.PmiPatient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired private CheckReportMapper checkReportMapper;
    @Autowired private LabReportMapper   labReportMapper;
    @Autowired private PmiPatientMapper  pmiPatientMapper;

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
                                .eq(LabReport::getOrderId, patientId)
                                .orderByDesc(LabReport::getCreateTime)
                )
        );
    }

    /**
     * 查询所有有CT报告的患者列表（CT影像分析列表页使用）
     * 返回每个患者的报告数量、最新报告时间、最大伪影像素数
     */
    @GetMapping("/ct/patients")
    public Result<?> ctPatients() {
        // 查所有CT报告
        List<CheckReport> all = checkReportMapper.selectList(
                new LambdaQueryWrapper<CheckReport>()
                        .orderByDesc(CheckReport::getCreateTime)
        );

        // 按患者ID分组
        Map<Long, List<CheckReport>> grouped = all.stream()
                .filter(r -> r.getPatientId() != null)
                .collect(Collectors.groupingBy(CheckReport::getPatientId));

        // 查患者姓名
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<CheckReport>> entry : grouped.entrySet()) {
            Long patientId = entry.getKey();
            List<CheckReport> reports = entry.getValue();

            // 查患者信息
            PmiPatient patient = pmiPatientMapper.selectById(patientId);

            // 最大伪影像素（从 artifactResult JSON 里取）
            long maxPixels = 0;
            for (CheckReport r : reports) {
                if (r.getArtifactResult() != null) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                        Map<String, Object> artifact = om.readValue(r.getArtifactResult(), Map.class);
                        Object px = artifact.get("artifact_pixel_count");
                        if (px instanceof Number) maxPixels = Math.max(maxPixels, ((Number) px).longValue());
                    } catch (Exception ignored) {}
                }
            }

            // 最新报告时间
            String lastDate = reports.isEmpty() ? "" :
                    reports.get(0).getCreateTime() != null ?
                            reports.get(0).getCreateTime().toString().substring(0, 16).replace("T", " ") : "";

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("patientId",    patientId);
            item.put("patientName",  patient != null ? patient.getName() : "患者" + patientId);
            item.put("reportCount",  reports.size());
            item.put("lastDate",     lastDate);
            item.put("maxPixels",    maxPixels);
            result.add(item);
        }

        // 按最新报告时间倒序
        result.sort((a, b) -> String.valueOf(b.get("lastDate")).compareTo(String.valueOf(a.get("lastDate"))));

        return Result.success(result);
    }

    /**
     * 删除患者所有CT影像报告（聚类页删除患者时同步调用）
     */
    @DeleteMapping("/ct/patient/{patientId}")
    public Result<?> deleteCtReports(@PathVariable Long patientId) {
        int rows = checkReportMapper.delete(
                new LambdaQueryWrapper<CheckReport>()
                        .eq(CheckReport::getPatientId, patientId)
        );
        return Result.success("已删除 " + rows + " 条CT影像报告");
    }
}
