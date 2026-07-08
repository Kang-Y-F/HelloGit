package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegisterPriorityLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Integer oldPriority;

    private Integer newPriority;

    /** 操作人ID（挂号员），暂无员工登录体系时为 null */
    private Long operatorId;

    private String reason;

    private LocalDateTime createTime;
}