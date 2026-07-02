package com.neusoft.demo.service;

import com.neusoft.demo.dto.*;
import com.neusoft.demo.entity.LabReport;
import com.neusoft.demo.vo.CheckOrderVO;
import java.util.List;
import java.util.Map;

public interface LabReportService {

    /** 查询待执行检验单 */
    List<Map<String, Object>> listPendingLabOrders(String keyword);

    /** 单项检验录入（兼容旧接口，内部也会触发AI解读） */
    LabReport createReport(Long operatorId, LabReportDTO dto);

    /** 套餐检验录入（多子项，提交后同步生成AI综合解读） */
    List<LabReport> createSuiteReport(Long operatorId, LabReportSuiteDTO dto);

    /** 审核检验报告 */
    boolean auditReport(Long reportId, Integer auditStatus);

    /** 修改后确认 */
    boolean confirmWithEdit(Long reportId, Integer auditStatus, String editedContent);

    /** 按患者查询所有检验报告 */
    List<LabReport> listByPatient(Long patientId);

    /** 今日录入列表 */
    List<LabReport> listTodayByOperator(Long operatorId);

    /** 对已录入的单条报告触发AI解读（用于手动重新解读） */
    String generateAiSummary(Long reportId);

    String generateAiPreview(AiPreviewRequest req);

    /** 获取患者所有有历史记录的指标名列表 */
    List<String> getAvailableIndicators(Long patientId);

    /** 获取某指标历史趋势 + Python预测 */
    Map<String, Object> getTrend(Long patientId, String indicator, String subItem);

    /** 批量写入（HL7仿真/CGM数据用） */
    BatchLabReportCreateResponse batchCreate(BatchLabReportCreateRequest request);
}