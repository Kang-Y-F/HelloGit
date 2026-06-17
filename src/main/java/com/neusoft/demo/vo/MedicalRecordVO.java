package com.neusoft.demo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 病历详情（含患者基本信息 + AI建议）
 */
@Data
public class MedicalRecordVO {

    private Long id;

    private Long userId;
    private String patientName;
    private String patientPhone;
    private Integer gender;

    private Long doctorId;
    private String doctorName;

    private String chiefComplaint;
    private String presentHistory;
    private String checkResult;
    private String diagnosis;

    private String aiDiagnosis;
    private String aiCheckAdvice;
    private String aiDrugAdvice;

    /** 0未确认 1已确认 2修改后确认 3驳回 */
    private Integer aiConfirmStatus;

    private LocalDateTime createTime;
}
