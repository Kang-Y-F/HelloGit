package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.dto.LabReportDTO;
import com.neusoft.demo.entity.CheckOrder;
import com.neusoft.demo.entity.LabReport;
import com.neusoft.demo.entity.MedicalOrder;
import com.neusoft.demo.mapper.CheckOrderMapper;
import com.neusoft.demo.mapper.LabReportMapper;
import com.neusoft.demo.mapper.MedicalOrderMapper;
import com.neusoft.demo.service.LabReportService;
import com.neusoft.demo.vo.CheckOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class LabReportServiceImpl implements LabReportService {

    @Autowired private LabReportMapper    labReportMapper;
    @Autowired private CheckOrderMapper   checkOrderMapper;
    @Autowired private MedicalOrderMapper medicalOrderMapper;
    @Autowired private ChatClient         chatClient;

    /**
     * 查询待执行的检验单（check_order.status=1 且 order_type=2）
     */
    @Override
    public List<CheckOrderVO> listPendingLabOrders(String keyword) {
        // 复用 CheckOrderMapper 的联表查询，但这里需要 status=1 + orderType=2
        // 直接用 selectPendingPayment 只查 status=0，所以这里自定义查询
        return labReportMapper.selectPendingLabOrders(keyword);
    }

    /**
     * 录入检验报告
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LabReport createReport(Long operatorId, LabReportDTO dto) {

        // 1. 校验 check_order
        CheckOrder checkOrder = checkOrderMapper.selectById(dto.getCheckOrderId());
        if (checkOrder == null) {
            throw new RuntimeException("检验单不存在: " + dto.getCheckOrderId());
        }
        if (checkOrder.getStatus() != 1 && checkOrder.getStatus() != 3) {
            throw new RuntimeException("该检验单状态异常，无法录入（当前状态：" + checkOrder.getStatus() + "）");
        }

        // 2. 自动判断异常
        int abnormalFlag = isAbnormal(dto.getTestValue(), dto.getReferenceRange()) ? 1 : 0;

        // 3. 写 lab_report
        LabReport report = new LabReport();
        report.setOrderId(dto.getCheckOrderId());
        report.setOrderMainId(checkOrder.getOrderId());  // medical_order.id
        report.setPatientId(checkOrder.getUserId());
        report.setItemName(dto.getItemName());
        report.setTestValue(dto.getTestValue());
        report.setReferenceRange(dto.getReferenceRange());
        report.setAbnormalFlag(abnormalFlag);
        report.setAuditStatus(0);  // 待审核
        report.setOperatorId(operatorId);
        report.setCreateTime(LocalDateTime.now());

        // 组装 report_content
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            report.setReportContent("{\"desc\":\"" + dto.getDescription().replace("\"", "'") + "\"}");
        }

        labReportMapper.insert(report);

        // 4. 更新 check_order.status = 4（已完成）
        checkOrderMapper.updateStatus(checkOrder.getId(), 4);

        // 5. 更新 medical_order.exec_status = 2（已完成）
        if (checkOrder.getOrderId() != null) {
            medicalOrderMapper.updateExecStatus(checkOrder.getOrderId(), 2);
        }

        return report;
    }

    /**
     * 审核检验报告
     */
    @Override
    public boolean auditReport(Long reportId, Integer auditStatus) {
        LabReport report = labReportMapper.selectById(reportId);
        if (report == null) return false;

        report.setAuditStatus(auditStatus);
        return labReportMapper.updateById(report) > 0;
    }

    /**
     * 按患者查询
     */
    @Override
    public List<LabReport> listByPatient(Long patientId) {
        return labReportMapper.selectList(
            new LambdaQueryWrapper<LabReport>()
                .eq(LabReport::getPatientId, patientId)
                .orderByDesc(LabReport::getCreateTime)
        );
    }

    /**
     * AI 异常解读
     * 对单条检验报告做解读，生成临床意义描述
     */
    @Override
    public String generateAiSummary(Long reportId) {
        LabReport report = labReportMapper.selectById(reportId);
        if (report == null) throw new RuntimeException("检验报告不存在");

        // 查该患者所有检验报告（用于综合判断）
        List<LabReport> allLabs = labReportMapper.selectList(
            new LambdaQueryWrapper<LabReport>()
                .eq(LabReport::getPatientId, report.getPatientId())
                .orderByDesc(LabReport::getCreateTime)
                .last("LIMIT 10")
        );

        StringBuilder labInfo = new StringBuilder();
        for (LabReport l : allLabs) {
            labInfo.append(String.format("%s：%s（参考范围 %s，%s）\n",
                l.getItemName(), l.getTestValue(), l.getReferenceRange(),
                l.getAbnormalFlag() == 1 ? "⚠异常" : "正常"));
        }

        String prompt = String.format("""
            你是一名专业的检验医学AI助手。请对以下检验指标做临床解读。
            
            当前关注指标：%s = %s（参考范围 %s）
            
            该患者全部检验结果：
            %s
            
            请输出：
            1. 该指标当前值的临床意义（1-2句话）
            2. 结合其他指标的综合判断（如有异常组合模式则指出）
            3. 建议（是否需要复查或进一步检查）
            
            要求：简洁专业，不超过150字。
            """,
            report.getItemName(), report.getTestValue(),
            report.getReferenceRange(), labInfo
        );

        try {
            String result = chatClient.prompt().user(prompt).call().content();

            // 写入 report_content
            report.setReportContent("{\"desc\":\"" + result.replace("\"", "'").replace("\n", " ") + "\"}");
            labReportMapper.updateById(report);

            return result;
        } catch (Exception e) {
            log.error("AI检验解读失败 reportId={}", reportId, e);
            throw new RuntimeException("AI服务暂时不可用");
        }
    }

    /**
     * 血糖趋势预测
     * 查询患者历史血糖数据，调用 Python 服务做时序预测
     * 如果 Python 服务不可用，则仅返回历史数据 + 简单趋势
     */
    @Override
    public Map<String, Object> predictBloodSugar(Long patientId) {

        // 查历史血糖相关指标
        List<LabReport> glucoseList = labReportMapper.selectList(
            new LambdaQueryWrapper<LabReport>()
                .eq(LabReport::getPatientId, patientId)
                .like(LabReport::getItemName, "血糖")
                .orderByAsc(LabReport::getCreateTime)
        );

        List<Map<String, Object>> history = new ArrayList<>();
        for (LabReport l : glucoseList) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", l.getCreateTime().toString().substring(0, 10));
            point.put("value", parseDouble(l.getTestValue()));
            point.put("abnormal", l.getAbnormalFlag() == 1);
            history.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("history", history);
        result.put("count", history.size());

        // 尝试调用 Python 预测服务
        try {
            // TODO: 调用 Python 服务 POST http://localhost:5000/predict/blood-sugar
            // RestTemplate / WebClient 调用，传入历史数据，拿回预测曲线
            // 暂时用简单线性趋势替代

            if (history.size() >= 2) {
                double last = (double) history.get(history.size() - 1).get("value");
                double prev = (double) history.get(history.size() - 2).get("value");
                double trend = last - prev;
                String riskLevel = last > 7.0 ? "偏高风险" : (last > 6.1 ? "临界关注" : "正常范围");

                result.put("lastValue", last);
                result.put("trend", trend > 0 ? "上升" : (trend < 0 ? "下降" : "平稳"));
                result.put("trendValue", Math.round(trend * 100.0) / 100.0);
                result.put("riskLevel", riskLevel);
                result.put("predictedNext", Math.round((last + trend) * 100.0) / 100.0);
                result.put("source", "simple_linear");
            } else {
                result.put("riskLevel", "数据不足");
                result.put("source", "insufficient_data");
            }
        } catch (Exception e) {
            log.warn("血糖预测失败", e);
            result.put("riskLevel", "预测服务不可用");
            result.put("source", "error");
        }

        return result;
    }

    // ── 工具方法 ──────────────────────────────────────────────

    /**
     * 简单判断是否异常：解析参考范围如 "4.0-10.0"，判断检测值是否在范围内
     */
    private boolean isAbnormal(String testValue, String referenceRange) {
        try {
            double val = Double.parseDouble(testValue.trim());
            if (referenceRange != null && referenceRange.contains("-")) {
                String[] parts = referenceRange.split("-");
                double low  = Double.parseDouble(parts[0].trim());
                double high = Double.parseDouble(parts[1].trim());
                return val < low || val > high;
            }
        } catch (Exception ignored) {}
        return false;  // 无法判断时默认正常
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0.0; }
    }
}
