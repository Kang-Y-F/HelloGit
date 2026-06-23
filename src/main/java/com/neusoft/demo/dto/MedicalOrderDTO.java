package com.neusoft.demo.dto;

import lombok.Data;

import java.util.List;

/**
 * 开医嘱请求参数
 * 变更：新增 recordId 字段，用于在开检查/检验医嘱时同步写入 check_order 表
 */
@Data
public class MedicalOrderDTO {

    /** 挂号单ID */
    private Long registerOrderId;

    /** 病历ID（开检查/检验医嘱时必填，用于关联 check_order.record_id） */
    private Long recordId;

    /** 患者ID */
    private Long patientId;

    /** 医嘱类型：1检查 2检验 3用药 */
    private Integer orderType;

    /** 检查/检验项目ID（orderType=1或2时填，对应 check_item.id） */
    private Long itemId;

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
