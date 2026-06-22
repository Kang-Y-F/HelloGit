package com.neusoft.demo.vo;

import lombok.Data;

@Data
public class ScheduleVO {

    private Long scheduleId;

    private String fullDate;

    private String date;

    private String week;

    private String time;

    private Double fee;

    private Integer remaining;
}