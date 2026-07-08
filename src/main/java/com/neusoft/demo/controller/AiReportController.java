package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.AiComprehensiveReport;
import com.neusoft.demo.mapper.AiComprehensiveReportMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * AI 多模态综合诊疗报告
 * 调用 Python LangChain 服务生成报告，并落库缓存
 */
@Slf4j
@RestController
@RequestMapping("/ai-report")
public class AiReportController {

    private static final String PYTHON_BASE_URL = "http://localhost:8000";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Autowired
    private AiComprehensiveReportMapper aiComprehensiveReportMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestTemplate getRestTemplate() {
        if (restTemplate != null) return restTemplate;
        return new RestTemplate();
    }

    /** 检查患者数据是否就绪 */
    @GetMapping("/status/{patientId}")
    public Result<?> checkStatus(@PathVariable Long patientId) {
        try {
            String url = PYTHON_BASE_URL + "/ai/report-status/" + patientId;
            Map<String, Object> response = getRestTemplate().getForObject(url, Map.class);
            return Result.success(response);
        } catch (Exception e) {
            return Result.fail("AI服务暂不可用：" + e.getMessage());
        }
    }

    /**
     * 查询已缓存的最新一份综合报告，不触发生成。
     * 页面/面板进入时先调这个：有缓存直接展示；没有再引导用户点"生成"。
     */
    private void enrichReportWithReviewInfo(Map<String, Object> reportMap, AiComprehensiveReport record) {
        reportMap.put("report_id", record.getId());
        reportMap.put("review_status", record.getReviewStatus() == null ? "pending" : record.getReviewStatus());
        reportMap.put("is_edited", Boolean.TRUE.equals(record.getIsEdited()));
        reportMap.put("confirmed_by", record.getConfirmedBy());
        reportMap.put("confirmed_at", record.getConfirmedAt() == null ? null : record.getConfirmedAt().format(FMT));
        reportMap.put("last_edited_by", record.getLastEditedBy());
        reportMap.put("last_edited_at", record.getLastEditedAt() == null ? null : record.getLastEditedAt().format(FMT));
    }

    @GetMapping("/latest/{patientId}")
    public Result<?> getLatest(@PathVariable Long patientId) {
        AiComprehensiveReport latest = aiComprehensiveReportMapper.selectOne(
                new LambdaQueryWrapper<AiComprehensiveReport>()
                        .eq(AiComprehensiveReport::getPatientId, patientId)
                        .orderByDesc(AiComprehensiveReport::getCreateTime)
                        .last("LIMIT 1"));
        if (latest == null) {
            return Result.success(null);
        }
        try {
            Map<String, Object> reportMap = objectMapper.readValue(latest.getReportJson(), Map.class);
            enrichReportWithReviewInfo(reportMap, latest); // 新增这一行

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("report", reportMap);
            data.put("chain_steps", objectMapper.readValue(latest.getChainStepsJson(), Object.class));
            data.put("data_summary", objectMapper.readValue(latest.getDataSummaryJson(), Object.class));
            data.put("patient_name", latest.getPatientName());
            data.put("generated_at", latest.getCreateTime().format(FMT));
            return Result.success(data);
        } catch (Exception e) {
            log.warn("缓存报告解析失败 patientId={}", patientId, e);
            return Result.success(null);
        }
    }

    /**
     * 生成多模态综合诊疗报告
     * 调用 Python LangChain 4步链式推理，成功后落一条新缓存记录
     */

    @PostMapping("/generate/{patientId}")
    public Result<?> generateReport(@PathVariable Long patientId) {
        String threadId = "patient_" + patientId + "_" + UUID.randomUUID().toString().substring(0, 8);
        Long reportId = insertPendingRecord(patientId, threadId);

        try {
            String url = PYTHON_BASE_URL + "/ai/generate-report/" + patientId + "?thread_id=" + threadId;
            Map<String, Object> response = getRestTemplate().postForObject(url, null, Map.class);

            if (response != null && Integer.valueOf(200).equals(response.get("code"))) {
                updateReportRecord(reportId, response, "completed");
                response.put("generated_at", LocalDateTime.now().format(FMT));

                // 新增：新生成的报告默认待审核、未编辑
                Object reportObj = response.get("report");
                if (reportObj instanceof Map) {
                    Map<String, Object> reportMap = (Map<String, Object>) reportObj;
                    reportMap.put("report_id", reportId);
                    reportMap.put("review_status", "pending");
                    reportMap.put("is_edited", false);
                }
                return Result.success(response);
            } else {
                updateReportRecord(reportId, response, "failed");
                return Result.fail("报告生成失败");
            }
        } catch (Exception e) {
            log.warn("调用AI报告生成接口异常 patientId={} threadId={}", patientId, threadId, e);
            return Result.fail("AI服务暂不可用，可稍后重试续跑：" + e.getMessage());
        }
    }

    @PostMapping("/resume/{patientId}")
    public Result<?> resumeReport(@PathVariable Long patientId) {
        AiComprehensiveReport pending = aiComprehensiveReportMapper.selectOne(
                new LambdaQueryWrapper<AiComprehensiveReport>()
                        .eq(AiComprehensiveReport::getPatientId, patientId)
                        .eq(AiComprehensiveReport::getGenStatus, "generating")
                        .orderByDesc(AiComprehensiveReport::getCreateTime)
                        .last("LIMIT 1"));
        if (pending == null) {
            return Result.fail("没有找到未完成的生成任务");
        }
        try {
            String url = PYTHON_BASE_URL + "/ai/generate-report/" + patientId
                    + "/resume?thread_id=" + pending.getLanggraphThreadId();
            Map<String, Object> response = getRestTemplate().postForObject(url, null, Map.class);
            String status = (response != null && Integer.valueOf(200).equals(response.get("code")))
                    ? "completed" : "failed";
            updateReportRecord(pending.getId(), response, status);
            return "completed".equals(status) ? Result.success(response) : Result.fail("续跑失败");
        } catch (Exception e) {
            log.warn("续跑AI报告异常 patientId={} threadId={}", patientId, pending.getLanggraphThreadId(), e);
            return Result.fail("AI服务暂不可用：" + e.getMessage());
        }
    }

