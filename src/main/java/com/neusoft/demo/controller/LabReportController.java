package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.LabReportDTO;
import com.neusoft.demo.entity.LabReport;
import com.neusoft.demo.service.LabReportService;
import com.neusoft.demo.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 检验报告 Controller
 *
 * 接口概览：
 *   GET  /lab-report/pending        待执行检验单列表（检验科用）
 *   POST /lab-report/create         录入检验报告
 *   PUT  /lab-report/{id}/audit     审核报告（1通过 2驳回）
 *   GET  /lab-report/patient/{id}   按患者查报告
 *   POST /lab-report/{id}/ai-summary  AI异常解读
 *   GET  /lab-report/blood-sugar/{patientId}  血糖预测
 */
@RestController
@RequestMapping("/lab-report")
public class LabReportController {

    @Autowired
    private LabReportService labReportService;

    /**
     * 待执行检验单列表
     * 查 check_order: status=1（已缴费/待执行）且 order_type=2（检验）
     */
    @GetMapping("/pending")
    public Result<?> pendingLabOrders(
            @RequestParam(required = false) String keyword
    ) {
        return Result.success(labReportService.listPendingLabOrders(keyword));
    }

    /**
     * 录入检验报告
     * 自动判断异常 → 写 lab_report → 更新 check_order.status=4
     */
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

    /**
     * 审核检验报告
     */
    @PutMapping("/{id}/audit")
    public Result<?> audit(@PathVariable Long id, @RequestParam Integer status) {
        boolean ok = labReportService.auditReport(id, status);
        return ok ? Result.success("操作成功") : Result.fail("操作失败");
    }

    /**
     * 按患者查检验报告
     */
    @GetMapping("/patient/{patientId}")
    public Result<?> listByPatient(@PathVariable Long patientId) {
        return Result.success(labReportService.listByPatient(patientId));
    }

    /**
     * AI 异常解读（对单条检验报告做临床解读）
     */
    @PostMapping("/{id}/ai-summary")
    public Result<?> aiSummary(@PathVariable Long id) {
        try {
            String summary = labReportService.generateAiSummary(id);
            return Result.success(summary);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 血糖趋势预测
     */
    @GetMapping("/blood-sugar/{patientId}")
    public Result<?> bloodSugar(@PathVariable Long patientId) {
        return Result.success(labReportService.predictBloodSugar(patientId));
    }

    private Long parseUserId(HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        return claims.get("userId", Long.class);
    }
}
