package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.BatchLabReportCreateRequest;
import com.neusoft.demo.dto.BatchLabReportCreateResponse;
import com.neusoft.demo.dto.LabReportConfirmDTO;
import com.neusoft.demo.dto.LabReportDTO;
import com.neusoft.demo.entity.LabReport;
import com.neusoft.demo.mapper.LabReportMapper;
import com.neusoft.demo.service.LabReportService;
import com.neusoft.demo.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/lab-report")
public class LabReportController {

    @Autowired private LabReportService labReportService;
    @Autowired private LabReportMapper  labReportMapper;
    @Autowired private RestTemplate restTemplate;
    /** 待执行检验单列表 */
    @GetMapping("/pending")
    public Result<?> pendingLabOrders(@RequestParam(required = false) String keyword) {
        return Result.success(labReportService.listPendingLabOrders(keyword));
    }

    /** 录入检验报告 */
    @PostMapping("/create")
    public Result<?> create(@RequestBody LabReportDTO dto, HttpServletRequest request) {
        Long operatorId = parseUserId(request);
        try {
            LabReport report = labReportService.createReport(operatorId, dto);
            return Result.success(report);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 审核检验报告 */
    @PutMapping("/{id}/audit")
    public Result<?> audit(@PathVariable Long id, @RequestParam Integer status) {
        boolean ok = labReportService.auditReport(id, status);
        return ok ? Result.success("操作成功") : Result.fail("操作失败");
    }
    /**
     * 修改后确认检验报告
     * auditStatus=3，同时把修改后的内容写入 report_content
     */
    @PutMapping("/{id}/confirm")
    public Result<?> confirm(@PathVariable Long id, @RequestBody LabReportConfirmDTO dto) {
        try {
            boolean ok = labReportService.confirmWithEdit(id, dto.getAuditStatus(), dto.getEditedContent());
            return ok ? Result.success("操作成功") : Result.fail("操作失败");
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
    /** 按患者查检验报告 */
    @GetMapping("/patient/{patientId}")
    public Result<?> listByPatient(@PathVariable Long patientId) {
        return Result.success(labReportService.listByPatient(patientId));
    }

    /** AI 异常解读 */
    @PostMapping("/{id}/ai-summary")
    public Result<?> aiSummary(@PathVariable Long id) {
        try {
            String summary = labReportService.generateAiSummary(id);
            return Result.success(summary);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 血糖趋势预测 */
    @GetMapping("/blood-sugar/{patientId}")
    public Result<?> bloodSugar(@PathVariable Long patientId) {
        return Result.success(labReportService.predictBloodSugar(patientId));
    }

    /**
     * 今日已录入报告（带患者姓名）
     * 直接调 Mapper 联表返回 Map，前端拿 patient_name 字段展示
     */
    @GetMapping("/today")
    public Result<?> todayReports(HttpServletRequest request) {
        Long operatorId = parseUserId(request);
        return Result.success(labReportMapper.selectTodayWithPatient(operatorId));
    }

    private Long parseUserId(HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        return claims.get("userId", Long.class);
    }

    /**
     * 单条检验报告详情（含 report_content 里的 AI 解读）
     */
    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        LabReport report = labReportMapper.selectById(id);
        if (report == null) return Result.fail("报告不存在");
        return Result.success(report);
    }

    /** 获取患者所有有历史记录的指标 */
    @GetMapping("/indicators/{patientId}")
    public Result<?> indicators(@PathVariable Long patientId) {
        return Result.success(labReportService.getAvailableIndicators(patientId));
    }

    /** 通用多指标趋势 */
    @GetMapping("/trend/{patientId}")
    public Result<?> trend(@PathVariable Long patientId, @RequestParam String indicator) {
        return Result.success(labReportService.getTrend(patientId, indicator));
    }

    @PostMapping("/batch-create")
    public Result<BatchLabReportCreateResponse> batchCreate(
            @RequestBody BatchLabReportCreateRequest request) {
        BatchLabReportCreateResponse resp = labReportService.batchCreate(request);
        return Result.success(resp);
    }
    @Value("${python.predict.service.url}")
    private String pythonHl7BaseUrl;
    @PostMapping("/hl7-sim/cgm-series")
    public Result<?> simulateCgmSeries(@RequestBody Map<String, Object> body) {
        // 修复：把数字转为字符串，匹配Python str类型要求
        if (body.containsKey("patientId")) {
            body.put("patientId", String.valueOf(body.get("patientId")));
        }
        if (body.containsKey("checkOrderId")) {
            body.put("checkOrderId", String.valueOf(body.get("checkOrderId")));
        }

        String targetUrl = pythonHl7BaseUrl + "/hl7-sim/cgm-series";
        Map<String, Object> pythonResp = restTemplate.postForObject(targetUrl, body, Map.class);
        return Result.success(pythonResp);
    }
}
