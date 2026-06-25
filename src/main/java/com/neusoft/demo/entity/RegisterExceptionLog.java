package com.neusoft.demo.entity;

import lombok.Data;

import java.util.Date;

@Data
public class RegisterExceptionLog {

    private Long id;

    private Long userId;
    private Long doctorId;
    private Long scheduleId;

    /**
     * 1重复挂号 2超额挂号 3规则违规
     */
    private Integer exceptionType;

    private String exceptionMsg;

    private String orderNo;

    private Date createTime;
}
