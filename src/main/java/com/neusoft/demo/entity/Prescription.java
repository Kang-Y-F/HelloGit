package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("prescription")
public class Prescription {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String drugCode;

    private String drugName;

    private String dosage;

    private String drugUsage;

    /** 0正常 1作废 2已发药 */
    private Integer prescStatus;
}
