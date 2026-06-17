package com.neusoft.demo.service;

import com.neusoft.demo.dto.MedicalOrderDTO;
import com.neusoft.demo.entity.MedicalOrder;

import java.util.List;

public interface MedicalOrderService {

    /** 开医嘱（含处方），返回新建医嘱ID */
    Long create(Long doctorId, MedicalOrderDTO dto);

    /** 查询挂号单下所有医嘱 */
    List<MedicalOrder> listByRegisterOrder(Long registerOrderId);
}
