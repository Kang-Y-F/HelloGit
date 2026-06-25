package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("doctor")
@Data
public class Doctor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long deptId;

    private String title;

    private String skills;

    private String avatar;

    private String username;

    /** 角色：doctor / registrar / pharmacist / admin */
    private String role;

    private String password;

    private Integer status;

    private Integer auditStatus;
}