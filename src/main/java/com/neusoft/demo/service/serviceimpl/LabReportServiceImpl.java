package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.demo.dto.*;
import com.neusoft.demo.entity.CheckOrder;
import com.neusoft.demo.entity.LabReport;
import com.neusoft.demo.mapper.CheckOrderMapper;
import com.neusoft.demo.mapper.LabReportMapper;
import com.neusoft.demo.mapper.MedicalOrderMapper;
import com.neusoft.demo.service.LabReportService;
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
public class LabReportServiceImpl
        extends ServiceImpl<LabReportMapper, LabReport>
        implements LabReportService {

    @Autowired private LabReportMapper    labReportMapper;
    @Autowired private CheckOrderMapper   checkOrderMapper;
    @Autowired private MedicalOrderMapper medicalOrderMapper;
    @Autowired private ChatClient         chatClient;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.predict.service.url}")
    private String pythonPredictServiceUrl;

    public LabReportServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ════════════════════════════════════════════════════════════
    //  待执行检验单
    // ════════════════════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> listPendingLabOrders(String keyword) {
        return labReportMapper.selectPendingLabOrders(keyword);
    }

    // ════════════════════════════════════════════════════════════
    //  单项检验录入（兼容旧接口）
    //  内部包装成单子项调用套餐接口，保持逻辑统一
    // ════════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LabReport createReport(Long operatorId, LabReportDTO dto) {
        LabReportSuiteDTO suiteDTO = new LabReportSuiteDTO();
        suiteDTO.setCheckOrderId(dto.getCheckOrderId());
        suiteDTO.setItemName(dto.getItemName());

        LabReportSuiteDTO.SubItem sub = new LabReportSuiteDTO.SubItem();
        sub.setSubItemName(null);               // 单项时 subItemName 为 null
        sub.setTestValue(dto.getTestValue());
        sub.setReferenceRange(dto.getReferenceRange());
        sub.setDescription(dto.getDescription());
        suiteDTO.setSubItems(List.of(sub));

        List<LabReport> saved = createSuiteReport(operatorId, suiteDTO);
        return saved.get(0);
    }

    // ════════════════════════════════════════════════════════════
    //  套餐检验录入（多子项 + 提交后同步AI解读）
    // ════════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<LabReport> createSuiteReport(Long operatorId, LabReportSuiteDTO dto) {

        // 1. 校验检验单
        CheckOrder checkOrder = checkOrderMapper.selectById(dto.getCheckOrderId());
        if (checkOrder == null)
            throw new RuntimeException("检验单不存在: " + dto.getCheckOrderId());
        if (checkOrder.getStatus() != 1 && checkOrder.getStatus() != 3)
            throw new RuntimeException("该检验单状态异常，无法录入（当前状态：" + checkOrder.getStatus() + "）");

        boolean isSuite = dto.getSubItems().size() > 1;
        // 套餐用同一个 suiteGroup；单项直接用 null
        String suiteGroup = isSuite
                ? UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                : null;

        List<LabReport> saved = new ArrayList<>();

        // 2. 逐子项写库
        for (LabReportSuiteDTO.SubItem sub : dto.getSubItems()) {
            LabReport report = new LabReport();
            report.setOrderId(dto.getCheckOrderId());
            report.setOrderMainId(checkOrder.getOrderId());
            report.setPatientId(checkOrder.getUserId());
            report.setItemName(dto.getItemName());
            report.setSubItemName(sub.getSubItemName());
            report.setTestValue(sub.getTestValue());
            report.setReferenceRange(sub.getReferenceRange());
            report.setAbnormalFlag(isAbnormal(sub.getTestValue(), sub.getReferenceRange()) ? 1 : 0);
            report.setAuditStatus(0);
            report.setOperatorId(operatorId);
            report.setSuiteGroup(suiteGroup);
            report.setCreateTime(LocalDateTime.now());

            // 描述存入 report_content（description 字段不在实体里）
            if (sub.getDescription() != null && !sub.getDescription().isBlank()) {
                report.setReportContent(toReportContent(sub.getDescription()));
            }

            labReportMapper.insert(report);
            saved.add(report);
        }

        // 3. 更新检验单状态
        checkOrderMapper.updateStatus(checkOrder.getId(), 4);
        if (checkOrder.getOrderId() != null)
            medicalOrderMapper.updateExecStatus(checkOrder.getOrderId(), 2);

        // 4. 提交后同步触发 AI 解读
        //    AI 结果存在第一条记录的 report_content 里，其他子项通过 suite_group 共享
        try {
            String aiResult = generateSuiteAiSummary(
                    saved, checkOrder.getUserId(), dto.getItemName());
            LabReport first = saved.get(0);
            first.setReportContent(toReportContent(aiResult));
            labReportMapper.updateById(first);
            // 让调用方能直接拿到 AI 结果，不需要再查库
            first.setReportContent(toReportContent(aiResult));
        } catch (Exception e) {
            log.warn("AI解读生成失败，suiteGroup={}, itemName={}",
                    suiteGroup, dto.getItemName(), e);
            // AI 失败不影响录入成功，前端可以手动点"AI解读"重新生成
        }

        return saved;
    }

    // ════════════════════════════════════════════════════════════
    //  AI 解读（套餐综合解读，内部方法）
    // ════════════════════════════════════════════════════════════

    private String generateSuiteAiSummary(
            List<LabReport> subItems, Long patientId, String suiteName) {

        // 拼当前各子项结果
        StringBuilder current = new StringBuilder();
        for (LabReport r : subItems) {
            String label = r.getSubItemName() != null ? r.getSubItemName() : r.getItemName();
            current.append(String.format("  %s：%s（参考 %s，%s）\n",
                    label, r.getTestValue(), r.getReferenceRange(),
                    r.getAbnormalFlag() == 1 ? "⚠异常" : "正常"));
        }

        // 查该患者同一套餐最近历史（最多15条子项记录 ≈ 3次复查）
        List<LabReport> history = labReportMapper.selectList(
                new LambdaQueryWrapper<LabReport>()
                        .eq(LabReport::getPatientId, patientId)
                        .eq(LabReport::getItemName, suiteName)
                        .orderByDesc(LabReport::getCreateTime)
                        .last("LIMIT 15"));

        StringBuilder histInfo = new StringBuilder();
        if (history.isEmpty()) {
            histInfo.append("该患者首次进行本套餐检验，无历史对比数据。\n");
        } else {
            for (LabReport h : history) {
                String label = h.getSubItemName() != null ? h.getSubItemName() : h.getItemName();
                histInfo.append(String.format("  %s %s：%s（%s）\n",
                        h.getCreateTime().toString().substring(0, 10),
                        label, h.getTestValue(),
                        h.getAbnormalFlag() == 1 ? "⚠异常" : "正常"));
            }
        }

        String prompt = String.format("""
                你是专业检验医学AI助手，请对以下检验结果做综合临床解读。
                
                检验套餐：%s
                
                本次结果：
                %s
                历史记录（供对比）：
                %s
                
                输出要求：
                1. 各异常项的临床意义（只说异常项，正常项一句带过）
                2. 整组指标的综合判断
                3. 随访/复查/进一步检查建议
                
                要求：简洁专业，不超过200字，使用纯文本无需 Markdown。
                """, suiteName, current, histInfo);

        return chatClient.prompt().user(prompt).call().content();
    }

    // ════════════════════════════════════════════════════════════
    //  手动触发 AI 解读（已录入记录，前端点"AI解读"按钮时调用）
    // ════════════════════════════════════════════════════════════

    @Override
    public String generateAiSummary(Long reportId) {
        LabReport report = labReportMapper.selectById(reportId);
        if (report == null) throw new RuntimeException("检验报告不存在");

        // 如果是套餐，取同组所有子项一起解读
        List<LabReport> subItems;
        if (report.getSuiteGroup() != null) {
            subItems = labReportMapper.selectList(
                    new LambdaQueryWrapper<LabReport>()
                            .eq(LabReport::getSuiteGroup, report.getSuiteGroup())
                            .orderByAsc(LabReport::getId));
        } else {
            subItems = List.of(report);
        }

        try {
            String aiResult = generateSuiteAiSummary(
                    subItems, report.getPatientId(), report.getItemName());
            // 写入第一条（对套餐而言是 reportId 指向的那条，对单项就是它自己）
            report.setReportContent(toReportContent(aiResult));
            labReportMapper.updateById(report);
            return aiResult;
        } catch (Exception e) {
            log.error("AI解读失败 reportId={}", reportId, e);
            throw new RuntimeException("AI服务暂时不可用");
        }
    }

    // ════════════════════════════════════════════════════════════
    //  审核 / 修改后确认
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean auditReport(Long reportId, Integer auditStatus) {
        LabReport report = labReportMapper.selectById(reportId);
        if (report == null) return false;

        // 套餐场景：同步更新同组所有子项的审核状态，保持一致
        if (report.getSuiteGroup() != null) {
            List<LabReport> siblings = labReportMapper.selectList(
                    new LambdaQueryWrapper<LabReport>()
                            .eq(LabReport::getSuiteGroup, report.getSuiteGroup()));
            for (LabReport s : siblings) {
                s.setAuditStatus(auditStatus);
                labReportMapper.updateById(s);
            }
            return true;
        }

        report.setAuditStatus(auditStatus);
        return labReportMapper.updateById(report) > 0;
    }

    @Override
    public boolean confirmWithEdit(Long reportId, Integer auditStatus, String editedContent) {
        LabReport report = labReportMapper.selectById(reportId);
        if (report == null) return false;

        report.setAuditStatus(auditStatus);
        if (editedContent != null && !editedContent.isBlank()) {
            report.setReportContent(toReportContent(editedContent));
        }

        // 同步更新同组其他子项的审核状态
        if (report.getSuiteGroup() != null) {
            labReportMapper.selectList(
                            new LambdaQueryWrapper<LabReport>()
                                    .eq(LabReport::getSuiteGroup, report.getSuiteGroup())
                                    .ne(LabReport::getId, reportId))
                    .forEach(s -> {
                        s.setAuditStatus(auditStatus);
                        labReportMapper.updateById(s);
                    });
        }

        return labReportMapper.updateById(report) > 0;
    }

    // ════════════════════════════════════════════════════════════
    //  查询类
    // ════════════════════════════════════════════════════════════

    @Override
    public List<LabReport> listByPatient(Long patientId) {
        return labReportMapper.selectList(
                new LambdaQueryWrapper<LabReport>()
                        .eq(LabReport::getPatientId, patientId)
                        .orderByDesc(LabReport::getCreateTime));
    }

    @Override
    public List<LabReport> listTodayByOperator(Long operatorId) {
        return labReportMapper.selectList(
                new LambdaQueryWrapper<LabReport>()
                        .eq(LabReport::getOperatorId, operatorId)
                        .apply("DATE(create_time) = CURDATE()")
                        .orderByDesc(LabReport::getCreateTime));
    }

    @Override
    public List<String> getAvailableIndicators(Long patientId) {
        return labReportMapper.selectDistinctItems(patientId);
    }

    // ════════════════════════════════════════════════════════════
    //  趋势预测（调用 Python 服务）
    // ════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> getTrend(Long patientId, String indicator) {
        List<LabReport> list = labReportMapper.selectList(
                new LambdaQueryWrapper<LabReport>()
                        .eq(LabReport::getPatientId, patientId)
                        .like(LabReport::getItemName, indicator)
                        .orderByAsc(LabReport::getCreateTime));

        List<Map<String, Object>> history = new ArrayList<>();
        for (LabReport l : list) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", l.getCreateTime().toString().substring(0, 10));
            p.put("time", l.getCreateTime().toString());
            p.put("value", parseDouble(l.getTestValue()));
            p.put("abnormal", l.getAbnormalFlag() == 1);
            p.put("referenceRange", l.getReferenceRange());
            p.put("subItemName", l.getSubItemName());
            history.add(p);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("indicator", indicator);
        result.put("patientId", patientId);
        result.put("history", history);
        result.put("count", history.size());

        if (history.size() >= 2) {
            String refRange = (String) history.get(0).get("referenceRange");
            Map<String, Object> predictResult =
                    callPythonPredictService(history, refRange, indicator);
            if (predictResult != null) {
                result.put("predictions", predictResult.get("predictions"));
                result.put("trend", predictResult.get("trend"));
                result.put("referenceRange", refRange);
            }
        }

        return result;
    }

    private Map<String, Object> callPythonPredictService(
            List<Map<String, Object>> history, String referenceRange, String indicator) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("history", history);
            body.put("referenceRange", referenceRange);
            body.put("indicator", indicator);
            body.put("steps", 3);
            body.put("granularity", "auto");

            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    pythonPredictServiceUrl + "/predict/trend", body, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return (Map<String, Object>) resp.getBody().get("data");
            }
        } catch (Exception e) {
            log.warn("调用Python预测服务失败，indicator={}", indicator, e);
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════
    //  批量写入（CGM/HL7仿真数据）
    // ════════════════════════════════════════════════════════════

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
                Long checkOrderId = item.getOrderId();
                report.setOrderId(checkOrderId);
                CheckOrder checkOrder = checkOrderMapper.selectById(checkOrderId);
                if (checkOrder == null) {
                    throw new RuntimeException("检验单ID[" + checkOrderId + "]不存在");
                }
                report.setOrderMainId(checkOrder.getOrderId());
                report.setPatientId(item.getPatientId());
                report.setItemName(item.getItemName());
                report.setSuiteGroup(item.getSuiteGroup());
                report.setSubItemName(item.getSubItemName());
                report.setTestValue(item.getTestValue());
                report.setReferenceRange(item.getReferenceRange());
                report.setOperatorId(item.getOperatorId());
                report.setAbnormalFlag(judgeAbnormal(item.getTestValue(), item.getReferenceRange()) ? 1 : 0);
                report.setAuditStatus(0);

                Map<String, Object> contentMap = new HashMap<>();
                contentMap.put("desc", item.getDescription() != null ? item.getDescription() : "");
                report.setReportContent(objectMapper.writeValueAsString(contentMap));

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
            boolean ok = saveBatch(toInsert, 200);
            if (ok) successCount = toInsert.size();
            else failReasons.add("批量写入数据库失败");
        }

        response.setSuccessCount(successCount);
        response.setFailCount(items.size() - successCount);
        response.setFailReasons(failReasons);
        return response;
    }

    // ════════════════════════════════════════════════════════════
    //  工具方法
    // ════════════════════════════════════════════════════════════

    private boolean isAbnormal(String testValue, String referenceRange) {
        try {
            double val = Double.parseDouble(testValue.trim());
            if (referenceRange != null && referenceRange.contains("-")) {
                String[] parts = referenceRange.split("-");
                double lo = Double.parseDouble(parts[0].trim());
                double hi = Double.parseDouble(parts[1].trim());
                return val < lo || val > hi;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean judgeAbnormal(String testValue, String referenceRange) {
        return isAbnormal(testValue, referenceRange);
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0.0; }
    }

    /** 把文本包装成 report_content JSON格式 */
    private String toReportContent(String text) {
        return "{\"desc\":\"" + text.replace("\"", "'").replace("\n", " ") + "\"}";
    }
}