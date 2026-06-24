package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.dto.MedicalOrderDTO;
import com.neusoft.demo.entity.CheckItem;
import com.neusoft.demo.entity.CheckOrder;
import com.neusoft.demo.entity.MedicalOrder;
import com.neusoft.demo.entity.Prescription;
import com.neusoft.demo.mapper.CheckItemMapper;
import com.neusoft.demo.mapper.CheckOrderMapper;
import com.neusoft.demo.mapper.MedicalOrderMapper;
import com.neusoft.demo.mapper.PrescriptionMapper;
import com.neusoft.demo.service.MedicalOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MedicalOrderServiceImpl implements MedicalOrderService {

    @Autowired private MedicalOrderMapper medicalOrderMapper;
    @Autowired private CheckOrderMapper   checkOrderMapper;
    @Autowired private CheckItemMapper    checkItemMapper;
    @Autowired private PrescriptionMapper prescriptionMapper;

    /**
     * 开医嘱
     *
     * 检查(1)/检验(2)：
     *   - 必须传 recordId（病历ID）和 itemId（检查项目ID）
     *   - 写 medical_order + check_order（status=0 待缴费，item_id=选定项目）
     *
     * 用药(3)：
     *   - 写 medical_order + prescription 明细
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long doctorId, MedicalOrderDTO dto) {

        // 1. 写 medical_order
        MedicalOrder order = new MedicalOrder();
        order.setRegisterOrderId(dto.getRegisterOrderId());
        order.setPatientId(dto.getPatientId());
        order.setDoctorId(doctorId);
        order.setOrderType(dto.getOrderType());
        order.setExecStatus(0);
        order.setCreateTime(LocalDateTime.now());
        medicalOrderMapper.insert(order);

        // 2. 检查/检验 → 校验项目 → 写 check_order
        if (dto.getOrderType() == 1 || dto.getOrderType() == 2) {
            if (dto.getRecordId() == null) {
                throw new IllegalArgumentException("开检查/检验医嘱时必须传入 recordId（病历ID）");
            }
            if (dto.getItemId() == null) {
                throw new IllegalArgumentException("请选择具体的检查/检验项目");
            }

            // 校验项目存在
            CheckItem item = checkItemMapper.selectById(dto.getItemId());
            if (item == null) {
                throw new IllegalArgumentException("检查项目不存在: " + dto.getItemId());
            }

            CheckOrder checkOrder = new CheckOrder();
            checkOrder.setRecordId(dto.getRecordId());
            checkOrder.setOrderId(order.getId());
            checkOrder.setUserId(dto.getPatientId());
            checkOrder.setDoctorId(doctorId);
            checkOrder.setItemId(dto.getItemId());       // ← 医生选定的具体项目
            checkOrder.setOrderType(dto.getOrderType());
            checkOrder.setStatus(0);                      // 待缴费
            checkOrder.setCreateTime(LocalDateTime.now());
            checkOrderMapper.insert(checkOrder);
        }

        // 3. 用药 → 写处方
        if (dto.getOrderType() == 3 && dto.getPrescriptions() != null) {
            for (MedicalOrderDTO.PrescriptionItemDTO p : dto.getPrescriptions()) {
                Prescription presc = new Prescription();
                presc.setRegisterOrderId(dto.getRegisterOrderId());
                presc.setPatientId(dto.getPatientId());
                presc.setDoctorId(doctorId);
                presc.setMedicalOrderId(order.getId());
                presc.setDrugCode(p.getDrugCode());
                presc.setDrugName(p.getDrugName());
                presc.setDosage(p.getDosage());
                presc.setDrugUsage(p.getDrugUsage());
                presc.setCreateTime(LocalDateTime.now());
                prescriptionMapper.insert(presc);
            }
        }

        return order.getId();
    }

    @Override
    public List<MedicalOrder> listByRegisterOrder(Long registerOrderId) {
        return medicalOrderMapper.selectByRegisterOrderId(registerOrderId);
    }
}
