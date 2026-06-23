package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.dto.GuideAnalyzeDTO;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.entity.PatientMessage;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.service.AiGuideService;
import com.neusoft.demo.service.PatientMessageService;
import com.neusoft.demo.vo.GuideAnalyzeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AiGuideServiceImpl implements AiGuideService {

    // 复用项目已注入的AI客户端（和医生端共用）
    @Autowired
    private ChatClient chatClient;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private PatientMessageService messageService;

    // 急症关键词（与前端对齐）
    private static final List<String> EMERGENCY_KEYWORDS =
            Arrays.asList("突发剧烈头痛", "突然晕倒", "口角歪斜", "一侧偏瘫");

    @Override
    public GuideAnalyzeVO analyzeSymptoms(GuideAnalyzeDTO dto, Long patientId) {
        GuideAnalyzeVO result = new GuideAnalyzeVO();
        List<String> symptoms = dto.getSymptoms();
        String duration = dto.getDuration();

        // ========== 第一步：规则判断急症（兜底，优先执行） ==========
        boolean isEmergency = false;
        String allSymptom = String.join("，", symptoms);
        for (String keyword : EMERGENCY_KEYWORDS) {
            if (allSymptom.contains(keyword)) {
                isEmergency = true;
                break;
            }
        }
        result.setUrgency(isEmergency ? "emergency" : "normal");

        // ========== 第二步：组装Prompt 调用通义千问 ==========
        String prompt = String.format("""
                你是脑科医院智能导诊AI，请根据患者症状和持续时间，给出专业导诊结果。
                患者症状：%s
                症状持续时间：%s
                
                输出要求：
                1. 第一行【建议科室】：只输出科室名称，例如：神经内科、神经外科
                2. 第二部分【病情分析】：简单通俗的病情说明，控制在100字以内
                """, allSymptom, duration);

        String aiResp;
        try {
            aiResp = chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.error("智能导诊AI调用异常", e);
            result.setSuggestedDept("神经内科");
            result.setAnalysis("AI服务异常，为您默认推荐神经内科，请前往线下就诊。");
            result.setRecommendedDoctors(getDoctorByDept(1L));
            return result;
        }

        // ========== 第三步：解析AI返回内容 ==========
        String suggestedDept = "";
        String analysis = "";
        if (aiResp.contains("【建议科室】") && aiResp.contains("【病情分析】")) {
            String[] parts = aiResp.split("【");
            for (String part : parts) {
                if (part.startsWith("建议科室】")) {
                    suggestedDept = part.replace("建议科室】", "").trim();
                } else if (part.startsWith("病情分析】")) {
                    analysis = part.replace("病情分析】", "").trim();
                }
            }
        } else {
            suggestedDept = "神经内科";
            analysis = aiResp;
        }

        result.setSuggestedDept(suggestedDept);
        result.setAnalysis(analysis);

        // ========== 第四步：根据科室匹配在岗医生 ==========
        // 这里做简单映射：科室名称 -> deptId，根据你实际数据库调整
        Long deptId = switch (suggestedDept) {
            case "神经外科" -> 1L;
            case "神经内科" -> 2L;
            case "小儿神经科" -> 3L;
            default -> 1L;
        };
        List<Doctor> doctorList = getDoctorByDept(deptId);
        // 清空密码脱敏
        doctorList.forEach(doc -> doc.setPassword(null));
        result.setRecommendedDoctors(doctorList);

        // ========== 新增：生成AI导诊站内消息 ==========
        PatientMessage msg = new PatientMessage();
        msg.setPatientId(patientId);
        msg.setTitle("AI智能导诊分析完成");
        msg.setContent("你提交的症状已完成AI分析，建议就诊科室：" + result.getSuggestedDept());
        msg.setMsgType(3);
        msg.setJumpPath("pages/guide/guide");
        messageService.addMessage(msg);

        return result;
    }

    /**
     * 根据科室ID查询在岗医生（status=1）
     */
    private List<Doctor> getDoctorByDept(Long deptId) {
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper
                .eq(Doctor::getDeptId, deptId)
                .eq(Doctor::getRole,"doctor")
                .eq(Doctor::getStatus, 1);
        // 直接查询并返回，无需额外 stream 转换
        return doctorMapper.selectList(wrapper);
    }
}