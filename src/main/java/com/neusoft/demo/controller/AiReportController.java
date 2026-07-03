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

/**
 * AI 多模态综合诊疗报告
 * 调用 Python LangChain 服务生成报告，并落库缓存
 */
@Slf4j
@RestController
@RequestMapping("/ai-report")
public class AiReportController {

    private static final String PYTHON_BASE_URL = "http://localhost:8000";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("report", objectMapper.readValue(latest.getReportJson(), Map.class));
            data.put("chain_steps", objectMapper.readValue(latest.getChainStepsJson(), Object.class));
            data.put("data_summary", objectMapper.readValue(latest.getDataSummaryJson(), Object.class));
            data.put("patient_name", latest.getPatientName());
            data.put("generated_at", latest.getCreateTime().format(FMT));
            return Result.success(data);
        } catch (Exception e) {
            log.warn("缓存报告解析失败 patientId={}", patientId, e);
            return Result.success(null); // 解析失败当作没有缓存，走正常生成流程，不阻断用户
        }
    }

    /**
     * 生成多模态综合诊疗报告
     * 调用 Python LangChain 4步链式推理，成功后落一条新缓存记录
     */
    @PostMapping("/generate/{patientId}")
    public Result<?> generateReport(@PathVariable Long patientId) {
        try {
            String url = PYTHON_BASE_URL + "/ai/generate-report/" + patientId;
            Map<String, Object> response = getRestTemplate().postForObject(url, null, Map.class);

            if (response != null && Integer.valueOf(200).equals(response.get("code"))) {
                String generatedAt = LocalDateTime.now().format(FMT);
                saveReportCache(patientId, response);
                response.put("generated_at", generatedAt);
                return Result.success(response);
            } else {
                return Result.fail("报告生成失败");
            }
        } catch (Exception e) {
            return Result.fail("AI服务暂不可用：" + e.getMessage());
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
}