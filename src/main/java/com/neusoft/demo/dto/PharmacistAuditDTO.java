package com.neusoft.demo.dto;
import lombok.Data;

@Data
public class PharmacistAuditDTO {
    private Long prescriptionId;
    /** 1通过 2拒绝 */
    private Integer status;
    private String remark;
}
