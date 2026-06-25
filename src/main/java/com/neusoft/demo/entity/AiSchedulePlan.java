package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ai_schedule_plan")
public class AiSchedulePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate planDate;

    private Long doctorId;

    private String timeSlot;

    private Integer expectedLoad;

    private Integer status;

    private LocalDateTime createTime;
}
