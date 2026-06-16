package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("doctor")
@Data
public class Doctor {

    private Long id;

    private String name;

    private Long deptId;

    private String title;

    private String skills;

    private String avatar;

    private String username;

    private String password;

    private Integer status;
}