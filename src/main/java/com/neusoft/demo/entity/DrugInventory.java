package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("drug_inventory")
public class DrugInventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long drugId;
    private Integer stockQty;
    private Integer safetyQty;
    private Integer totalIn;
    private Integer totalOut;
    private LocalDateTime lastInTime;
    private LocalDateTime lastOutTime;
    private LocalDateTime updateTime;
}
