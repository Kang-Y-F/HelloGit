package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("drug_inout_record")
public class DrugInoutRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordNo;
    private Long drugId;
    private String drugName;
    /** 1采购入库 2发药出库 3报损 4退货入库 5调拨入库 6盘点调整 */
    private Integer recordType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Integer beforeStock;
    private Integer afterStock;
    private Long refId;
    private String refNo;
    private String supplier;
    private String batchNo;
    private LocalDate expiryDate;
    private String reason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;
}
