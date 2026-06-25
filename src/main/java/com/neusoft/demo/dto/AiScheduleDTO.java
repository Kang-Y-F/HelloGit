package com.neusoft.demo.dto;

import lombok.Data;

@Data
public class AiScheduleDTO {

    private Long doctorId;

    private String timeSlot;

    private String reason;
}
