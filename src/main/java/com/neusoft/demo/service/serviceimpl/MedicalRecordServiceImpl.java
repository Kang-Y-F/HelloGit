package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.neusoft.demo.dto.AiConfirmDTO;
import com.neusoft.demo.dto.MedicalRecordDTO;
import com.neusoft.demo.entity.*;
import com.neusoft.demo.mapper.*;
import com.neusoft.demo.service.MedicalRecordService;
import com.neusoft.demo.vo.MedicalRecordVO;
import com.neusoft.demo.vo.PatientMedicalRecordVO;
import com.neusoft.demo.vo.PatientMedicalRecordDetailVO;
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

    @Autowired private MedicalRecordMapper    medicalRecordMapper;
    @Autowired private AiOperationLogMapper   aiLogMapper;
    @Autowired private ChatClient             chatClient;
    @Autowired private CheckReportMapper checkReportMapper;
    @Autowired private LabReportMapper labReportMapper;
    @Autowired private MedicalOrderMapper medicalOrderMapper;
    @Autowired private CheckOrderMapper checkOrderMapper;
    @Autowired private DoctorMapper doctorMapper;
    @Autowired private PrescriptionMapper prescriptionMapper;
    @Autowired private PmiPatientMapper pmiPatientMapper;
    @Autowired private DepartmentMapper departmentMapper;


    // 日期格式化
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Transactional
    public Long create(Long doctorId, MedicalRecordDTO dto) {
        MedicalRecord record = new MedicalRecord();
        record.setUserId(dto.getUserId());
        record.setRegisterOrderId(dto.getRegisterOrderId());
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
        List<MedicalRecordVO> voList = medicalRecordMapper.selectByPatientId(patientId);
        enrichWithReports(voList);
        return voList;
    }

    @Override
    public List<MedicalRecordVO> listByDoctor(Long doctorId, String keyword) {
        List<MedicalRecordVO> voList = medicalRecordMapper.selectByDoctorId(doctorId, keyword);
        enrichWithReports(voList);
        return voList;
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
                vo.getPresentHistory() == null ? "暂无" : vo.getPresentHistory(),
                vo.getCheckResult()    == null ? "暂无" : vo.getCheckResult()
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
                        .set(MedicalRecord::getAiDiagnosis,    aiDiagnosis)
                        .set(MedicalRecord::getAiCheckAdvice,  aiCheckAdvice)
                        .set(MedicalRecord::getAiDrugAdvice,   aiDrugAdvice)
                        .set(MedicalRecord::getAiConfirmStatus, 0)
        );

        // 写溯源日志：0=查看（AI建议已生成）
        writeLog(recordId, aiResponse, null, 0);

        vo.setAiDiagnosis(aiDiagnosis);
        vo.setAiCheckAdvice(aiCheckAdvice);
        vo.setAiDrugAdvice(aiDrugAdvice);
        vo.setAiConfirmStatus(0);
        return vo;
    }

    @Override
    @Transactional
    public boolean confirmAi(Long recordId, AiConfirmDTO dto) {
        // 查原始AI内容
        MedicalRecordVO old = getDetail(recordId);
        String aiOriginal = String.format("诊断：%s | 检查：%s | 用药：%s",
                old.getAiDiagnosis(), old.getAiCheckAdvice(), old.getAiDrugAdvice());

        LambdaUpdateWrapper<MedicalRecord> wrapper =
                new LambdaUpdateWrapper<MedicalRecord>()
                        .eq(MedicalRecord::getId, recordId)
                        .set(MedicalRecord::getAiConfirmStatus, dto.getConfirmStatus());

        if (dto.getDiagnosis()     != null) wrapper.set(MedicalRecord::getDiagnosis,    dto.getDiagnosis());
        if (dto.getAiCheckAdvice() != null) wrapper.set(MedicalRecord::getAiCheckAdvice, dto.getAiCheckAdvice());
        if (dto.getAiDrugAdvice()  != null) wrapper.set(MedicalRecord::getAiDrugAdvice,  dto.getAiDrugAdvice());

        boolean ok = medicalRecordMapper.update(null, wrapper) > 0;

        if (ok) {
            // 写溯源日志
            String doctorModify = null;
            if (dto.getConfirmStatus() == 2) {
                doctorModify = String.format("诊断：%s | 检查：%s | 用药：%s",
                        dto.getDiagnosis(), dto.getAiCheckAdvice(), dto.getAiDrugAdvice());
            }
            writeLog(recordId, aiOriginal, doctorModify, dto.getConfirmStatus());
        }
        return ok;
    }

    // ── 工具 ──────────────────────────────────────────────────────

    private void writeLog(Long recordId, String aiOriginal, String doctorModify, Integer operateType) {
        AiOperationLog log = new AiOperationLog();
        log.setRecordId(recordId);
        log.setAiOriginal(aiOriginal);
        log.setDoctorModify(doctorModify);
        log.setOperateType(operateType);
        log.setOperateTime(LocalDateTime.now());
        aiLogMapper.insert(log);
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

    // ========== 新增：患者端查询病历完整详情 ==========
    @Override
    public PatientMedicalRecordDetailVO getPatientMedicalRecordDetail(Long recordId) {
        // 1. 查询病历基础信息
        MedicalRecord record = medicalRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("病历不存在");
        }

        PatientMedicalRecordDetailVO vo = new PatientMedicalRecordDetailVO();

        // 2. 基础信息
        vo.setId(record.getId());
        if (record.getCreateTime() != null) {
            vo.setVisitDate(record.getCreateTime().format(DATE_FORMAT));
        }
        vo.setCreateTime(record.getCreateTime());

        // 3. 查询医生信息
        if (record.getDoctorId() != null) {
            Doctor doctor = doctorMapper.selectById(record.getDoctorId());
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
                vo.setDoctorTitle(doctor.getTitle());

                // 查询科室信息
                if (doctor.getDeptId() != null) {
                    Department dept = departmentMapper.selectById(doctor.getDeptId());
                    if (dept != null) {
                        vo.setDepartmentName(dept.getName());
                    }
                }
            }
        }

        // 4. 病历内容
        vo.setChiefComplaint(record.getChiefComplaint());
        vo.setPresentHistory(record.getPresentHistory());
        vo.setDiagnosis(record.getDiagnosis());

        // 5. AI辅助信息
        vo.setAiDiagnosis(record.getAiDiagnosis());
        vo.setAiCheckAdvice(record.getAiCheckAdvice());
        vo.setAiDrugAdvice(record.getAiDrugAdvice());
        vo.setAiConfirmStatus(record.getAiConfirmStatus());

        // 6. 查询检查报告（含图片）
        List<PatientMedicalRecordDetailVO.CheckReportVO> checkReports = new ArrayList<>();
        if (record.getRegisterOrderId() != null) {
            List<MedicalOrder> orders = medicalOrderMapper.selectByRegisterOrderId(record.getRegisterOrderId());
            for (MedicalOrder mo : orders) {
                if (mo.getOrderType() == 1) { // 检查类
                    List<CheckReport> crs = checkReportMapper.selectList(
                            new LambdaQueryWrapper<CheckReport>()
                                    .eq(CheckReport::getOrderId, mo.getId())
                    );
                    for (CheckReport cr : crs) {
                        PatientMedicalRecordDetailVO.CheckReportVO crVO = new PatientMedicalRecordDetailVO.CheckReportVO();
                        crVO.setId(cr.getId());
                        crVO.setImgType(cr.getImgType());
                        crVO.setImageUrl(cr.getImageUrl());
                        crVO.setCtUrl(cr.getCtUrl());
                        crVO.setReportText(cr.getReportText());
                        crVO.setAiAnalysis(cr.getAiAnalysis());
                        crVO.setDoctorConfirmedText(cr.getDoctorConfirmedText());
                        crVO.setAiConfirmStatus(cr.getAiConfirmStatus());
                        crVO.setCreateTime(cr.getCreateTime());
                        checkReports.add(crVO);
                    }
                }
            }
        }
        vo.setCheckReports(checkReports);

        // 7. 查询检验报告
        List<PatientMedicalRecordDetailVO.LabReportVO> labReports = new ArrayList<>();
        if (record.getRegisterOrderId() != null) {
            List<MedicalOrder> orders = medicalOrderMapper.selectByRegisterOrderId(record.getRegisterOrderId());
            for (MedicalOrder mo : orders) {
                if (mo.getOrderType() == 1 || mo.getOrderType() == 2) { // 检查/检验类
                    List<CheckOrder> cos = checkOrderMapper.selectByOrderId(mo.getId());
                    for (CheckOrder co : cos) {
                        if (co.getOrderType() == 2) { // 检验类
                            List<LabReport> lrs = labReportMapper.selectList(
                                    new LambdaQueryWrapper<LabReport>()
                                            .eq(LabReport::getOrderId, co.getId())
                            );
                            for (LabReport lr : lrs) {
                                PatientMedicalRecordDetailVO.LabReportVO lrVO = new PatientMedicalRecordDetailVO.LabReportVO();
                                lrVO.setId(lr.getId());
                                lrVO.setItemName(lr.getItemName());
                                lrVO.setTestValue(lr.getTestValue());
                                lrVO.setReferenceRange(lr.getReferenceRange());
                                lrVO.setAbnormalFlag(lr.getAbnormalFlag());
                                lrVO.setReportContent(lr.getReportContent());
                                lrVO.setAuditStatus(lr.getAuditStatus());
                                lrVO.setCreateTime(lr.getCreateTime());
                                labReports.add(lrVO);
                            }
                        }
                    }
                }
            }
        }
        vo.setLabReports(labReports);

        // 8. 查询处方用药
        List<PatientMedicalRecordDetailVO.PrescriptionVO> prescriptions = new ArrayList<>();
        if (record.getRegisterOrderId() != null) {
            List<Prescription> prList = prescriptionMapper.selectList(
                    new LambdaQueryWrapper<Prescription>()
                            .eq(Prescription::getRegisterOrderId, record.getRegisterOrderId())
            );
            for (Prescription pr : prList) {
                PatientMedicalRecordDetailVO.PrescriptionVO prVO = new PatientMedicalRecordDetailVO.PrescriptionVO();
                prVO.setId(pr.getId());
                prVO.setPrescriptionNo(pr.getPrescriptionNo());
                prVO.setDrugName(pr.getDrugName());
                prVO.setDosage(pr.getDosage());
                prVO.setQuantity(pr.getQuantity());
                prVO.setDays(pr.getDays());
                prVO.setDrugUsage(pr.getDrugUsage());
                prVO.setAuditResult(pr.getAuditResult());
                prVO.setAuditStatus(pr.getAuditStatus());
                prVO.setCreateTime(pr.getCreateTime());
                prescriptions.add(prVO);
            }
        }
        vo.setPrescriptions(prescriptions);

        return vo;
    }

    /**
     * 为病历列表补充本次就诊的检查/检验摘要
     */
    private void enrichWithReports(List<MedicalRecordVO> voList) {
        for (MedicalRecordVO vo : voList) {
            if (vo.getRegisterOrderId() == null) continue;

            Long regId = vo.getRegisterOrderId();

            // 1. 查本次挂号单下的所有医嘱
            List<MedicalOrder> orders = medicalOrderMapper.selectByRegisterOrderId(regId);

            List<MedicalRecordVO.CheckSummary> checks = new ArrayList<>();
            List<MedicalRecordVO.LabSummary> labs = new ArrayList<>();

            for (MedicalOrder mo : orders) {
                if (mo.getOrderType() == 1) {
                    // 检查类 → 查 check_report
                    List<CheckReport> crs = checkReportMapper.selectList(
                            new LambdaQueryWrapper<CheckReport>()
                                    .eq(CheckReport::getOrderId, mo.getId())
                    );
                    for (CheckReport cr : crs) {
                        MedicalRecordVO.CheckSummary cs = new MedicalRecordVO.CheckSummary();
                        cs.setId(cr.getId());
                        cs.setImgType(cr.getImgType());
                        cs.setReportText(cr.getReportText());
                        cs.setAiConfirmStatus(cr.getAiConfirmStatus());
                        cs.setDoctorConfirmedText(cr.getDoctorConfirmedText());
                        cs.setAiAnalysis(cr.getAiAnalysis());
                        cs.setImageUrl(cr.getImageUrl());
                        cs.setCtUrl(cr.getCtUrl());
                        cs.setArtifactResult(cr.getArtifactResult());
                        cs.setPatientId(cr.getPatientId());
                        checks.add(cs);
                    }
                }

                if (mo.getOrderType() == 1 || mo.getOrderType() == 2) {
                    // 检查/检验类 → 先找 check_order，再查 lab_report
                    List<CheckOrder> cos = checkOrderMapper.selectByOrderId(mo.getId());
                    for (CheckOrder co : cos) {
                        if (co.getOrderType() == 2) {
                            // 检验类 → 查 lab_report
                            List<LabReport> lrs = labReportMapper.selectList(
                                    new LambdaQueryWrapper<LabReport>()
                                            .eq(LabReport::getOrderId, co.getId())
                            );
                            for (LabReport lr : lrs) {
                                MedicalRecordVO.LabSummary ls = new MedicalRecordVO.LabSummary();
                                ls.setId(lr.getId());
                                ls.setItemName(lr.getItemName());
                                ls.setTestValue(lr.getTestValue());
                                ls.setReferenceRange(lr.getReferenceRange());
                                ls.setAbnormalFlag(lr.getAbnormalFlag());
                                ls.setAuditStatus(lr.getAuditStatus());
                                labs.add(ls);
                            }
                        }
                    }
                }
            }

            vo.setCheckReports(checks);
            vo.setLabReports(labs);
        }
    }

}
