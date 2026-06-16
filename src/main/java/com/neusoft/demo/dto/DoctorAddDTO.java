package com.neusoft.demo.dto;

import lombok.Data;

@Data
public class DoctorAddDTO {

    private String name;

    private String username;

    private String password;

    private Long deptId;

    private String title;

    private String skills;
}