package com.neusoft.demo.service.serviceimpl;
import com.neusoft.demo.mapper.PrescriptionMapper;
import com.neusoft.demo.service.PrescriptionService;
import com.neusoft.demo.vo.PatientPrescriptionVO;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {
    @Autowired
    private PrescriptionMapper prescriptionMapper;

    @Override
    public List<PatientPrescriptionVO> getMyPrescription(Long patientId) {
        return prescriptionMapper.selectPrescriptionByPatientId(patientId);
    }
}