package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.dto.MedicalOrderDTO;
import com.neusoft.demo.entity.*;
import com.neusoft.demo.mapper.*;
import com.neusoft.demo.service.MedicalOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.neusoft.demo.dto.AiAuditPrescriptionDTO;
import com.neusoft.demo.service.PharmacyService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
public class MedicalOrderServiceImpl implements MedicalOrderService {

    @Autowired private MedicalOrderMapper medicalOrderMapper;
    @Autowired private CheckOrderMapper   checkOrderMapper;
    @Autowired private CheckItemMapper    checkItemMapper;
    @Autowired private PrescriptionMapper prescriptionMapper;
    @Autowired private PharmacyService pharmacyService;
    @Autowired private DrugMapper drugMapper;
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

// 3. 用药 → 写处方 + 自动 AI 审方
        if (dto.getOrderType() == 3 && dto.getPrescriptions() != null) {

            // ── 原有逻辑改造：补全字段，并收集已插入的处方 ──
            List<Prescription> savedPrescriptions = new ArrayList<>();   // ← 新增：收集结果

            for (MedicalOrderDTO.PrescriptionItemDTO p : dto.getPrescriptions()) {
                Prescription presc = new Prescription();
                presc.setRegisterOrderId(dto.getRegisterOrderId());
                presc.setPatientId(dto.getPatientId());
                presc.setDoctorId(doctorId);
                presc.setMedicalOrderId(order.getId());
                presc.setOrderId(order.getId());           // ← 补上
                presc.setDrugCode(p.getDrugCode());
                presc.setDrugId(p.getDrugId());            // ← 补上
                presc.setDrugName(p.getDrugName());
                presc.setDosage(p.getDosage());
                presc.setDrugUsage(p.getDrugUsage());
                presc.setQuantity(p.getQuantity() != null ? p.getQuantity() : 1);
                presc.setDays(p.getDays() != null ? p.getDays() : 1);
// 优先用前端传来的价格，没有则从药品表取
                BigDecimal unitPrice = p.getUnitPrice();
                if (unitPrice == null && p.getDrugId() != null) {
                    Drug drug = drugMapper.selectById(p.getDrugId());
                    if (drug != null && drug.getPrice() != null) {
                        unitPrice = drug.getPrice();
                    }
                }
                presc.setUnitPrice(unitPrice);

// 自动计算总金额
                if (p.getTotalAmount() != null) {
                    presc.setTotalAmount(p.getTotalAmount());
                } else if (unitPrice != null) {
                    int qty = presc.getQuantity() != null ? presc.getQuantity() : 1;
                    presc.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(qty)));
                }
                presc.setPrescStatus(1);      // 已开方
                presc.setPayStatus(0);        // 未付
                presc.setAuditStatus(0);      // 待AI审
                presc.setDispenseStatus(0);   // 未发
                presc.setPrescriptionNo("RX"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + String.format("%03d", (int)(Math.random() * 1000)));
                presc.setCreateTime(LocalDateTime.now());
                prescriptionMapper.insert(presc);
                savedPrescriptions.add(presc);  // ← 新增：收集
            }

            // ── 新增：处方插入完成后，自动触发 AI 审方 ──
            try {
                AiAuditPrescriptionDTO auditDTO = new AiAuditPrescriptionDTO();
                auditDTO.setPatientId(dto.getPatientId());

                List<Map<String, Object>> drugs = savedPrescriptions.stream().map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("drugId",    p.getDrugId());
                    m.put("drugName",  p.getDrugName());
                    m.put("dosage",    p.getDosage());
                    m.put("quantity",  p.getQuantity());
                    m.put("days",      p.getDays());
                    m.put("drugUsage", p.getDrugUsage());
                    return m;
                }).collect(Collectors.toList());
                auditDTO.setDrugs(drugs);

                Map<String, Object> aiResult = pharmacyService.aiAuditPrescription(auditDTO);

                String resultStr = String.valueOf(aiResult.get("result"));
                int auditStatus = "拒绝".equals(resultStr) ? 3 : "警告".equals(resultStr) ? 2 : 1;
                String summary  = String.valueOf(aiResult.get("summary"));

                for (Prescription p : savedPrescriptions) {
                    prescriptionMapper.update(null,
                            new LambdaUpdateWrapper<Prescription>()
                                    .eq(Prescription::getId, p.getId())
                                    .set(Prescription::getAuditStatus, auditStatus)
                                    .set(Prescription::getAuditResult, summary)
                    );
                }
            } catch (Exception e) {
                // AI 失败不影响开方，处方已入库，药师可手动审
                log.warn("AI审方失败，处方正常保存: {}", e.getMessage());
            }
        }

        return order.getId();
    }

    @Override
    public List<MedicalOrder> listByRegisterOrder(Long registerOrderId) {
        return medicalOrderMapper.selectByRegisterOrderId(registerOrderId);
    }
}
