package com.neusoft.demo.dto;
import lombok.Data;

@Data
public class DrugOutDTO {
    private Long drugId;
    private Integer quantity;
    private Integer recordType;  // 3报损 4退货入库 6盘点
    private String reason;
}
