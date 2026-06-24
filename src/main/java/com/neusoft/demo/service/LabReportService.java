package com.neusoft.demo.service;

import com.neusoft.demo.dto.LabReportDTO;
import com.neusoft.demo.entity.LabReport;
import com.neusoft.demo.vo.CheckOrderVO;

import java.util.List;
import java.util.Map;

public interface LabReportService {

    /**
     * 查询待执行的检验单（status=1 已缴费/待执行，orderType=2 检验）
     */
    List<CheckOrderVO> listPendingLabOrders(String keyword);

    /**
     * 录入检验报告
     * 自动判断异常（检测值超出参考范围则标记 abnormal_flag=1）
     * 录入后将 check_order.status 更新为 4（已完成）
     */
    LabReport createReport(Long operatorId, LabReportDTO dto);

    /**
     * 审核检验报告（1通过 2驳回）
     */
    boolean auditReport(Long reportId, Integer auditStatus);

    /**
     * 按患者查询检验报告
     */
    List<LabReport> listByPatient(Long patientId);

    /**
     * AI 异常解读（简单的异常指标分析，生成一句话描述写入 report_content）
     */
    String generateAiSummary(Long reportId);

    /**
     * 血糖趋势预测（调用Python服务）
     * 返回历史数据 + 预测值
     */
    Map<String, Object> predictBloodSugar(Long patientId);
}
