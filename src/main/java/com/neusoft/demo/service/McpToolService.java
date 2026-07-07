package com.neusoft.demo.service;

import java.util.List;

/**
 * MCP工具调用服务
 */
public interface McpToolService {

    /** 通用问答（使用全部MCP工具） */
    String answerWithMcpTools(String question);

    /** 辅助诊断（查病历 + 查医学知识） */
    String assistDiagnosisWithMcp(Long patientId, String symptoms);

    /** 智能导诊（查医学知识） */
    String guidePatientWithMcp(String symptoms);

    /** 导入华佗数据集 */
    String importHuatuoDataset(String filePath);

    /** 检验结果解读（查医学知识） */
    String interpretLabResultsWithMcp(List<String> abnormalItems, String patientSymptoms);

    // ========== 新增 ==========

    /** 智能分诊（查医学知识） */
    String triageWithMcp(String symptoms);

    /** CT影像AI分析（查医学知识） */
    String analyzeCtWithMcp(Long reportId, String patientInfo, String artifactInfo,
                            String recordInfo, String labInfo);

    /** 病历诊疗建议生成（查医学知识 + 数据库候选项目/药品清单） */
    String generateAdviceWithMcp(String chiefComplaint, String presentHistory, String checkResult,
                                 String checkCandidates, String labCandidates, String drugCandidates);
}