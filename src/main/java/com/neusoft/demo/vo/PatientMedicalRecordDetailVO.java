package com.neusoft.demo.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 患者端 - 病历完整详情VO（供患者端查看完整病历）
 */
@Data
public class PatientMedicalRecordDetailVO {

    // ========== 基础信息 ==========
    private Long id;
    private String visitDate;
    private String doctorName;
    private String doctorTitle;
    private String departmentName;

    // ========== 病历内容 ==========
    private String chiefComplaint;      // 主诉
    private String presentHistory;      // 现病史
    private String diagnosis;            // 诊断结果

    // ========== AI辅助信息 ==========
    private String aiDiagnosis;          // AI诊断建议
    private String aiCheckAdvice;        // AI检查建议
    private String aiDrugAdvice;         // AI用药建议
    private Integer aiConfirmStatus;     // AI确认状态：0未确认 1已确认 2修改后确认 3驳回

    // ========== 检查报告（含图片）==========
    private List<CheckReportVO> checkReports;

    // ========== 检验报告 ==========
    private List<LabReportVO> labReports;

    // ========== 处方用药 ==========
    private List<PrescriptionVO> prescriptions;

    // ========== 时间信息 ==========
    private LocalDateTime createTime;

    // ========== 内嵌VO类 ==========

    @Data
    public static class CheckReportVO {
        private Long id;
        private String imgType;           // 检查类型（CT/X光等）
        private String imageUrl;          // 掩码图片URL
        private String ctUrl;             // 原始CT文件URL
        private String reportText;        // 报告文本
        private String aiAnalysis;        // AI辅助诊断分析
        private String doctorConfirmedText; // 医生确认后的最终结论
        private Integer aiConfirmStatus;  // AI确认状态
        private LocalDateTime createTime;
    }

    @Data
    public static class LabReportVO {
        private Long id;
        private String itemName;          // 检验项目名（血常规/血脂四项等）
        private String testValue;         // 检测值
        private String referenceRange;    // 参考范围
        private Integer abnormalFlag;     // 0正常 1异常
        private String reportContent;     // 详细内容JSON
        private Integer auditStatus;      // 0待审核 1已审核 2驳回
        private LocalDateTime createTime;
    }

    @Data
    public static class PrescriptionVO {
        private Long id;
        private String prescriptionNo;    // 处方编号
        private String drugName;          // 药品名称
        private String dosage;            // 单次剂量
        private Integer quantity;         // 数量
        private Integer days;             // 天数
        private String drugUsage;         // 用法
        private String auditResult;       // AI审方意见
        private Integer auditStatus;      // AI审方状态
        private LocalDateTime createTime;
    }
}
