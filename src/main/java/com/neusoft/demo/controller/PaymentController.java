package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.PaymentDTO;
import com.neusoft.demo.dto.RefundDTO;
import com.neusoft.demo.service.PaymentService;
import com.neusoft.demo.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 收费台 — 挂号员/收银员收银
 */
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /** 收费 */
    @PostMapping("/collect")
    public Result<?> collect(@RequestBody PaymentDTO dto, HttpServletRequest request) {
        Long operatorId = parseUserId(request);
        try {
            return Result.success(paymentService.collectPayment(dto, operatorId));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 退款 / 取消挂号 */
    @PostMapping("/refund")
    public Result<?> refund(@RequestBody RefundDTO dto, HttpServletRequest request) {
        Long operatorId = parseUserId(request);
        try {
            boolean ok = paymentService.refund(dto, operatorId);
            return ok ? Result.success("操作成功") : Result.fail("操作失败");
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 收费记录查询 */
    @GetMapping("/records")
    public Result<?> records(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) Integer payMethod,
            @RequestParam(required = false) Integer payStatus,
            @RequestParam(required = false) String keyword
    ) {
        Map<String, Object> filter = new HashMap<>();
        if (orderId    != null) filter.put("orderId",    orderId);
        if (patientId  != null) filter.put("patientId",  patientId);
        if (operatorId != null) filter.put("operatorId", operatorId);
        if (payMethod  != null) filter.put("payMethod",  payMethod);
        if (payStatus  != null) filter.put("payStatus",  payStatus);
        if (keyword    != null && !keyword.isBlank()) filter.put("keyword", keyword);
        return Result.success(paymentService.listPayments(filter));
    }

    /** 今日收费统计（当前用户） */
    @GetMapping("/today-stats")
    public Result<?> todayStats(HttpServletRequest request) {
        Long operatorId = parseUserId(request);
        return Result.success(paymentService.todayStats(operatorId));
    }

    /** 今日全院收费统计 */
    @GetMapping("/today-stats/all")
    public Result<?> todayStatsAll() {
        return Result.success(paymentService.todayStats(null));
    }

    private Long parseUserId(HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        return claims.get("userId", Long.class);
    }

    /** 待缴处方费列表 */
    @GetMapping("/pending-prescription")
    public Result<?> pendingPrescription(@RequestParam(required = false) String keyword) {
        // 查 prescription 表：pharmacist_status=1 AND pay_status=0
        // 关联 pmi_patient 拿姓名/手机号，关联 doctor 拿医生名
        return Result.success(paymentService.listPendingPrescription(keyword));
    }
}
