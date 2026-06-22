package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.neusoft.demo.dto.AiConfirmDTO;
import com.neusoft.demo.dto.MedicalRecordDTO;
import com.neusoft.demo.entity.MedicalRecord;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.mapper.MedicalRecordMapper;
import com.neusoft.demo.service.MedicalRecordService;
import com.neusoft.demo.vo.MedicalRecordVO;
import com.neusoft.demo.vo.PatientMedicalRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    @Autowired
    private MedicalRecordMapper medicalRecordMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private ChatClient chatClient;

    // 日期格式化
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

    // ========== 新增：患者端查询本人病历 ==========
    @Override
    public List<PatientMedicalRecordVO> listPatientMedicalRecord(Long patientId) {
        // 复用组员已写好的Mapper方法
        List<MedicalRecordVO> originList = medicalRecordMapper.selectByPatientId(patientId);
        List<PatientMedicalRecordVO> resultList = new ArrayList<>();

        for (MedicalRecordVO origin : originList) {
            PatientMedicalRecordVO vo = new PatientMedicalRecordVO();
            // 基础字段赋值
            vo
                    .setId(origin.getId());
            // 就诊日期格式化
            if (origin.getCreateTime() != null) {
                vo
                        .setVisitDate(origin.getCreateTime().format(DATE_FORMAT));
            }
            vo
                    .setDoctorName(origin.getDoctorName());

            // 补充医生职称：根据doctorId查询职称（也可优化联查，临时方案直接查）
            if (origin.getDoctorId() != null) {
                Doctor doctor = doctorMapper.selectById(origin.getDoctorId());
                if (doctor != null) {
                    vo
                            .setDoctorTitle(doctor.getTitle());
                }
            }

            // 主诉、诊断
            vo
                    .setChiefComplaint(origin.getChiefComplaint());
            // 优先取医生最终诊断，无则使用AI诊断
            vo
                    .setDiagnosis(origin.getAiDiagnosis() == null ? "暂无诊断" : origin.getAiDiagnosis());

            // 处方：拼接用药建议
            vo
                    .setPrescription(origin.getAiDrugAdvice() == null ? "暂无处方" : origin.getAiDrugAdvice());

            // 检查项目：拆分检查建议为数组
            List<String> checkList = new ArrayList<>();
            if (origin.getAiCheckAdvice() != null && !origin.getAiCheckAdvice().isEmpty()) {
                // 按顿号分割
                String[] checks = origin.getAiCheckAdvice().split("、");
                for (String item : checks) {
                    checkList
                            .add(item.trim());
                }
            }
            vo
                    .setCheckItems(checkList);

            // 病历状态：统一为 completed已完成（当前业务病历均为已完成）
            vo
                    .setStatus("completed");

            resultList
                    .add(vo);
        }
        return resultList;
    }
}
