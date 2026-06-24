package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.*;
import com.neusoft.demo.entity.Drug;
import com.neusoft.demo.mapper.DrugMapper;
import com.neusoft.demo.service.PharmacyService;
import com.neusoft.demo.utils.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 药房管理（药师工作台）
 */
@RestController
@RequestMapping("/pharmacy")
public class PharmacyController {

    @Autowired private PharmacyService pharmacyService;
    @Autowired private DrugMapper      drugMapper;

    // ── 药品目录 ───────────────────────────────────────────

    /** 药品列表（含库存） */
    @GetMapping("/drugs")
    public Result<?> listDrugs() {
        return Result.success(pharmacyService.listAllDrugsWithStock());
    }

    /** 简单药品搜索（医生开药用） */
    @GetMapping("/drugs/search")
    public Result<?> searchDrugs(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Drug> w = new LambdaQueryWrapper<Drug>()
                .eq(Drug::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            w.and(q -> q.like(Drug::getDrugName, kw)
                    .or().like(Drug::getGenericName, kw)
                    .or().like(Drug::getDrugCode, kw));
        }
        w.last("LIMIT 30");
        return Result.success(drugMapper.selectList(w));
    }

    /** 低库存预警 */
    @GetMapping("/drugs/low-stock")
    public Result<?> lowStock() {
        return Result.success(pharmacyService.listLowStockDrugs());
    }

    // ── 入库出库 ───────────────────────────────────────────

    @PostMapping("/drug-in")
    public Result<?> drugIn(@RequestBody DrugInDTO dto, HttpServletRequest request) {
        try {
            return Result.success(pharmacyService.drugIn(dto, parseUserId(request)));
        } catch (Exception e) { return Result.fail(e.getMessage()); }
    }

    @PostMapping("/drug-out")
    public Result<?> drugOut(@RequestBody DrugOutDTO dto, HttpServletRequest request) {
        try {
            return Result.success(pharmacyService.drugOut(dto, parseUserId(request)));
        } catch (Exception e) { return Result.fail(e.getMessage()); }
    }

    @GetMapping("/inout-records")
    public Result<?> inoutRecords(
            @RequestParam(required = false) Long drugId,
            @RequestParam(required = false) Integer recordType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Map<String, Object> f = new HashMap<>();
        if (drugId     != null) f.put("drugId", drugId);
        if (recordType != null) f.put("recordType", recordType);
        if (startDate  != null && !startDate.isEmpty()) f.put("startDate", startDate + " 00:00:00");
        if (endDate    != null && !endDate.isEmpty())   f.put("endDate",   endDate   + " 23:59:59");
        return Result.success(pharmacyService.listInoutRecords(f));
    }

    // ── AI 审方 ───────────────────────────────────────────

    @PostMapping("/ai-audit")
    public Result<?> aiAudit(@RequestBody AiAuditPrescriptionDTO dto) {
        try {
            return Result.success(pharmacyService.aiAuditPrescription(dto));
        } catch (Exception e) { return Result.fail(e.getMessage()); }
    }

    // ── 药师审核 ──────────────────────────────────────────

    @GetMapping("/pending-audit")
    public Result<?> pendingAudit() {
        return Result.success(pharmacyService.listPendingAudit());
    }

    @PostMapping("/pharmacist-audit")
    public Result<?> pharmacistAudit(@RequestBody PharmacistAuditDTO dto, HttpServletRequest request) {
        try {
            boolean ok = pharmacyService.pharmacistAudit(dto, parseUserId(request));
            return ok ? Result.success("审核完成") : Result.fail("审核失败");
        } catch (Exception e) { return Result.fail(e.getMessage()); }
    }

    // ── 发药 ──────────────────────────────────────────────

    @GetMapping("/pending-dispense")
    public Result<?> pendingDispense() {
        return Result.success(pharmacyService.listPendingDispense());
    }

    @PostMapping("/dispense")
    public Result<?> dispense(@RequestBody DispenseDTO dto, HttpServletRequest request) {
        try {
            return Result.success(pharmacyService.dispense(dto, parseUserId(request)));
        } catch (Exception e) { return Result.fail(e.getMessage()); }
    }

    @GetMapping("/dispense-records")
    public Result<?> dispenseRecords(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Map<String, Object> f = new HashMap<>();
        if (patientId != null) f.put("patientId", patientId);
        if (startDate != null && !startDate.isEmpty()) f.put("startDate", startDate + " 00:00:00");
        if (endDate   != null && !endDate.isEmpty())   f.put("endDate",   endDate   + " 23:59:59");
        return Result.success(pharmacyService.listDispenseRecords(f));
    }

    // ── 统计 ──────────────────────────────────────────────

    @GetMapping("/today-stats")
    public Result<?> todayStats(HttpServletRequest request) {
        return Result.success(pharmacyService.todayStats(parseUserId(request)));
    }

    private Long parseUserId(HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        return claims.get("userId", Long.class);
    }
}
