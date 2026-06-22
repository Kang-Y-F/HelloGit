package com.neusoft.demo.vo;

import lombok.Data;

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
    private LocalDateTime visitTime;

    // 状态
    private Integer status;
}
