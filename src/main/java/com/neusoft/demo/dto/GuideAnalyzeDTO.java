package com.neusoft.demo.dto;

import lombok.Data;
import java.util.List;

/**
 * 智能导诊 - 症状分析入参
 */
@Data
public class GuideAnalyzeDTO {
    // 症状列表
    private List<String> symptoms;
    // 症状持续时间
    private String duration;
}