package com.neusoft.demo.service;

import com.neusoft.demo.dto.PaymentDTO;
import com.neusoft.demo.dto.RefundDTO;
import com.neusoft.demo.entity.PaymentRecord;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    /** 收费（挂号员代办收银） */
    PaymentRecord collectPayment(PaymentDTO dto, Long operatorId);

    /** 退款（取消挂号或医保拒付时） */
    boolean refund(RefundDTO dto, Long operatorId);

    /** 查询收费记录 */
    List<PaymentRecord> listPayments(Map<String, Object> filter);

    /** 今日收费统计 */
    Map<String, Object> todayStats(Long operatorId);

    List<Map<String, Object>> listPendingPrescription(String keyword);
}
