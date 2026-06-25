package com.neusoft.demo.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RegisterFeeRule {


    private Long id;


    /**
     * 医生职称
     */
    private String title;


    /**
     * 挂号费用
     */
    private BigDecimal price;


    private LocalDateTime createTime;

}