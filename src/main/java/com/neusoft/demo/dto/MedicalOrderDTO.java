package com.neusoft.demo.dto;

import lombok.Data;

import java.util.List;

/**
 * 开医嘱请求参数
 */
@Data
public class MedicalOrderDTO {

    private Long registerOrderId;

    private Long patientId;

    /** 1检查 2检验 3用药 */
    private Integer orderType;

    /** 用药医嘱时附带处方列表 */
    private List<PrescriptionItemDTO> prescriptions;

    @Data
    public static class PrescriptionItemDTO {
        private String drugCode;
        private String drugName;
        private String dosage;
        private String drugUsage;
    }
}
