package com.neusoft.demo.vo;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PatientPrescriptionVO {
    // 处方主键
    private Long id;
    // 医嘱ID
    private Long medicalOrderId;
    // 药品名称
    private String drugName;
    // 剂量
    private String dosage;
    // 服用频次
    private String frequency;
    // 服用周期
    private String duration;
    // 就诊医生
    private String doctorName;
    // 就诊日期
    private String visitDate;
    // 主诉（简要病情）
    private String chiefComplaint;
}