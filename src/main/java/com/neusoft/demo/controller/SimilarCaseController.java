package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.MedicalRecord;
import com.neusoft.demo.entity.PmiPatient;
import com.neusoft.demo.mapper.MedicalRecordMapper;
import com.neusoft.demo.mapper.PmiPatientMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/similar")
public class SimilarCaseController {

    @Autowired private PmiPatientMapper    pmiPatientMapper;
    @Autowired private MedicalRecordMapper medicalRecordMapper;

    private final RestTemplate  rest = new RestTemplate();
    private final ObjectMapper  om   = new ObjectMapper();

    private static final String PY_BASE = "http://localhost:8000";

    /**
     * 检索与指定患者最相似的K个患者
     * 调 Python 微服务做向量相似度计算，再补充患者姓名、最近诊断
     */
    @GetMapping("/cases/{patientId}")
    public Result<?> findSimilar(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "5") int topK
    ) {
        // 1. 调 Python 微服务
        String url = String.format("%s/similar/%s?top_k=%d", PY_BASE, patientId, topK);
        Map<String, Object> pyResp;
        try {
            pyResp = rest.getForObject(url, Map.class);
        } catch (Exception e) {
            return Result.fail("检索失败：" + e.getMessage());
        }
        if (pyResp == null) return Result.fail("Python服务无响应");

        List<Map<String, Object>> results = (List<Map<String, Object>>) pyResp.getOrDefault("results", new ArrayList<>());

        // 2. 补充每个相似患者的姓名和最近诊断
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Map<String, Object> item : results) {
            String simPid = String.valueOf(item.get("patientId"));
            Long pid;
            try { pid = Long.parseLong(simPid); } catch (Exception e) { continue; }

            // 查患者基本信息
            PmiPatient p = pmiPatientMapper.selectById(pid);

            // 查最近一次病历（用于显示诊断/主诉）
            MedicalRecord lastRec = medicalRecordMapper.selectOne(
                    new LambdaQueryWrapper<MedicalRecord>()
                            .eq(MedicalRecord::getUserId, pid)
                            .orderByDesc(MedicalRecord::getCreateTime)
                            .last("LIMIT 1")
            );

            Map<String, Object> enrichedItem = new LinkedHashMap<>();
            enrichedItem.put("patientId",       pid);
            enrichedItem.put("patientName",     p != null ? p.getName()   : "患者" + pid);
            enrichedItem.put("gender",          p != null ? p.getGender() : null);
            enrichedItem.put("similarity",      item.get("similarity"));
            enrichedItem.put("similarityPct",   Math.round(((Number) item.get("similarity")).doubleValue() * 10000) / 100.0);
            enrichedItem.put("chiefComplaint",  lastRec != null ? lastRec.getChiefComplaint() : null);
            enrichedItem.put("diagnosis",       lastRec != null ? lastRec.getDiagnosis()      : null);
            enrichedItem.put("lastVisitDate",   lastRec != null ? lastRec.getCreateTime()     : null);
            enriched.add(enrichedItem);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("target", patientId);
        result.put("count",  enriched.size());
        result.put("cases",  enriched);
        return Result.success(result);
    }
}
