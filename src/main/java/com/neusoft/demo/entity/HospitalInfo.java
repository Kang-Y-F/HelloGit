package com.neusoft.demo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HospitalInfo {


    private Long id;

    /** 医院名称 */
    private String hospitalName;

    /** 地址 */
    private String address;

    /** 联系电话 */
    private String phone;

    /** 简介 */
    private String intro;


    private LocalDateTime updateTime;

}