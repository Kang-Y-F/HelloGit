package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.mapper.RegisterOrderMapper;
import com.neusoft.demo.service.CheckOrderService;
import com.neusoft.demo.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

/**
 * 挂号台数据看板
 */
@RestController
@RequestMapping("/register-dashboard")
public class RegisterDashboardController {

    @Autowired private RegisterOrderMapper registerOrderMapper;
    @Autowired private PaymentService      paymentService;
    @Autowired private CheckOrderService   checkOrderService;

    private static final Map<Integer, String> STATUS_LABEL = Map.of(
            0, "待支付", 1, "已挂号", 2, "已取消", 3, "就诊中", 4, "已完成"
    );

    @GetMapping("/stats")
    public Result<?> stats() {
        Map<String, Object> result = new HashMap<>();

        // 1. 今日收费统计（全院）
        result.put("todayStats", paymentService.todayStats(null));

        // 2. 挂号状态分布
        List<Map<String, Object>> statusRaw = registerOrderMapper.selectStatusDistribution();
        List<Map<String, Object>> statusDistribution = new ArrayList<>();
        for (Map<String, Object> row : statusRaw) {
            Integer status = ((Number) row.get("status")).intValue();
            Map<String, Object> item = new HashMap<>();
            item.put("status", status);
            item.put("label", STATUS_LABEL.getOrDefault(status, "未知"));
            item.put("cnt", row.get("cnt"));
            statusDistribution.add(item);
        }
        result.put("statusDistribution", statusDistribution);

        // 3. 近7日挂号量趋势（补全无数据的日期为0）
        List<Map<String, Object>> trendRaw = registerOrderMapper.selectTrend7Days();
        Map<String, Long> trendMap = new HashMap<>();
        for (Map<String, Object> row : trendRaw) {
            trendMap.put(row.get("day").toString(), ((Number) row.get("cnt")).longValue());
        }
        List<Map<String, Object>> trend7d = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String day = LocalDate.now().minusDays(i).toString();
            Map<String, Object> item = new HashMap<>();
            item.put("date", day);
            item.put("cnt", trendMap.getOrDefault(day, 0L));
            trend7d.add(item);
        }
        result.put("trend7d", trend7d);

        // 4. Top5 医生挂号量排行
        result.put("doctorRank", registerOrderMapper.selectDoctorRankTop5());

        // 5. 待处理提醒数量
        result.put("pendingCheckCount", checkOrderService.listPendingPayment(null, null).size());
        result.put("pendingPrescriptionCount", paymentService.listPendingPrescription(null).size());

        return Result.success(result);
    }
}