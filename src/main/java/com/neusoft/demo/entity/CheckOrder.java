package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("check_order")
public class CheckOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联病历ID（medical_record.id） */
    private Long recordId;

    /** 关联医嘱ID（medical_order.id） */
    private Long orderId;

    /** 患者ID */
    private Long userId;

    /** 开单医生ID */
    private Long doctorId;

    /** 检查项目ID（check_item.id） */
    private Long itemId;

    /**
     * 医嘱类型：1=检查  2=检验
     */
    private Integer orderType;

    /**
     * 检查单状态：
     *   0=待缴费
     *   1=已缴费/待执行
     *   2=已取消
     *   3=执行中
     *   4=已完成（结果已回传）
     */
    private Integer status;

    private LocalDateTime createTime;
}
