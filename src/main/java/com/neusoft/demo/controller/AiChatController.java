package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.AiChatMessage;
import com.neusoft.demo.entity.MedicalRecord;
import com.neusoft.demo.entity.PmiPatient;
import com.neusoft.demo.mapper.AiChatMessageMapper;
import com.neusoft.demo.mapper.MedicalRecordMapper;
import com.neusoft.demo.mapper.PmiPatientMapper;
import com.neusoft.demo.service.McpToolService;
import com.neusoft.demo.service.MemoryReflectionService;
import com.neusoft.demo.utils.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/ai-chat")
public class AiChatController {

    @Autowired private AiChatMessageMapper     chatMapper;
    @Autowired private PmiPatientMapper        pmiPatientMapper;
    @Autowired private MedicalRecordMapper     medicalRecordMapper;
    @Autowired private ChatClient              chatClient;
    @Autowired private ChatMemory              chatMemory;
    @Autowired private MemoryReflectionService reflectionService;

    @Autowired(required = false)
    private McpToolService mcpToolService;

    @GetMapping("/history/{patientId}")
    public Result<?> history(@PathVariable Long patientId, HttpServletRequest request) {
        Long doctorId = getDoctorId(request);
        List<AiChatMessage> list = chatMapper.selectByDoctorAndPatient(doctorId, patientId);
        return Result.success(list);
    }

    @PostMapping("/send/{patientId}")
    public Result<?> send(@PathVariable Long patientId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        return doSend(patientId, body, request, false);
    }

    @PostMapping("/assist-diagnosis/{patientId}")
    public Result<?> assistDiagnosis(@PathVariable Long patientId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        return doSend(patientId, body, request, true);
    }

    @DeleteMapping("/history/{patientId}")
    public Result<?> clearHistory(@PathVariable Long patientId, HttpServletRequest request) {
        Long doctorId = getDoctorId(request);
        int rows = chatMapper.delete(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getDoctorId, doctorId)
                        .eq(AiChatMessage::getPatientId, patientId)
        );
        // ChatMemory 是独立存储（spring_ai_chat_memory 表），要一并清掉，否则模型仍记得旧对话
        chatMemory.clear(doctorId + "_" + patientId);
        return Result.success("已清空 " + rows + " 条对话");
    }

    /** 反馈打分：医生对某条AI回复评价 +1/-1 */
    @PostMapping("/feedback/{patientId}/{messageId}")
    public Result<?> feedback(@PathVariable Long patientId, @PathVariable Long messageId,
                              @RequestParam int reward, HttpServletRequest request) {
        Long doctorId = getDoctorId(request);
        reflectionService.recordFeedback(messageId, doctorId, patientId, reward);
        return Result.success("反馈已记录");
    }

    private Result<?> doSend(Long patientId, Map<String, String> body, HttpServletRequest request, boolean useMcp) {
        Long doctorId = getDoctorId(request);
        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank()) return Result.fail("消息内容不能为空");

        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setDoctorId(doctorId);
        userMsg.setPatientId(patientId);
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        userMsg.setCreateTime(LocalDateTime.now());
        chatMapper.insert(userMsg);

        String aiResponse;
        if (useMcp && mcpToolService != null) {
            try {
                aiResponse = mcpToolService.assistDiagnosisWithMcp(patientId, userMessage);
            } catch (Exception e) {
                log.warn("MCP辅助诊断失败，降级为普通对话模式", e);
                aiResponse = doPlainChat(doctorId, patientId, userMessage);
            }
        } else {
            aiResponse = doPlainChat(doctorId, patientId, userMessage);
        }

        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setDoctorId(doctorId);
        aiMsg.setPatientId(patientId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(aiResponse);
        aiMsg.setCreateTime(LocalDateTime.now());
        chatMapper.insert(aiMsg); // aiMsg.getId() 插入后自动回填，前端用它做反馈的 messageId

        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("userMessage", userMsg);
        ret.put("aiMessage",   aiMsg);
        return Result.success(ret);
    }

    /** 普通对话：不再手动拼历史，交给 ChatMemory + Advisor 自动管理 */
    private String doPlainChat(Long doctorId, Long patientId, String userMessage) {
        String conversationId = doctorId + "_" + patientId;
        String systemPrompt = buildSystemPrompt(patientId);
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .advisors(a -> a.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI对话调用异常", e);
            return "AI服务异常：" + e.getMessage();
        }
    }

    private Long getDoctorId(HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        return claims.get("userId", Long.class);
    }

    private String buildSystemPrompt(Long patientId) {
        PmiPatient patient = pmiPatientMapper.selectById(patientId);
        List<MedicalRecord> records = medicalRecordMapper.selectList(
                new LambdaQueryWrapper<MedicalRecord>()
                        .eq(MedicalRecord::getUserId, patientId)
                        .orderByDesc(MedicalRecord::getCreateTime)
                        .last("LIMIT 2")
        );

        StringBuilder sb = new StringBuilder();
        sb.append("你是一名专业的脑科AI辅助诊断助手。请用专业、简洁的语言回答医生的问诊问题，必要时给出鉴别诊断建议、检查建议和治疗方向。\n\n");
        sb.append("【当前患者背景】\n");
        if (patient != null) {
            sb.append("姓名：").append(patient.getName())
                    .append("，性别：").append(patient.getGender() == null ? "未知" : (patient.getGender() == 1 ? "男" : "女"))
                    .append("\n");
        }
        if (!records.isEmpty()) {
            sb.append("\n【近期就诊记录】\n");
            for (int i = 0; i < records.size(); i++) {
                MedicalRecord r = records.get(i);
                sb.append(String.format("[%d] 主诉=%s；现病史=%s；诊断=%s\n",
                        i + 1, nv(r.getChiefComplaint()), nv(r.getPresentHistory()), nv(r.getDiagnosis())));
            }
        }

        List<Map<String, Object>> reflections = reflectionService.getTopReflections(patientId, 3);
        if (!reflections.isEmpty()) {
            sb.append("\n【历史被验证有效的诊疗经验（按重要性排序）】\n");
            for (Map<String, Object> r : reflections) {
                sb.append("- ").append(r.get("summary"))
                        .append(String.format("（权重：%.2f）\n", ((Number) r.get("rewardScore")).doubleValue()));
            }
        }

        sb.append("\n请基于以上背景，与医生进行多轮诊疗讨论。每次回答控制在200字以内，要点明确。");
        return sb.toString();
    }

    private String nv(String s) { return s == null || s.isEmpty() ? "暂无" : s; }
}