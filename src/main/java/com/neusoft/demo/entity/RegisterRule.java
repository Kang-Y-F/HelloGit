package com.neusoft.demo.entity;

import lombok.Data;

import java.util.Date;

@Data
public class RegisterRule {

    private Long id;

    /** 是否允许超额挂号 */
    private Integer allowOverbook;

    /** 可提前预约天数 */
    private Integer maxDaysAhead;

    /** 同一患者限制次数 */
    private Integer repeatLimit;

    /** 是否允许急诊插队 */
    private Integer emergencyPriority;

    private Date updateTime;
}