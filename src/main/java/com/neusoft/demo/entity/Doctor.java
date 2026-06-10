package com.neusoft.demo.entity;

import lombok.Data;

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