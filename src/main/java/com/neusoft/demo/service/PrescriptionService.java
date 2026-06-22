package com.neusoft.demo.service;
import com.neusoft.demo.vo.PatientPrescriptionVO;
import java.util.List;

public interface PrescriptionService {
    List<PatientPrescriptionVO> getMyPrescription(Long patientId);
}