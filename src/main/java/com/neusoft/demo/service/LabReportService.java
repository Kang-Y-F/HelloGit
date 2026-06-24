package com.neusoft.demo.service;

import com.neusoft.demo.dto.LabReportDTO;
import com.neusoft.demo.entity.LabReport;
import com.neusoft.demo.vo.CheckOrderVO;

import java.util.List;
import java.util.Map;

public interface LabReportService {

    List<CheckOrderVO> listPendingLabOrders(String keyword);

    LabReport createReport(Long operatorId, LabReportDTO dto);

    boolean auditReport(Long reportId, Integer auditStatus);

    List<LabReport> listByPatient(Long patientId);

    String generateAiSummary(Long reportId);

    Map<String, Object> predictBloodSugar(Long patientId);

    /** 查询当前操作员今日录入的检验报告 */
    List<LabReport> listTodayByOperator(Long operatorId);

    /** 修改后确认（auditStatus=3 + 写入修改内容） */
    boolean confirmWithEdit(Long reportId, Integer auditStatus, String editedContent);

}
