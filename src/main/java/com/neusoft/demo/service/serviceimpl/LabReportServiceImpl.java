package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.demo.dto.BatchLabReportCreateRequest;
import com.neusoft.demo.dto.BatchLabReportCreateResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class LabReportServiceImpl extends ServiceImpl<LabReportMapper, LabReport> implements LabReportService {

    @Autowired private LabReportMapper    labReportMapper;
    @Autowired private CheckOrderMapper   checkOrderMapper;
    @Autowired private MedicalOrderMapper medicalOrderMapper;
    @Autowired private ChatClient         chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate;

    // 推荐用构造器注入，而不是 @Autowired 字段注入，方便测试且不会出现循环依赖问题
    public LabReportServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // application.yml 里配置 python.predict.service.url: http://127.0.0.1:8000
    @Value("${python.predict.service.url}")
    private String pythonPredictServiceUrl;
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

    /**
     * 查询当前操作员今日录入的检验报告
     */
    @Override
    public List<LabReport> listTodayByOperator(Long operatorId) {
        return labReportMapper.selectList(
                new LambdaQueryWrapper<LabReport>()
                        .eq(LabReport::getOperatorId, operatorId)
                        .apply("DATE(create_time) = CURDATE()")
                        .orderByDesc(LabReport::getCreateTime)
        );
    }

    @Override
    public boolean confirmWithEdit(Long reportId, Integer auditStatus, String editedContent) {
        LabReport report = labReportMapper.selectById(reportId);
        if (report == null) return false;

        report.setAuditStatus(auditStatus);
        if (editedContent != null && !editedContent.isBlank()) {
            // 把修改后的内容写入 report_content（覆盖AI原始解读）
            report.setReportContent("{\"desc\":\"" + editedContent.replace("\"", "'").replace("\n", " ") + "\"}");
        }
        return labReportMapper.updateById(report) > 0;
    }

    @Override
    public List<String> getAvailableIndicators(Long patientId) {
        return labReportMapper.selectDistinctItems(patientId);
    }

    @Override
    public Map<String, Object> getTrend(Long patientId, String indicator) {
        List<LabReport> list = labReportMapper.selectList(
                new LambdaQueryWrapper<LabReport>()
                        .eq(LabReport::getPatientId, patientId)
                        .like(LabReport::getItemName, indicator)
                        .orderByAsc(LabReport::getCreateTime)
        );

        List<Map<String, Object>> history = new ArrayList<>();
        for (LabReport l : list) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", l.getCreateTime().toString().substring(0, 10));
            p.put("time", l.getCreateTime().toString());
            p.put("value", parseDouble(l.getTestValue()));
            p.put("abnormal", l.getAbnormalFlag() == 1);
            p.put("referenceRange", l.getReferenceRange());
            history.add(p);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("indicator", indicator);
        result.put("patientId", patientId);
        result.put("history", history);
        result.put("count", history.size());

        // ── 原来手写线性预测的部分整段删除，改成调用 Python 预测服务 ──
        if (history.size() >= 2) {
            String refRange = (String) history.get(0).get("referenceRange");
            Map<String, Object> predictResult = callPythonPredictService(history, refRange, indicator);
            if (predictResult != null) {
                result.put("predictions", predictResult.get("predictions"));
                result.put("trend", predictResult.get("trend"));
                result.put("referenceRange", refRange);
            }
        }

        return result;
    }

    /** 调用 Python FastAPI 的 /predict/trend 接口 */
    private Map<String, Object> callPythonPredictService(
            List<Map<String, Object>> history, String referenceRange, String indicator) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("history", history);
            body.put("referenceRange", referenceRange);
            body.put("indicator", indicator);
            body.put("steps", 3);          // 需要预测几个点
            body.put("granularity", "auto"); // 单次复查 / CGM 由 Python 侧根据采样间隔自动判断，也可显式传 "cgm" / "labtest"

            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    pythonPredictServiceUrl + "/predict/trend",   // 配置到 application.yml
                    body,
                    Map.class
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return (Map<String, Object>) resp.getBody().get("data");
            }
        } catch (Exception e) {
            log.warn("调用Python预测服务失败，indicator={}, patientId history size={}",
                    indicator, history.size(), e);
        }
        return null; // 失败时不返回 predictions，前端会显示"暂无该指标历史数据"之外的正常 history
    }

    // LabReportServiceImpl.java 里新增
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchLabReportCreateResponse batchCreate(BatchLabReportCreateRequest request) {
        List<BatchLabReportCreateRequest.LabReportItem> items = request.getItems();
        BatchLabReportCreateResponse response = new BatchLabReportCreateResponse();

        if (items == null || items.isEmpty()) {
            response.setSuccessCount(0);
            response.setFailCount(0);
            response.setFailReasons(Collections.emptyList());
            return response;
        }

        List<LabReport> toInsert = new ArrayList<>();
        List<String> failReasons = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            BatchLabReportCreateRequest.LabReportItem item = items.get(i);
            try {
                LabReport report = new LabReport();
                report.setOrderId(item.getOrderId());
                report.setOrderMainId(item.getOrderMainId());
                report.setPatientId(item.getPatientId());
                report.setItemName(item.getItemName());
                report.setTestValue(item.getTestValue());
                report.setReferenceRange(item.getReferenceRange());
                report.setOperatorId(item.getOperatorId());

                // description 没有独立字段，按你实体类约定拼成 JSON 存进 reportContent
                Map<String, Object> contentMap = new HashMap<>();
                contentMap.put("desc", item.getDescription() != null ? item.getDescription() : "");
                report.setReportContent(objectMapper.writeValueAsString(contentMap));

                report.setAbnormalFlag(judgeAbnormal(item.getTestValue(), item.getReferenceRange()) ? 1 : 0);
                report.setAuditStatus(0); // 待审核

                if (item.getTestTime() != null && !item.getTestTime().isEmpty()) {
                    report.setCreateTime(LocalDateTime.parse(item.getTestTime()));
                } else {
                    report.setCreateTime(LocalDateTime.now());
                }

                toInsert.add(report);
            } catch (Exception e) {
                failReasons.add(String.format("第%d条数据处理失败：%s", i + 1, e.getMessage()));
            }
        }

        int successCount = 0;
        if (!toInsert.isEmpty()) {
            boolean ok = saveBatch(toInsert, 200); // 直接调用父类 ServiceImpl 的方法，不要加 this 之外的前缀
            if (ok) {
                successCount = toInsert.size();
            } else {
                failReasons.add("批量写入数据库失败");
            }
        }

        response.setSuccessCount(successCount);
        response.setFailCount(items.size() - successCount);
        response.setFailReasons(failReasons);
        return response;
    }

    private boolean judgeAbnormal(String testValue, String referenceRange) {
        try {
            double val = Double.parseDouble(testValue.trim());
            String[] parts = referenceRange.trim().split("-");
            double lo = Double.parseDouble(parts[0].trim());
            double hi = Double.parseDouble(parts[1].trim());
            return val < lo || val > hi;
        } catch (Exception e) {
            return false;
        }
    }
}
