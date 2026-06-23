package com.neusoft.demo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RegisterOrderVO {

    private Long id;

    // 患者
    private String patientName;

    // 医生
    private String doctorName;

    // 科室
    private String departmentName;

    // 就诊时间
    private LocalDate visitDate;

    private String timeSlot;

    // 状态
    private Integer status;

    // 挂号费（关键）
    private BigDecimal fee;

    // 订单号（关键）
    private String orderNo;
}
