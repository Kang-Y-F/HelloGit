package com.neusoft.demo.service;

import com.neusoft.demo.entity.RegisterOrder;

import java.util.List;

public interface RegisterOrderService {

    /** 挂号单详情 */
    RegisterOrder getById(Long id);

    /** 患者所有挂号记录 */
    List<RegisterOrder> listByPatient(Long patientId);
}
