package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.service.RegisterOrderService;
import com.neusoft.demo.utils.JwtUtil;
import com.neusoft.demo.vo.RegisterOrderVO;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/register-order")
public class RegisterOrderController {

    @Autowired
    private RegisterOrderService registerOrderService;

    /** 挂号单详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        var order = registerOrderService.getById(id);
        if (order == null) return Result.fail("挂号单不存在");
        return Result.success(order);
    }

    /** 患者所有挂号记录 */
    @GetMapping("/patient/{patientId}")
    public Result<?> listByPatient(@PathVariable Long patientId) {
        return Result.success(registerOrderService.listByPatient(patientId));
    }

    /**
     * 在线挂号
     */
    @PostMapping("/add")
    public Result<?> add(HttpServletRequest request,
            @RequestParam Long doctorId,
            @RequestParam Long scheduleId,
            @RequestParam Integer priority
    ) {

        Long userId = (Long) request.getAttribute("userId");

        try {
            RegisterOrder order = registerOrderService.addRegisterOrder(
                    userId,
                    doctorId,
                    scheduleId,
                    priority
            );
            return Result.success(order);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 取消挂号
     */
    @PostMapping("/cancel/{id}")
    public Result<?> cancel(
            HttpServletRequest request,
            @PathVariable Long id
    ) {

        Long userId =
                (Long) request.getAttribute("userId");

        return Result.success(
                registerOrderService.cancelOrder(
                        userId,
                        id
                )
        );
    }

    /**
     * 患者端：挂号单详情
     */
    @GetMapping("/patient/detail/{id}")
    public Result<?> patientDetail(@PathVariable Long id) {

        RegisterOrderVO vo = registerOrderService.patientDetail(id);

        if (vo == null) {
            return Result.fail("挂号单不存在");
        }

        return Result.success(vo);
    }
    /**
     * 挂号员修改优先级（普通 <-> 急诊）
     */
    @PostMapping("/updatePriority")
    public Result<?> updatePriority(
            HttpServletRequest request,
            @RequestParam Long orderId,
            @RequestParam Integer newPriority,
            @RequestParam String reason
    ) {
        Long operatorId = parseDoctorId(request);

        String result = registerOrderService.updatePriority(operatorId, orderId, newPriority, reason);

        if ("修改成功".equals(result)) {
            return Result.success(result);
        }
        return Result.fail(result);
    }

    /**
     * 从 token 中解析当前登录人 id（挂号员/医生/药师统一走 doctor 表）
     */
    private Long parseDoctorId(HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        return claims.get("userId", Long.class);
    }
}
