package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("register_order")
public class RegisterOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long doctorId;

    private Long scheduleId;

    /** 0待支付 1候诊 2已取消 3就诊中 4已完成 */
    private Integer status;

    private BigDecimal price;

    /** 0普通 1急诊 */
    private Integer priority;

    private LocalDateTime createTime;
}
