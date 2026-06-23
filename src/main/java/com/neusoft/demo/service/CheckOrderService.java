package com.neusoft.demo.service;

import com.neusoft.demo.vo.CheckOrderVO;

import java.util.List;

/**
 * 检查/检验单 Service
 */
public interface CheckOrderService {

    /**
     * 待缴费检查单列表（供挂号台使用）
     * @param patientId 患者ID（可选）
     * @param keyword   患者姓名/手机号关键词（可选）
     */
    List<CheckOrderVO> listPendingPayment(Long patientId, String keyword);

    /**
     * 更新检查单状态（附带状态流转校验）
     * 允许：1→3, 3→4, 0/1→2
     */
    void updateStatus(Long checkOrderId, Integer newStatus);

    /**
     * 按挂号单查所有检查单（含缴费状态）
     */
    List<CheckOrderVO> listByRegisterOrder(Long registerOrderId);
}
