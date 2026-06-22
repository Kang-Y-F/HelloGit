package com.neusoft.demo.vo;

import lombok.Data;

@Data
public class PatientMessageVO {
    private Long id;
    private String title;
    private String content;
    private Integer msgType;
    private Integer readStatus;
    private String jumpPath;
    // 格式化日期 yyyy-MM-dd HH:mm
    private String createTime;
}