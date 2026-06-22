package com.neusoft.demo.vo;

import com.neusoft.demo.entity.Doctor;
import lombok.Data;
import java.util.List;

/**
 * 智能导诊 - AI分析返回VO
 */
@Data
public class GuideAnalyzeVO {
    // 建议就诊科室
    private String suggestedDept;
    // AI分析文案
    private String analysis;
    // 紧急程度：normal / emergency
    private String urgency;
    // 推荐医生列表
    private List<Doctor> recommendedDoctors;
}