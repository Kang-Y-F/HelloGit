package com.neusoft.demo.dto;

import lombok.Data;

@Data
public class RegisterOrderDTO {

    private Long doctorId;

    private Long scheduleId;

    private Integer priority;
}