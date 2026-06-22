package com.neusoft.demo.dto;

import lombok.Data;

@Data
public class RegisterCreateDTO {
    private Long patientId;
    private Long doctorId;
    private Long scheduleId;
    private Integer priority;
    private Double price;
}
