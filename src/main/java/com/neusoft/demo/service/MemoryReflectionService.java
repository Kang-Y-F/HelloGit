package com.neusoft.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.entity.AiChatFeedback;
import com.neusoft.demo.entity.AiChatMessage;
import com.neusoft.demo.mapper.AiChatFeedbackMapper;
import com.neusoft.demo.mapper.AiChatMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MemoryReflectionService {

    @Autowired private AiChatFeedbackMapper feedbackMapper;
    @Autowired private AiChatMessageMapper  chatMapper;
    @Autowired private ChatClient           chatClient;

    private final RestTemplate rest = new RestTemplate();
    private static final String PY_BASE = "http://localhost:8000";
    private static final int REFLECT_TRIGGER_COUNT = 5; // 每5条好评触发一次反思

    public void recordFeedback(Long messageId, Long doctorId, Long patientId, int reward) {
        AiChatFeedback fb = new AiChatFeedback();
        fb.setMessageId(messageId);
        fb.setDoctorId(doctorId);
        fb.setPatientId(patientId);
        fb.setReward(reward);
        fb.setCreateTime(LocalDateTime.now());
        feedbackMapper.insert(fb);

        int total = feedbackMapper.countByPatient(patientId);
        if (reward > 0 && total % REFLECT_TRIGGER_COUNT == 0) {
            try {
                reflect(patientId);
            } catch (Exception e) {
                log.warn("记忆反思失败，跳过本次", e);
            }
        }
    }

    private void reflect(Long patientId) {
        List<AiChatFeedback> positives = feedbackMapper.selectList(
                new LambdaQueryWrapper<AiChatFeedback>()
                        .eq(AiChatFeedback::getPatientId, patientId)
                        .eq(AiChatFeedback::getReward, 1)
                        .orderByDesc(AiChatFeedback::getCreateTime)
                        .last("LIMIT 5")
        );
        if (positives.isEmpty()) return;

        StringBuilder dialogs = new StringBuilder();
        for (AiChatFeedback fb : positives) {
            AiChatMessage aiMsg = chatMapper.selectById(fb.getMessageId());
            if (aiMsg != null) dialogs.append("【被医生认可的回答】").append(aiMsg.getContent()).append("\n");
        }
        if (dialogs.isEmpty()) return;

        String summary = summarize(dialogs.toString());
        if (summary == null || summary.isBlank()) return;

        // 交给 Python(pgvector) 做真正的语义相似度融合/去重
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("patient_id", patientId);
            body.put("summary", summary);
            body.put("reward", 1.0);
            Map<?, ?> resp = rest.postForObject(PY_BASE + "/memory/reflection/upsert", body, Map.class);
            log.info("反思经验已同步至pgvector: {}", resp);
        } catch (Exception e) {
            log.warn("调用Python反思融合接口失败", e);
        }
    }

    private String summarize(String dialogs) {
        String prompt = "以下是若干条被医生认可（好评）的AI辅助诊断回答，请你提炼出一条可复用的诊疗经验或表达模式"
                + "（不超过100字，客观、可操作，不要重复原文）：\n\n" + dialogs;
        try {
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            log.warn("反思摘要调用LLM失败", e);
            return null;
        }
    }

    /** 供 buildSystemPrompt 调用：从Python(pgvector)取该患者权重最高的经验 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTopReflections(Long patientId, int topN) {
        try {
            Map<String, Object> resp = rest.getForObject(
                    PY_BASE + "/memory/reflection/top/" + patientId + "?top_n=" + topN, Map.class);
            if (resp == null) return List.of();
            return (List<Map<String, Object>>) resp.getOrDefault("reflections", List.of());
        } catch (Exception e) {
            log.warn("获取历史经验失败，降级为空", e);
            return List.of();
        }
    }
}