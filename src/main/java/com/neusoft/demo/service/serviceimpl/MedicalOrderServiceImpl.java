package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.dto.MedicalOrderDTO;
import com.neusoft.demo.entity.MedicalOrder;
import com.neusoft.demo.entity.Prescription;
import com.neusoft.demo.mapper.MedicalOrderMapper;
import com.neusoft.demo.mapper.PrescriptionMapper;
import com.neusoft.demo.service.MedicalOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MedicalOrderServiceImpl implements MedicalOrderService {

    @Autowired
    private MedicalOrderMapper medicalOrderMapper;

    @Autowired
    private PrescriptionMapper prescriptionMapper;

    @Override
    @Transactional
    public Long create(Long doctorId, MedicalOrderDTO dto) {
        MedicalOrder order = new MedicalOrder();
        order.setRegisterOrderId(dto.getRegisterOrderId());
        order.setPatientId(dto.getPatientId());
        order.setDoctorId(doctorId);
        order.setOrderType(dto.getOrderType());
        order.setExecStatus(0);
        order.setCreateTime(LocalDateTime.now());
        medicalOrderMapper.insert(order);

        // 用药医嘱时同步写处方
        if (dto.getOrderType() == 3 && !CollectionUtils.isEmpty(dto.getPrescriptions())) {
            dto.getPrescriptions().forEach(item -> {
                Prescription p = new Prescription();
                p.setOrderId(order.getId());
                p.setDrugCode(item.getDrugCode());
                p.setDrugName(item.getDrugName());
                p.setDosage(item.getDosage());
                p.setDrugUsage(item.getDrugUsage());
                p.setPrescStatus(0);
                prescriptionMapper.insert(p);
            });
        }
        return order.getId();
    }

    @Override
    public List<MedicalOrder> listByRegisterOrder(Long registerOrderId) {
        return medicalOrderMapper.selectList(
                new LambdaQueryWrapper<MedicalOrder>()
                        .eq(MedicalOrder::getRegisterOrderId, registerOrderId)
                        .orderByAsc(MedicalOrder::getCreateTime)
        );
    }
}
