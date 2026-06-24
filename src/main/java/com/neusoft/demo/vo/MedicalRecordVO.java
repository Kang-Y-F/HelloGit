package com.neusoft.demo.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 病历展示对象（含本次就诊的检查/检验摘要）
 */
@Data
public class MedicalRecordVO {

    private Long id;
    private Long userId;
    private Long registerOrderId;
    private Long doctorId;
    private String doctorName;
    private String patientName;
    private String patientPhone;
    private Integer gender;

    private String chiefComplaint;
    private String presentHistory;
    private String checkResult;
    private String diagnosis;

    private String aiDiagnosis;
    private String aiCheckAdvice;
    private String aiDrugAdvice;
    private Integer aiConfirmStatus;

    private LocalDateTime createTime;

    // ── 本次就诊关联的检查/检验摘要（新增） ──

    /** 本次就诊的检查报告（CT等） */
    private List<CheckSummary> checkReports;

    /** 本次就诊的检验报告（血常规等） */
    private List<LabSummary> labReports;
    @Data
    public static class CheckSummary {
        private Long id;
        private String imgType;
        private String reportText;
        private Integer aiConfirmStatus;
        private String doctorConfirmedText;
        /** AI分析原文 */
        private String aiAnalysis;
        /** 掩码图URL（用于缩略图展示） */
        private String imageUrl;
        /** 原始CT URL（用于跳转查看） */
        private String ctUrl;
        /** 伪影结果JSON */
        private String artifactResult;
        /** 患者ID（用于跳转） */
        private Long patientId;
    }

    @Data
    public static class LabSummary {
        private Long id;
        private String itemName;
        private String testValue;
        private String referenceRange;
        private Integer abnormalFlag;
        private Integer auditStatus;
    }
}
