package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.demo.dto.CtAiConfirmDTO;
import com.neusoft.demo.entity.*;
import com.neusoft.demo.mapper.*;
import com.neusoft.demo.service.CtAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CtAnalysisServiceImpl implements CtAnalysisService {

    @Autowired private CheckReportMapper    checkReportMapper;
    @Autowired private PmiPatientMapper     pmiPatientMapper;
    @Autowired private MedicalRecordMapper  medicalRecordMapper;
    @Autowired private LabReportMapper      labReportMapper;
    @Autowired private AiOperationLogMapper aiLogMapper;
    @Autowired private ChatClient           chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public CheckReport generateAiAnalysis(Long reportId) {
        CheckReport report = checkReportMapper.selectById(reportId);
        if (report == null) throw new RuntimeException("影像报告不存在");

        // 1. 收集患者基本信息
        PmiPatient patient = pmiPatientMapper.selectById(report.getPatientId());
        String patientInfo = patient != null
                ? String.format("姓名：%s，性别：%s",
                        patient.getName(),
                        patient.getGender() == null ? "未知" : (patient.getGender() == 1 ? "男" : "女"))
                : "患者信息缺失";

        // 2. 收集伪影数据
        String artifactInfo = "无伪影数据";
        long pixelCount = 0;
        if (report.getArtifactResult() != null) {
            try {
                Map<String, Object> obj = objectMapper.readValue(report.getArtifactResult(), Map.class);
                Object px = obj.get("artifact_pixel_count");
                if (px instanceof Number) pixelCount = ((Number) px).longValue();
                String level = riskLevel(pixelCount);
                artifactInfo = String.format("伪影像素数：%d（%s），特征维度：%s",
                        pixelCount, level, obj.getOrDefault("feature_shape", "未知"));
            } catch (Exception e) {
                log.warn("解析 artifact_result 失败", e);
            }
        }

        // 3. 收集历史病历（最近3条）
        List<MedicalRecord> records = medicalRecordMapper.selectList(
                new LambdaQueryWrapper<MedicalRecord>()
                        .eq(MedicalRecord::getUserId, report.getPatientId())
                        .orderByDesc(MedicalRecord::getCreateTime)
                        .last("LIMIT 3")
        );
        StringBuilder recordInfo = new StringBuilder();
        if (records.isEmpty()) {
            recordInfo.append("暂无历史病历");
        } else {
            for (int i = 0; i < records.size(); i++) {
                MedicalRecord r = records.get(i);
                recordInfo.append(String.format("第%d次接诊：主诉=%s；现病史=%s；诊断=%s\n",
                        i + 1,
                        nonNull(r.getChiefComplaint()),
                        nonNull(r.getPresentHistory()),
                        nonNull(r.getDiagnosis())));
            }
        }

        // 4. 收集检验报告
        List<LabReport> labs = labReportMapper.selectList(
                new LambdaQueryWrapper<LabReport>()
                        .eq(LabReport::getPatientId, report.getPatientId())
                        .orderByDesc(LabReport::getCreateTime)
                        .last("LIMIT 5")
        );
        StringBuilder labInfo = new StringBuilder();
        if (labs.isEmpty()) {
            labInfo.append("暂无检验报告");
        } else {
            for (LabReport l : labs) {
                labInfo.append(String.format("%s：%s（参考%s，%s）\n",
                        nonNull(l.getItemName()),
                        nonNull(l.getTestValue()),
                        nonNull(l.getReferenceRange()),
                        l.getAbnormalFlag() != null && l.getAbnormalFlag() == 1 ? "异常" : "正常"));
            }
        }

        // 5. 构造 Prompt 调大模型
        String prompt = String.format("""
                你是一名专业的脑科影像AI辅助诊断助手。请根据以下患者完整资料，给出结构化的影像分析报告。
                
                ━━━ 患者信息 ━━━
                %s
                
                ━━━ CT伪影检测结果 ━━━
                %s
                
                ━━━ 历史病历 ━━━
                %s
                
                ━━━ 检验报告 ━━━
                %s
                
                ━━━ 输出要求 ━━━
                请严格按以下格式输出（共4段，每段以标题开头），语言专业、简洁、有针对性，不要泛泛而谈。
                
                【影像质量评估】
                （基于伪影数据判断本次CT影像质量是否满足诊断需求，2-3句）
                
                【疾病可能性分析】
                （结合病历、检验、伪影数据，推断可能的疾病或异常，按可能性排序，2-4条）
                
                【重点关注区域】
                （指出影像中医生应重点观察的解剖部位或异常征象，3-5个要点）
                
                【建议下一步检查】
                （建议进一步的影像/化验/会诊项目，2-3条）
                """,
                patientInfo, artifactInfo, recordInfo, labInfo
        );

        String aiResponse;
        try {
            aiResponse = chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.error("AI影像分析生成失败 reportId={}", reportId, e);
            throw new RuntimeException("AI服务暂时不可用，请稍后重试");
        }

        // 6. 写库
        checkReportMapper.update(null,
                new LambdaUpdateWrapper<CheckReport>()
                        .eq(CheckReport::getId, reportId)
                        .set(CheckReport::getAiAnalysis,      aiResponse)
                        .set(CheckReport::getAiConfirmStatus, 1)   // 1=已生成待确认
        );

        // 7. 写溯源日志（0=查看/已生成）
        writeLog(reportId, aiResponse, null, 0);

        report.setAiAnalysis(aiResponse);
        report.setAiConfirmStatus(1);
        return report;
    }

    @Override
    @Transactional
    public boolean confirmAiAnalysis(Long reportId, CtAiConfirmDTO dto) {
        CheckReport report = checkReportMapper.selectById(reportId);
        if (report == null) return false;

        Integer status = dto.getConfirmStatus(); // 2确认 3修改 4驳回
        String confirmedText;
        if (status == 2)      confirmedText = report.getAiAnalysis();        // 确认：直接用AI原文
        else if (status == 3) confirmedText = dto.getConfirmedText();        // 修改：用医生填写的
        else                  confirmedText = null;                          // 驳回：清空

        int rows = checkReportMapper.update(null,
                new LambdaUpdateWrapper<CheckReport>()
                        .eq(CheckReport::getId, reportId)
                        .set(CheckReport::getAiConfirmStatus,    status)
                        .set(CheckReport::getDoctorConfirmedText, confirmedText)
        );

        // 写溯源日志
        String aiOriginal   = report.getAiAnalysis();
        String doctorChange = status == 3 ? dto.getConfirmedText() : null;
        writeLog(reportId, aiOriginal, doctorChange, status);

        return rows > 0;
    }

    // ── 工具方法 ─────────────────────────────────────────────

    private String riskLevel(long px) {
        if (px == 0)       return "无伪影";
        if (px < 1000)     return "轻度伪影";
        if (px < 10000)    return "中度伪影";
        return "重度伪影";
    }

    private String nonNull(String s) { return s == null || s.isEmpty() ? "暂无" : s; }

    private void writeLog(Long reportId, String aiOriginal, String doctorModify, Integer operateType) {
        try {
            AiOperationLog log = new AiOperationLog();
            // 复用 ai_operation_log 表，用 record_id 字段记录 CT 报告 ID
            // 通过 operate_type 区分：0/1/2=接诊AI，10/11/12/13=CT AI（加10偏移）
            log.setRecordId(reportId);
            log.setAiOriginal(aiOriginal);
            log.setDoctorModify(doctorModify);
            log.setOperateType(operateType + 10);   // CT AI 加10偏移
            log.setOperateTime(LocalDateTime.now());
            aiLogMapper.insert(log);
        } catch (Exception e) {
            log.error("写AI溯源日志失败", e);
        }
    }
}
