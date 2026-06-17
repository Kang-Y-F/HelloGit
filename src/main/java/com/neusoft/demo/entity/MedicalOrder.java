package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_order")
public class MedicalOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long registerOrderId;

    private Long patientId;

    private Long doctorId;

    /** 1检查 2检验 3用药 */
    private Integer orderType;

    /** 0待执行 1执行中 2已完成 3作废 */
    private Integer execStatus;

    private LocalDateTime createTime;
}
