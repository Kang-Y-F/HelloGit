package com.neusoft.demo.service;

import com.neusoft.demo.dto.AiConfirmDTO;
import com.neusoft.demo.dto.MedicalRecordDTO;
import com.neusoft.demo.vo.MedicalRecordVO;
import com.neusoft.demo.vo.PatientMedicalRecordVO;

import java.util.List;

public interface MedicalRecordService {

    Long create(Long doctorId, MedicalRecordDTO dto);

    MedicalRecordVO getDetail(Long id);

    List<MedicalRecordVO> listByPatient(Long patientId);

    /** 查询医生自己接诊的所有病历，支持关键词搜索 */
    List<MedicalRecordVO> listByDoctor(Long doctorId, String keyword);

    MedicalRecordVO generateAiAdvice(Long recordId);

    boolean confirmAi(Long recordId, AiConfirmDTO dto);

    /** 根据患者ID查询本人所有病历（患者端） */
    List<PatientMedicalRecordVO> listPatientMedicalRecord(Long patientId);
}
