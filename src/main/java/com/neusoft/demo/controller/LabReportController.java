package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.*;
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

import java.util.*;

@RestController
@RequestMapping("/lab-report")
public class LabReportController {

    @Autowired private LabReportService labReportService;
    @Autowired private LabReportMapper  labReportMapper;
    @Autowired private RestTemplate     restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.predict.service.url}")
    private String pythonBaseUrl;

    // ── 待执行检验单 ──────────────────────────────────────────
    @GetMapping("/pending")
    public Result<?> pendingLabOrders(@RequestParam(required = false) String keyword) {
        return Result.success(labReportService.listPendingLabOrders(keyword));
    }

    // ── 单项检验录入（兼容旧接口） ────────────────────────────
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

    // ── 套餐检验录入（多子项，提交后自动AI解读） ──────────────
    @PostMapping("/create-suite")
    public Result<?> createSuite(@RequestBody LabReportSuiteDTO dto, HttpServletRequest request) {
        Long operatorId = parseUserId(request);
        try {
            List<LabReport> reports = labReportService.createSuiteReport(operatorId, dto);
            return Result.success(reports);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    // ── 审核 ─────────────────────────────────────────────────
    @PutMapping("/{id}/audit")
    public Result<?> audit(@PathVariable Long id, @RequestParam Integer status) {
        boolean ok = labReportService.auditReport(id, status);
        return ok ? Result.success("操作成功") : Result.fail("操作失败");
    }

    // ── 修改后确认 ───────────────────────────────────────────
    @PutMapping("/{id}/confirm")
    public Result<?> confirm(@PathVariable Long id,
                             @RequestBody LabReportConfirmDTO dto) {
        try {
            boolean ok = labReportService.confirmWithEdit(
                    id, dto.getAuditStatus(), dto.getEditedContent());
            return ok ? Result.success("操作成功") : Result.fail("操作失败");
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    // ── 今日已录入（分组后的聚合列表） ───────────────────────
    @GetMapping("/today")
    public Result<?> todayReports(HttpServletRequest request) {
        Long operatorId = parseUserId(request);
        return Result.success(labReportMapper.selectTodayWithPatient(operatorId));
    }

    // ── 按 suiteGroup 查套餐详情 + AI解读 ────────────────────
    // 前端点击今日已录入的套餐卡片时调用，拿到所有子项 + AI结果
    @GetMapping("/suite/{suiteGroup}")
    public Result<?> suiteDetail(@PathVariable String suiteGroup) {
        List<LabReport> list = labReportMapper.selectList(
                new LambdaQueryWrapper<LabReport>()
                        .eq(LabReport::getSuiteGroup, suiteGroup)
                        .orderByAsc(LabReport::getId));
        if (list.isEmpty()) return Result.fail("记录不存在");

        // AI解读存在第一条的 report_content
        String aiDesc = null;
        try {
            String content = list.get(0).getReportContent();
            if (content != null)
                aiDesc = objectMapper.readTree(content).get("desc").asText();
        } catch (Exception ignored) {}

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subItems", list);
        result.put("aiResult", aiDesc);
        result.put("auditStatus", list.get(0).getAuditStatus());
        result.put("representId", list.get(0).getId()); // 用于手动AI解读、审核接口的 id 参数
        return Result.success(result);
    }

    // ── AI 解读（手动触发，已录入记录） ──────────────────────
    @PostMapping("/{id}/ai-summary")
    public Result<?> aiSummary(@PathVariable Long id) {
        try {
            String summary = labReportService.generateAiSummary(id);
            return Result.success(summary);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
    // ── AI 解读预判（录入前，不落库） ──────────────────────
    @PostMapping("/ai-preview")
    public Result<?> aiPreview(@RequestBody AiPreviewRequest req) {
        try {
            String preview = labReportService.generateAiPreview(req);
            return Result.success(preview);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    // ── 单条报告详情 ─────────────────────────────────────────
    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        LabReport report = labReportMapper.selectById(id);
        if (report == null) return Result.fail("报告不存在");
        return Result.success(report);
    }

    // ── 患者所有报告 ─────────────────────────────────────────
    @GetMapping("/patient/{patientId}")
    public Result<?> listByPatient(@PathVariable Long patientId) {
        return Result.success(labReportService.listByPatient(patientId));
    }

    // ── 患者有历史记录的指标列表 ─────────────────────────────
    @GetMapping("/indicators/{patientId}")
    public Result<?> indicators(@PathVariable Long patientId) {
        return Result.success(labReportService.getAvailableIndicators(patientId));
    }

    // ── 指标历史趋势 + Python预测 ─────────────────────────────
    @GetMapping("/trend/{patientId}")
    public Result<?> trend(@PathVariable Long patientId,
                           @RequestParam String indicator,
                           @RequestParam(required = false) String subItem) {
        return Result.success(labReportService.getTrend(patientId, indicator, subItem));
    }

    // ── 批量写入（CGM / HL7仿真数据） ────────────────────────
    @PostMapping("/batch-create")
    public Result<BatchLabReportCreateResponse> batchCreate(
            @RequestBody BatchLabReportCreateRequest request) {
        return Result.success(labReportService.batchCreate(request));
    }

    // ── HL7仿真：Java转发给Python ─────────────────────────────
    @PostMapping("/hl7-sim/cgm-preview")
    public Result<?> previewCgmSeries(@RequestBody Map<String, Object> body) {
        if (body.containsKey("patientId"))
            body.put("patientId", String.valueOf(body.get("patientId")));
        if (body.containsKey("checkOrderId"))
            body.put("checkOrderId", String.valueOf(body.get("checkOrderId")));

        Map<?, ?> pythonResp = restTemplate.postForObject(
                pythonBaseUrl + "/hl7-sim/cgm-preview", body, Map.class);

        // Python 返回的是 {code, message, data:{...}}，只把 data 那层取出来往上抛，
        // 避免前端要多剥一层
        Object data = pythonResp != null ? pythonResp.get("data") : null;
        return Result.success(data);
    }

    // ── 工具：从JWT解析操作员ID ──────────────────────────────
    private Long parseUserId(HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        return claims.get("userId", Long.class);
    }
}