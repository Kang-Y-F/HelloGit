package com.neusoft.demo.service;

import com.neusoft.demo.dto.GuideAnalyzeDTO;
import com.neusoft.demo.vo.GuideAnalyzeVO;

public interface AiGuideService {
    GuideAnalyzeVO analyzeSymptoms(GuideAnalyzeDTO dto, Long patientId);
}