package com.neusoft.demo.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("pmi_patient")
@Data
public class PmiPatient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String name;

    private String idCard;

    private Integer gender;

    private LocalDate birthDate;

    private String avatar;

    private LocalDateTime createTime;

    private String password;

    public PmiPatient() {
    }


}