package com.neusoft.demo.service;

import com.neusoft.demo.dto.CtAiConfirmDTO;
import com.neusoft.demo.entity.CheckReport;

public interface CtAnalysisService {

    /** 生成AI分析 */
    CheckReport generateAiAnalysis(Long reportId);

    /** 医生确认/修改/驳回 */
    boolean confirmAiAnalysis(Long reportId, CtAiConfirmDTO dto);
}
