package com.neusoft.demo.vo;

import lombok.Data;

/**
 * 患者个人信息视图对象（脱敏返回）
 */
@Data
public class PatientInfoVO {
    private Long id;
    private String name;
    private String phone;
    private String avatar; // 头像URL
}