package com.neusoft.demo.service;

import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.vo.RegisterOrderVO;

import java.util.List;

public interface RegisterOrderService {

    /** 挂号单详情 */
    RegisterOrder getById(Long id);

    /** 患者所有挂号记录 */
    List<RegisterOrderVO> listByPatient(Long patientId);

    /** 在线挂号 */
    RegisterOrder addRegisterOrder(
            Long userId,
            Long doctorId,
            Long scheduleId,
            Integer priority
    );

    /** 取消挂号 */
    String cancelOrder(Long userId, Long orderId);

    /**
     * 患者端：挂号单详情
     */
    RegisterOrderVO patientDetail(Long id);

    /**
     * 挂号员修改优先级（普通/急诊互改）
     * @param operatorId 操作人ID，暂无员工登录体系时可为 null
     */
    String updatePriority(Long operatorId, Long orderId, Integer newPriority, String reason);
}
