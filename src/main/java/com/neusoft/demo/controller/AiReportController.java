package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AI 多模态综合诊疗报告
 * 调用 Python LangChain 服务生成报告
 */
@RestController
@RequestMapping("/ai-report")
public class AiReportController {

    private static final String PYTHON_BASE_URL = "http://localhost:8000";

    @Autowired(required = false)
    private RestTemplate restTemplate;

    private RestTemplate getRestTemplate() {
        if (restTemplate != null) return restTemplate;
        return new RestTemplate();
    }

    /**
     * 检查患者数据是否就绪
     */
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
     * 生成多模态综合诊疗报告
     * 调用 Python LangChain 4步链式推理
     */
    @PostMapping("/generate/{patientId}")
    public Result<?> generateReport(@PathVariable Long patientId) {
        try {
            String url = PYTHON_BASE_URL + "/ai/generate-report/" + patientId;
            Map<String, Object> response = getRestTemplate().postForObject(url, null, Map.class);

            if (response != null && Integer.valueOf(200).equals(response.get("code"))) {
                return Result.success(response);
            } else {
                return Result.fail("报告生成失败");
            }
        } catch (Exception e) {
            return Result.fail("AI服务暂不可用：" + e.getMessage());
        }
    }
}
