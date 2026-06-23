package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.dto.MedicalOrderDTO;
import com.neusoft.demo.entity.CheckOrder;
import com.neusoft.demo.entity.MedicalOrder;
import com.neusoft.demo.entity.Prescription;
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

    @Autowired
    private MedicalOrderMapper medicalOrderMapper;

    @Autowired
    private CheckOrderMapper checkOrderMapper;

    @Autowired
    private PrescriptionMapper prescriptionMapper;

    /**
     * 开医嘱（含处方），返回新建医嘱ID
     *
     * 逻辑说明：
     *  - orderType=1（检查）或 2（检验）：先写 medical_order，再同步写 check_order（status=0 待缴费）
     *  - orderType=3（用药）：写 medical_order + prescription 明细，不写 check_order
     *
     * check_order.status 定义：
     *   0=待缴费  1=已缴费/待执行  2=已取消  3=执行中  4=已完成（结果已回传）
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
        // exec_status 初始 0=待执行
        order.setExecStatus(0);
        order.setCreateTime(LocalDateTime.now());
        medicalOrderMapper.insert(order);

        // 2. 检查(1) / 检验(2) 医嘱 → 同步写 check_order（status=0 待缴费）
        if (dto.getOrderType() == 1 || dto.getOrderType() == 2) {
            if (dto.getRecordId() == null) {
                throw new IllegalArgumentException("开检查/检验医嘱时必须传入 recordId（病历ID）");
            }

            CheckOrder checkOrder = new CheckOrder();
            checkOrder.setRecordId(dto.getRecordId());           // 关联病历
            checkOrder.setOrderId(order.getId());                 // 关联 medical_order
            checkOrder.setUserId(dto.getPatientId());             // 患者ID
            checkOrder.setDoctorId(doctorId);                     // 开单医生
            checkOrder.setItemId(dto.getItemId());                // 检查项目（可为null，后续补全）
            checkOrder.setOrderType(dto.getOrderType());          // 1检查 2检验
            checkOrder.setStatus(0);                              // 0=待缴费
            checkOrder.setCreateTime(LocalDateTime.now());
            checkOrderMapper.insert(checkOrder);
        }

        // 3. 用药(3) 医嘱 → 写处方明细
        if (dto.getOrderType() == 3 && dto.getPrescriptions() != null) {
            for (MedicalOrderDTO.PrescriptionItemDTO item : dto.getPrescriptions()) {
                Prescription p = new Prescription();
                p.setRegisterOrderId(dto.getRegisterOrderId());
                p.setPatientId(dto.getPatientId());
                p.setDoctorId(doctorId);
                p.setMedicalOrderId(order.getId());
                p.setDrugCode(item.getDrugCode());
                p.setDrugName(item.getDrugName());
                p.setDosage(item.getDosage());
                p.setDrugUsage(item.getDrugUsage());
                p.setCreateTime(LocalDateTime.now());
                prescriptionMapper.insert(p);
            }
        }

        return order.getId();
    }

    /** 查询挂号单下所有医嘱 */
    @Override
    public List<MedicalOrder> listByRegisterOrder(Long registerOrderId) {
        return medicalOrderMapper.selectByRegisterOrderId(registerOrderId);
    }
}
