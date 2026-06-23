package com.neusoft.demo.dto;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiAuditPrescriptionDTO {
    private Long patientId;
    /** 药品列表：[{drugId, drugName, dosage, quantity, days, drugUsage}, ...] */
    private List<Map<String, Object>> drugs;
}
