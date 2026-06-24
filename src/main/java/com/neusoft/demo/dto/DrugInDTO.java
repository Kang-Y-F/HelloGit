package com.neusoft.demo.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DrugInDTO {
    private Long drugId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String supplier;
    private String batchNo;
    private LocalDate expiryDate;
    private String remark;
}

