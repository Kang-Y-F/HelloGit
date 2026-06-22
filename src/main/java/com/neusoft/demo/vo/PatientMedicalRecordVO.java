package com.neusoft.demo.vo;

import lombok.Data;
import java.util.List;

/**
 * 患者端 - 我的病历列表VO
 */
@Data
public class PatientMedicalRecordVO {
    // 病历ID
    private Long id;
    // 就诊日期（展示用）
    private String visitDate;
    // 接诊医生姓名
    private String doctorName;
    // 医生职称
    private String doctorTitle;
    // 主诉
    private String chiefComplaint;
    // 诊断结果
    private String diagnosis;
    // 处方/用药
    private String prescription;
    // 检查项目列表
    private List<String> checkItems;
    // 病历状态
    private String status;
}