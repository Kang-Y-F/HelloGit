package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.mapper.CheckOrderMapper;
import com.neusoft.demo.service.CheckOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 检查/检验单 Controller
 *
 * check_order.status 说明：
 *   0=待缴费  1=已缴费/待执行  2=已取消  3=执行中  4=已完成（结果已回传）
 */
@RestController
@RequestMapping("/check-order")
public class CheckOrderController {

    @Autowired
    private CheckOrderMapper checkOrderMapper;

    @Autowired
    private CheckOrderService checkOrderService;

    /** 患者所有检查单（供医生/影像科查询） */
    @GetMapping("/patient/{patientId}")
    public Result<?> listByPatient(@PathVariable Long patientId) {
        return Result.success(checkOrderMapper.selectByPatientId(patientId));
    }

    /** 单条检查单详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        var order = checkOrderMapper.selectById(id);
        if (order == null) return Result.fail("检查单不存在");
        return Result.success(order);
    }

    /**
     * 待缴费检查单列表（供挂号台收费使用）
     *
     * status=0（待缴费）的检查/检验医嘱，携带患者姓名、检查项目名称、开单医生
     *
     * 可选参数：
     *   patientId  - 按患者ID过滤
     *   keyword    - 按患者姓名/手机号模糊搜索
     */
    @GetMapping("/pending-payment")
    public Result<?> pendingPayment(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String keyword
    ) {
        return Result.success(
            checkOrderService.listPendingPayment(patientId, keyword)
        );
    }

    /**
     * 更新检查单状态（供检查科室使用）
     *
     * 允许的状态流转：
     *   1（已缴费/待执行）→ 3（执行中）
     *   3（执行中）       → 4（已完成）
     *   0/1               → 2（已取消）
     */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status
    ) {
        try {
            checkOrderService.updateStatus(id, status);
            return Result.success("状态更新成功");
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 挂号单下所有检查单（含缴费状态，供挂号台/医生查看完整流程状态）
     */
    @GetMapping("/by-register/{registerOrderId}")
    public Result<?> listByRegisterOrder(@PathVariable Long registerOrderId) {
        return Result.success(checkOrderService.listByRegisterOrder(registerOrderId));
    }
}
