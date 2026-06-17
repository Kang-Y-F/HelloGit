package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.neusoft.demo.dto.AiConfirmDTO;
import com.neusoft.demo.dto.MedicalRecordDTO;
import com.neusoft.demo.entity.MedicalRecord;
import com.neusoft.demo.mapper.MedicalRecordMapper;
import com.neusoft.demo.service.MedicalRecordService;
import com.neusoft.demo.vo.MedicalRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    @Autowired
    private MedicalRecordMapper medicalRecordMapper;

    @Autowired
    private ChatClient chatClient;

    @Override
    @Transactional
    public Long create(Long doctorId, MedicalRecordDTO dto) {
        MedicalRecord record = new MedicalRecord();
        record.setUserId(dto.getUserId());
        record.setDoctorId(doctorId);
        record.setChiefComplaint(dto.getChiefComplaint());
        record.setPresentHistory(dto.getPresentHistory());
        record.setCheckResult(dto.getCheckResult());
        record.setDiagnosis(dto.getDiagnosis());
        record.setAiConfirmStatus(0);
        record.setCreateTime(LocalDateTime.now());
        medicalRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public MedicalRecordVO getDetail(Long id) {
        MedicalRecordVO vo = medicalRecordMapper.selectDetailById(id);
        if (vo == null) throw new RuntimeException("病历不存在");
        return vo;
    }

    @Override
    public List<MedicalRecordVO> listByPatient(Long patientId) {
        return medicalRecordMapper.selectByPatientId(patientId);
    }

    @Override
    public List<MedicalRecordVO> listByDoctor(Long doctorId, String keyword) {
        return medicalRecordMapper.selectByDoctorId(doctorId, keyword);
    }

    @Override
    @Transactional
    public MedicalRecordVO generateAiAdvice(Long recordId) {
        MedicalRecordVO vo = getDetail(recordId);

        String prompt = String.format("""
                你是一名专业的脑科AI助理医生。请根据以下患者信息，给出结构化的诊疗建议。
                
                患者主诉：%s
                现病史：%s
                初步检查结果：%s
                
                请严格按照以下格式输出，不要添加其他内容：
                【诊断建议】
                （填写初步诊断）
                【检查建议】
                （填写建议检查项目，多项用顿号分隔）
                【用药建议】
                （填写建议用药，多项用顿号分隔，暂无则填"暂无"）
                """,
                vo.getChiefComplaint(),
                vo.getPresentHistory(),
                vo.getCheckResult() == null ? "暂无" : vo.getCheckResult()
        );

        String aiResponse;
        try {
            aiResponse = chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.error("AI建议生成失败 recordId={}", recordId, e);
            throw new RuntimeException("AI服务暂时不可用，请稍后重试");
        }

        String aiDiagnosis   = extractSection(aiResponse, "【诊断建议】", "【检查建议】");
        String aiCheckAdvice = extractSection(aiResponse, "【检查建议】", "【用药建议】");
        String aiDrugAdvice  = extractSection(aiResponse, "【用药建议】", null);

        medicalRecordMapper.update(null,
                new LambdaUpdateWrapper<MedicalRecord>()
                        .eq(MedicalRecord::getId, recordId)
                        .set(MedicalRecord::getAiDiagnosis,   aiDiagnosis)
                        .set(MedicalRecord::getAiCheckAdvice, aiCheckAdvice)
                        .set(MedicalRecord::getAiDrugAdvice,  aiDrugAdvice)
                        .set(MedicalRecord::getAiConfirmStatus, 0)
        );

        vo.setAiDiagnosis(aiDiagnosis);
        vo.setAiCheckAdvice(aiCheckAdvice);
        vo.setAiDrugAdvice(aiDrugAdvice);
        vo.setAiConfirmStatus(0);
        return vo;
    }

    @Override
    @Transactional
    public boolean confirmAi(Long recordId, AiConfirmDTO dto) {
        LambdaUpdateWrapper<MedicalRecord> wrapper =
                new LambdaUpdateWrapper<MedicalRecord>()
                        .eq(MedicalRecord::getId, recordId)
                        .set(MedicalRecord::getAiConfirmStatus, dto.getConfirmStatus());

        if (dto.getDiagnosis()     != null) wrapper.set(MedicalRecord::getDiagnosis,    dto.getDiagnosis());
        if (dto.getAiCheckAdvice() != null) wrapper.set(MedicalRecord::getAiCheckAdvice, dto.getAiCheckAdvice());
        if (dto.getAiDrugAdvice()  != null) wrapper.set(MedicalRecord::getAiDrugAdvice,  dto.getAiDrugAdvice());

        return medicalRecordMapper.update(null, wrapper) > 0;
    }

    private String extractSection(String text, String startTag, String endTag) {
        int start = text.indexOf(startTag);
        if (start == -1) return "";
        start += startTag.length();
        int end = endTag != null ? text.indexOf(endTag, start) : text.length();
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }
}