    private Long insertPendingRecord(Long patientId, String threadId) {
        AiComprehensiveReport record = new AiComprehensiveReport();
        record.setPatientId(patientId);
        record.setLanggraphThreadId(threadId);
        record.setGenStatus("generating");
        record.setCreateTime(LocalDateTime.now());
        aiComprehensiveReportMapper.insert(record);
        return record.getId();
    }

    private void updateReportRecord(Long reportId, Map<String, Object> response, String status) {
        try {
            AiComprehensiveReport record = new AiComprehensiveReport();
            record.setId(reportId);
            record.setGenStatus(status);
            if (response != null) {
                record.setPatientName((String) response.get("patient_name"));
                if (response.get("report") != null)
                    record.setReportJson(objectMapper.writeValueAsString(response.get("report")));
                if (response.get("chain_steps") != null)
                    record.setChainStepsJson(objectMapper.writeValueAsString(response.get("chain_steps")));
                if (response.get("data_summary") != null)
                    record.setDataSummaryJson(objectMapper.writeValueAsString(response.get("data_summary")));
            }
            aiComprehensiveReportMapper.updateById(record);
        } catch (Exception e) {
            log.warn("综合报告更新失败 reportId={}", reportId, e);
        }
    }
    private void saveReportCache(Long patientId, Map<String, Object> response) {
        try {
            AiComprehensiveReport record = new AiComprehensiveReport();
            record.setPatientId(patientId);
            record.setPatientName((String) response.get("patient_name"));
            record.setReportJson(objectMapper.writeValueAsString(response.get("report")));
            record.setChainStepsJson(objectMapper.writeValueAsString(response.get("chain_steps")));
            record.setDataSummaryJson(objectMapper.writeValueAsString(response.get("data_summary")));
            record.setCreateTime(LocalDateTime.now());
            aiComprehensiveReportMapper.insert(record);
        } catch (Exception e) {
            // 落库失败不影响本次报告正常展示给用户，只是下次刷新拿不到缓存，记日志即可
            log.warn("综合报告落库失败 patientId={}", patientId, e);
        }
    }

    /**
     * 医生编辑报告内容（sections / risk_level）
     * 编辑后自动重置为待审核状态
     */
    @PutMapping("/update/{patientId}")
    public Result<?> updateReport(@PathVariable Long patientId, @RequestBody Map<String, Object> body) {
        Object reportIdObj = body.get("report_id");
        if (reportIdObj == null) {
            return Result.fail("缺少 report_id");
        }
        Long reportId = Long.valueOf(reportIdObj.toString());

        AiComprehensiveReport record = aiComprehensiveReportMapper.selectById(reportId);
        if (record == null || !patientId.equals(record.getPatientId())) {
            return Result.fail("报告不存在");
        }

        try {
            // 把现有 report_json 解析出来，只替换 sections 和 risk_level，其余字段（title/summary等）保持不变
            Map<String, Object> reportMap = objectMapper.readValue(record.getReportJson(), Map.class);
            reportMap.put("sections", body.get("sections"));
            if (body.get("risk_level") != null) {
                reportMap.put("risk_level", body.get("risk_level"));
            }

            String currentUser = getCurrentUsername(); // TODO: 换成你们实际的登录用户获取方式
            LocalDateTime now = LocalDateTime.now();

            AiComprehensiveReport update = new AiComprehensiveReport();
            update.setId(reportId);
            update.setReportJson(objectMapper.writeValueAsString(reportMap));
            update.setIsEdited(true);
            update.setReviewStatus("pending"); // 内容变了，需要重新确认
            update.setLastEditedBy(currentUser);
            update.setLastEditedAt(now);
            aiComprehensiveReportMapper.updateById(update);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("last_edited_by", currentUser);
            data.put("last_edited_at", now.format(FMT));
            return Result.success(data);
        } catch (Exception e) {
            log.warn("报告更新失败 patientId={} reportId={}", patientId, reportId, e);
            return Result.fail("保存失败：" + e.getMessage());
        }
    }

    /** 医生确认报告无误 */
    @PostMapping("/confirm/{patientId}")
    public Result<?> confirmReport(@PathVariable Long patientId, @RequestBody Map<String, Object> body) {
        Object reportIdObj = body.get("report_id");
        if (reportIdObj == null) {
            return Result.fail("缺少 report_id");
        }
        Long reportId = Long.valueOf(reportIdObj.toString());

        AiComprehensiveReport record = aiComprehensiveReportMapper.selectById(reportId);
        if (record == null || !patientId.equals(record.getPatientId())) {
            return Result.fail("报告不存在");
        }

        String currentUser = getCurrentUsername(); // TODO: 换成你们实际的登录用户获取方式
        LocalDateTime now = LocalDateTime.now();

        AiComprehensiveReport update = new AiComprehensiveReport();
        update.setId(reportId);
        update.setReviewStatus("confirmed");
        update.setConfirmedBy(currentUser);
        update.setConfirmedAt(now);
        aiComprehensiveReportMapper.updateById(update);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("confirmed_by", currentUser);
        data.put("confirmed_at", now.format(FMT));
        return Result.success(data);
    }

    /** 占位：获取当前登录用户名，接入你们真实的鉴权体系后替换 */
    private String getCurrentUsername() {
        return "张医生";
    }


}