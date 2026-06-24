package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.AiChatMessage;
import com.neusoft.demo.entity.MedicalRecord;
import com.neusoft.demo.entity.PmiPatient;
import com.neusoft.demo.mapper.AiChatMessageMapper;
import com.neusoft.demo.mapper.MedicalRecordMapper;
import com.neusoft.demo.mapper.PmiPatientMapper;
import com.neusoft.demo.utils.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/ai-chat")
public class AiChatController {

    @Autowired private AiChatMessageMapper chatMapper;
    @Autowired private PmiPatientMapper    pmiPatientMapper;
    @Autowired private MedicalRecordMapper medicalRecordMapper;
    @Autowired private ChatClient          chatClient;

    /** 获取该医生与该患者的对话历史 */
    @GetMapping("/history/{patientId}")
    public Result<?> history(@PathVariable Long patientId, HttpServletRequest request) {
        Long doctorId = getDoctorId(request);
        List<AiChatMessage> list = chatMapper.selectByDoctorAndPatient(doctorId, patientId);
        return Result.success(list);
    }

    /** 发送消息，AI返回回复，自动保存对话历史 */
    @PostMapping("/send/{patientId}")
    public Result<?> send(
            @PathVariable Long patientId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    ) {
        Long doctorId = getDoctorId(request);
        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank()) return Result.fail("消息内容不能为空");

        // 1. 保存用户消息
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setDoctorId(doctorId);
        userMsg.setPatientId(patientId);
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        userMsg.setCreateTime(LocalDateTime.now());
        chatMapper.insert(userMsg);

        // 2. 构造对话上下文（含历史 + 患者背景）
        List<Message> messages = new ArrayList<>();

        // 系统提示词：注入患者背景信息
        String systemPrompt = buildSystemPrompt(patientId);
        messages.add(new SystemMessage(systemPrompt));

        // 历史消息（最近10条）
        List<AiChatMessage> history = chatMapper.selectByDoctorAndPatient(doctorId, patientId);
        int start = Math.max(0, history.size() - 11);  // 留出当前消息
        for (int i = start; i < history.size(); i++) {
            AiChatMessage m = history.get(i);
            if ("user".equals(m.getRole()))      messages.add(new UserMessage(m.getContent()));
            else if ("assistant".equals(m.getRole())) messages.add(new AssistantMessage(m.getContent()));
        }

        // 3. 调大模型
        String aiResponse;
        try {
            aiResponse = chatClient.prompt(new Prompt(messages)).call().content();
        } catch (Exception e) {
            return Result.fail("AI服务异常：" + e.getMessage());
        }

        // 4. 保存AI回复
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setDoctorId(doctorId);
        aiMsg.setPatientId(patientId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(aiResponse);
        aiMsg.setCreateTime(LocalDateTime.now());
        chatMapper.insert(aiMsg);

        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("userMessage", userMsg);
        ret.put("aiMessage",   aiMsg);
        return Result.success(ret);
    }

    /** 清空对话历史 */
    @DeleteMapping("/history/{patientId}")
    public Result<?> clearHistory(@PathVariable Long patientId, HttpServletRequest request) {
        Long doctorId = getDoctorId(request);
        int rows = chatMapper.delete(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getDoctorId, doctorId)
                        .eq(AiChatMessage::getPatientId, patientId)
        );
        return Result.success("已清空 " + rows + " 条对话");
    }

    // ── 工具方法 ─────────────────────────────────────────────

    private Long getDoctorId(HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        return claims.get("userId", Long.class);
    }

    private String buildSystemPrompt(Long patientId) {
        PmiPatient patient = pmiPatientMapper.selectById(patientId);

        // 最近2次病历
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
                        i + 1,
                        nv(r.getChiefComplaint()),
                        nv(r.getPresentHistory()),
                        nv(r.getDiagnosis())));
            }
        }
        sb.append("\n请基于以上背景，与医生进行多轮诊疗讨论。每次回答控制在200字以内，要点明确。");
        return sb.toString();
    }

    private String nv(String s) { return s == null || s.isEmpty() ? "暂无" : s; }
}
