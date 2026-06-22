package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pmi_patient")
public class PmiPatient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String phone;

    /** 0女 1男 */
    private Integer gender;

    private LocalDate birthDate;

    private String idCard;

    private String avatar;

    private LocalDateTime createTime;

    private String password;
}
