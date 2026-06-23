package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("drug")
public class Drug {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String drugCode;
    private String drugName;
    private String genericName;
    private String specification;
    private String manufacturer;
    private String unit;
    private String dosageForm;
    private String category;
    private BigDecimal price;
    /** 0非处方 1处方 */
    private Integer isPrescription;
    private String usageNotes;
    private String contraindication;
    private Integer status;
    private LocalDateTime createTime;
}
