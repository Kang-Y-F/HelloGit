package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.demo.dto.*;
import com.neusoft.demo.entity.*;
import com.neusoft.demo.mapper.*;
import com.neusoft.demo.service.PharmacyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PharmacyServiceImpl implements PharmacyService {

    @Autowired private DrugMapper              drugMapper;
    @Autowired private DrugInventoryMapper     drugInventoryMapper;
    @Autowired private DrugInoutRecordMapper   drugInoutRecordMapper;
    @Autowired private DispenseRecordMapper    dispenseRecordMapper;
    @Autowired private PrescriptionMapper      prescriptionMapper;
    @Autowired private PrescriptionAuditMapper prescriptionAuditMapper;
    @Autowired private DoctorMapper            doctorMapper;
    @Autowired private PmiPatientMapper        pmiPatientMapper;
    @Autowired private ChatClient              chatClient;

    private final ObjectMapper om = new ObjectMapper();

    // ════════════════════════════════════════════════════════
    //  库存
    // ════════════════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> listAllDrugsWithStock() {
        List<Map<String, Object>> list = drugInventoryMapper.selectAllWithStock();
        // 标记是否低库存
        for (Map<String, Object> item : list) {
            Object stock = item.get("stock_qty");
            Object safety = item.get("safety_qty");
            int s  = stock == null ? 0 : ((Number) stock).intValue();
            int sf = safety == null ? 0 : ((Number) safety).intValue();
            item.put("isLowStock", s <= sf);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> listLowStockDrugs() {
        return listAllDrugsWithStock().stream()
                .filter(d -> Boolean.TRUE.equals(d.get("isLowStock")))
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════
    //  入库
    // ════════════════════════════════════════════════════════

    @Override
    @Transactional
    public DrugInoutRecord drugIn(DrugInDTO dto, Long operatorId) {
        if (dto.getDrugId() == null || dto.getQuantity() == null || dto.getQuantity() <= 0)
            throw new RuntimeException("参数错误");

        Drug drug = drugMapper.selectById(dto.getDrugId());
        if (drug == null) throw new RuntimeException("药品不存在");

        Doctor operator = doctorMapper.selectById(operatorId);
        DrugInventory inv = drugInventoryMapper.selectOne(
                new LambdaQueryWrapper<DrugInventory>().eq(DrugInventory::getDrugId, dto.getDrugId())
        );
        if (inv == null) {
            inv = new DrugInventory();
            inv.setDrugId(dto.getDrugId());
            inv.setStockQty(0);
            inv.setTotalIn(0);
            inv.setTotalOut(0);
            inv.setSafetyQty(50);
            drugInventoryMapper.insert(inv);
        }

        int before = inv.getStockQty();
        int after  = before + dto.getQuantity();

        // 更新库存
        drugInventoryMapper.update(null,
                new LambdaUpdateWrapper<DrugInventory>()
                        .eq(DrugInventory::getDrugId, dto.getDrugId())
                        .set(DrugInventory::getStockQty, after)
                        .set(DrugInventory::getTotalIn, inv.getTotalIn() + dto.getQuantity())
                        .set(DrugInventory::getLastInTime, LocalDateTime.now())
        );

        // 记录入库
        DrugInoutRecord rec = new DrugInoutRecord();
        rec.setRecordNo(generateNo("IN"));
        rec.setDrugId(dto.getDrugId());
        rec.setDrugName(drug.getDrugName());
        rec.setRecordType(1);
        rec.setQuantity(dto.getQuantity());
        rec.setUnitPrice(dto.getUnitPrice());
        rec.setTotalAmount(dto.getUnitPrice() == null
                ? null
                : dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
        rec.setBeforeStock(before);
        rec.setAfterStock(after);
        rec.setSupplier(dto.getSupplier());
        rec.setBatchNo(dto.getBatchNo());
        rec.setExpiryDate(dto.getExpiryDate());
        rec.setOperatorId(operatorId);
        rec.setOperatorName(operator != null ? operator.getName() : null);
        rec.setReason(dto.getRemark());
        rec.setCreateTime(LocalDateTime.now());
        drugInoutRecordMapper.insert(rec);
        return rec;
    }

    @Override
    @Transactional
    public DrugInoutRecord drugOut(DrugOutDTO dto, Long operatorId) {
        if (dto.getDrugId() == null || dto.getQuantity() == null || dto.getQuantity() <= 0)
            throw new RuntimeException("参数错误");

        Drug drug = drugMapper.selectById(dto.getDrugId());
        if (drug == null) throw new RuntimeException("药品不存在");

        DrugInventory inv = drugInventoryMapper.selectOne(
                new LambdaQueryWrapper<DrugInventory>().eq(DrugInventory::getDrugId, dto.getDrugId())
        );
        if (inv == null || inv.getStockQty() < dto.getQuantity())
            throw new RuntimeException("库存不足");

        Doctor operator = doctorMapper.selectById(operatorId);
        int before = inv.getStockQty();
        int after  = before - dto.getQuantity();

        drugInventoryMapper.update(null,
                new LambdaUpdateWrapper<DrugInventory>()
                        .eq(DrugInventory::getDrugId, dto.getDrugId())
                        .set(DrugInventory::getStockQty, after)
                        .set(DrugInventory::getTotalOut, inv.getTotalOut() + dto.getQuantity())
                        .set(DrugInventory::getLastOutTime, LocalDateTime.now())
        );

        DrugInoutRecord rec = new DrugInoutRecord();
        rec.setRecordNo(generateNo("OUT"));
        rec.setDrugId(dto.getDrugId());
        rec.setDrugName(drug.getDrugName());
        rec.setRecordType(dto.getRecordType());
        rec.setQuantity(dto.getQuantity());
        rec.setBeforeStock(before);
        rec.setAfterStock(after);
        rec.setReason(dto.getReason());
        rec.setOperatorId(operatorId);
        rec.setOperatorName(operator != null ? operator.getName() : null);
        rec.setCreateTime(LocalDateTime.now());
        drugInoutRecordMapper.insert(rec);
        return rec;
    }

    @Override
    public List<DrugInoutRecord> listInoutRecords(Map<String, Object> filter) {
        LambdaQueryWrapper<DrugInoutRecord> w = new LambdaQueryWrapper<>();
        if (filter.get("drugId")     != null) w.eq(DrugInoutRecord::getDrugId, filter.get("drugId"));
        if (filter.get("recordType") != null) w.eq(DrugInoutRecord::getRecordType, filter.get("recordType"));
        if (filter.get("startDate")  != null) w.ge(DrugInoutRecord::getCreateTime, filter.get("startDate"));
        if (filter.get("endDate")    != null) w.le(DrugInoutRecord::getCreateTime, filter.get("endDate"));
        w.orderByDesc(DrugInoutRecord::getCreateTime).last("LIMIT 200");
        return drugInoutRecordMapper.selectList(w);
    }

    // ════════════════════════════════════════════════════════
    //  AI 审方（核心功能）
    // ════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> aiAuditPrescription(AiAuditPrescriptionDTO dto) {
        if (dto.getDrugs() == null || dto.getDrugs().isEmpty())
            throw new RuntimeException("处方为空");

        // 收集患者信息（性别、年龄等可影响用药）
        PmiPatient patient = pmiPatientMapper.selectById(dto.getPatientId());
        StringBuilder pInfo = new StringBuilder();
        if (patient != null) {
            pInfo.append(String.format("姓名：%s，性别：%s",
                    patient.getName(),
                    patient.getGender() == null ? "未知" : (patient.getGender() == 1 ? "男" : "女")));
            if (patient.getBirthDate() != null) {
                int age = LocalDate.now().getYear() - patient.getBirthDate().getYear();
                pInfo.append("，年龄约：").append(age).append("岁");
            }
        }

        // 收集药品列表 + 药品详细信息
        StringBuilder drugList = new StringBuilder();
        for (Map<String, Object> d : dto.getDrugs()) {
            Long drugId = d.get("drugId") == null ? null : Long.valueOf(d.get("drugId").toString());
            Drug drug = drugId != null ? drugMapper.selectById(drugId) : null;
            String drugName = d.getOrDefault("drugName", drug == null ? "未知" : drug.getDrugName()).toString();

            drugList.append(String.format("- %s | 剂量：%s | 数量：%s | 天数：%s | 用法：%s",
                    drugName,
                    d.getOrDefault("dosage",    "-"),
                    d.getOrDefault("quantity",  "-"),
                    d.getOrDefault("days",      "-"),
                    d.getOrDefault("drugUsage", "-")));
            if (drug != null && drug.getContraindication() != null)
                drugList.append("  [禁忌：").append(drug.getContraindication()).append("]");
            drugList.append("\n");
        }

        String prompt = String.format("""
                你是一名专业的临床药师审方AI，请对以下处方进行严格审查：
                
                【患者信息】
                %s
                
                【处方明细】
                %s
                
                请重点检查：
                1. 药物相互作用：本处方中各药品之间是否存在不良相互作用
                2. 剂量合理性：单次剂量、每日次数、疗程是否符合常规
                3. 禁忌冲突：根据患者性别/年龄/药品禁忌是否存在禁用情况
                4. 重复用药：是否有同类药物重复
                
                请严格按照以下JSON格式输出，不要添加任何额外文字或说明：
                {
                  "result": "通过|警告|拒绝",
                  "risk_level": "无|低|中|高",
                  "summary": "一句话审方结论",
                  "issues": [
                    {"type": "相互作用|剂量|禁忌|重复", "drug": "涉及药品", "detail": "具体问题"}
                  ],
                  "suggestion": "改进建议"
                }
                """, pInfo, drugList);

        String aiResponse;
        try {
            aiResponse = chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.error("AI审方失败", e);
            throw new RuntimeException("AI服务暂不可用");
        }

        // 解析 JSON
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // 提取 JSON 部分
            int start = aiResponse.indexOf('{');
            int end   = aiResponse.lastIndexOf('}');
            String jsonStr = (start >= 0 && end > start) ? aiResponse.substring(start, end + 1) : aiResponse;
            Map<String, Object> parsed = om.readValue(jsonStr, Map.class);
            result.put("result",     parsed.getOrDefault("result", "通过"));
            result.put("riskLevel",  parsed.getOrDefault("risk_level", "无"));
            result.put("summary",    parsed.getOrDefault("summary", "审方完成"));
            result.put("issues",     parsed.getOrDefault("issues", new ArrayList<>()));
            result.put("suggestion", parsed.getOrDefault("suggestion", ""));
        } catch (Exception e) {
            log.warn("AI返回解析失败，原文：{}", aiResponse);
            result.put("result", "通过");
            result.put("riskLevel", "无");
            result.put("summary", "AI审方结果解析失败，建议人工复核");
            result.put("issues", new ArrayList<>());
            result.put("suggestion", aiResponse);
        }
        result.put("rawText", aiResponse);
        return result;
    }

    // ════════════════════════════════════════════════════════
    //  药师审核
    // ════════════════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> listPendingAudit() {
        return prescriptionMapper.selectPendingAudit();
    }

    @Override
    @Transactional
    public boolean pharmacistAudit(PharmacistAuditDTO dto, Long pharmacistId) {
        if (dto.getStatus() == null || (dto.getStatus() != 1 && dto.getStatus() != 2))
            throw new RuntimeException("审核状态无效");

        Prescription p = prescriptionMapper.selectById(dto.getPrescriptionId());
        if (p == null) throw new RuntimeException("处方不存在");
        if (p.getPharmacistStatus() != null && p.getPharmacistStatus() != 0)
            throw new RuntimeException("该处方已审核过");

        Doctor pharmacist = doctorMapper.selectById(pharmacistId);

        // 更新处方
        prescriptionMapper.update(null,
                new LambdaUpdateWrapper<Prescription>()
                        .eq(Prescription::getId, dto.getPrescriptionId())
                        .set(Prescription::getPharmacistStatus, dto.getStatus())
                        .set(Prescription::getPharmacistRemark, dto.getRemark())
        );

        // 写审核日志
        PrescriptionAudit audit = new PrescriptionAudit();
        audit.setPrescriptionId(dto.getPrescriptionId());
        audit.setAuditType(2);
        audit.setAuditResult(dto.getStatus());
        audit.setAuditContent(dto.getRemark());
        audit.setRiskLevel(0);
        audit.setAuditorId(pharmacistId);
        audit.setAuditorName(pharmacist != null ? pharmacist.getName() : null);
        audit.setCreateTime(LocalDateTime.now());
        prescriptionAuditMapper.insert(audit);

        return true;
    }

    // ════════════════════════════════════════════════════════
    //  发药
    // ════════════════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> listPendingDispense() {
        return prescriptionMapper.selectPendingDispense();
    }

    @Override
    @Transactional
    public DispenseRecord dispense(DispenseDTO dto, Long pharmacistId) {
        if (dto.getPrescriptionIds() == null || dto.getPrescriptionIds().isEmpty())
            throw new RuntimeException("请选择处方");

        // 校验所有处方
        BigDecimal total = BigDecimal.ZERO;
        Long patientId = dto.getPatientId();
        for (Long pid : dto.getPrescriptionIds()) {
            Prescription p = prescriptionMapper.selectById(pid);
            if (p == null) throw new RuntimeException("处方 " + pid + " 不存在");
            if (p.getDispenseStatus() != null && p.getDispenseStatus() == 1)
                throw new RuntimeException("处方 " + p.getPrescriptionNo() + " 已发药");
            if (p.getPharmacistStatus() == null || p.getPharmacistStatus() != 1)
                throw new RuntimeException("处方 " + p.getPrescriptionNo() + " 未通过药师审核");
            if (p.getPayStatus() == null || p.getPayStatus() != 1)
                throw new RuntimeException("处方 " + p.getPrescriptionNo() + " 未付费");
            if (patientId != null && !patientId.equals(p.getPatientId()))
                throw new RuntimeException("处方患者不一致");
            if (patientId == null) patientId = p.getPatientId();

            // 库存校验
            if (p.getDrugId() != null) {
                DrugInventory inv = drugInventoryMapper.selectOne(
                        new LambdaQueryWrapper<DrugInventory>().eq(DrugInventory::getDrugId, p.getDrugId())
                );
                int qty = p.getQuantity() == null ? 1 : p.getQuantity();
                if (inv == null || inv.getStockQty() < qty)
                    throw new RuntimeException("药品 " + p.getDrugName() + " 库存不足");
            }
            if (p.getTotalAmount() != null) total = total.add(p.getTotalAmount());
        }

        Doctor pharmacist = doctorMapper.selectById(pharmacistId);
        PmiPatient patient = pmiPatientMapper.selectById(patientId);

        // 创建发药记录
        DispenseRecord rec = new DispenseRecord();
        rec.setDispenseNo(generateNo("DP"));
        rec.setPatientId(patientId);
        rec.setPatientName(patient != null ? patient.getName() : null);
        rec.setPharmacistId(pharmacistId);
        rec.setPharmacistName(pharmacist != null ? pharmacist.getName() : null);
        try {
            rec.setPrescriptionIds(om.writeValueAsString(dto.getPrescriptionIds()));
        } catch (Exception e) { rec.setPrescriptionIds(dto.getPrescriptionIds().toString()); }
        rec.setTotalAmount(total);
        rec.setDispenseTime(LocalDateTime.now());
        rec.setStatus(1);
        rec.setRemark(dto.getRemark());
        dispenseRecordMapper.insert(rec);

        // 减库存 + 写出库记录 + 更新处方状态
        for (Long pid : dto.getPrescriptionIds()) {
            Prescription p = prescriptionMapper.selectById(pid);
            int qty = p.getQuantity() == null ? 1 : p.getQuantity();

            if (p.getDrugId() != null) {
                DrugInventory inv = drugInventoryMapper.selectOne(
                        new LambdaQueryWrapper<DrugInventory>().eq(DrugInventory::getDrugId, p.getDrugId())
                );
                int before = inv.getStockQty();
                int after  = before - qty;
                drugInventoryMapper.update(null,
                        new LambdaUpdateWrapper<DrugInventory>()
                                .eq(DrugInventory::getDrugId, p.getDrugId())
                                .set(DrugInventory::getStockQty, after)
                                .set(DrugInventory::getTotalOut, inv.getTotalOut() + qty)
                                .set(DrugInventory::getLastOutTime, LocalDateTime.now())
                );
                // 出库记录
                DrugInoutRecord outRec = new DrugInoutRecord();
                outRec.setRecordNo(generateNo("OUT"));
                outRec.setDrugId(p.getDrugId());
                outRec.setDrugName(p.getDrugName());
                outRec.setRecordType(2);
                outRec.setQuantity(qty);
                outRec.setBeforeStock(before);
                outRec.setAfterStock(after);
                outRec.setRefId(rec.getId());
                outRec.setRefNo(rec.getDispenseNo());
                outRec.setOperatorId(pharmacistId);
                outRec.setOperatorName(pharmacist != null ? pharmacist.getName() : null);
                outRec.setCreateTime(LocalDateTime.now());
                drugInoutRecordMapper.insert(outRec);
            }

            // 更新处方为已发药
            prescriptionMapper.update(null,
                    new LambdaUpdateWrapper<Prescription>()
                            .eq(Prescription::getId, pid)
                            .set(Prescription::getDispenseStatus, 1)
            );
        }
        return rec;
    }

    @Override
    public List<DispenseRecord> listDispenseRecords(Map<String, Object> filter) {
        LambdaQueryWrapper<DispenseRecord> w = new LambdaQueryWrapper<>();
        if (filter.get("patientId")    != null) w.eq(DispenseRecord::getPatientId,    filter.get("patientId"));
        if (filter.get("pharmacistId") != null) w.eq(DispenseRecord::getPharmacistId, filter.get("pharmacistId"));
        if (filter.get("startDate")    != null) w.ge(DispenseRecord::getDispenseTime, filter.get("startDate"));
        if (filter.get("endDate")      != null) w.le(DispenseRecord::getDispenseTime, filter.get("endDate"));
        w.orderByDesc(DispenseRecord::getDispenseTime).last("LIMIT 200");
        return dispenseRecordMapper.selectList(w);
    }

    // ════════════════════════════════════════════════════════
    //  今日统计
    // ════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> todayStats(Long pharmacistId) {
        LocalDate today = LocalDate.now();

        // 今日发药数
        LambdaQueryWrapper<DispenseRecord> w1 = new LambdaQueryWrapper<DispenseRecord>()
                .ge(DispenseRecord::getDispenseTime, today.atStartOfDay())
                .lt(DispenseRecord::getDispenseTime, today.plusDays(1).atStartOfDay())
                .eq(DispenseRecord::getStatus, 1);
        if (pharmacistId != null) w1.eq(DispenseRecord::getPharmacistId, pharmacistId);
        Long todayDispense = dispenseRecordMapper.selectCount(w1);

        // 待审方数
        Long pendingAudit = (long) listPendingAudit().size();

        // 待发药数
        Long pendingDispense = (long) listPendingDispense().size();

        // 低库存
        Long lowStock = (long) listLowStockDrugs().size();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayDispense",   todayDispense);
        result.put("pendingAudit",    pendingAudit);
        result.put("pendingDispense", pendingDispense);
        result.put("lowStock",        lowStock);
        return result;
    }

    // ── 工具 ─────────────────────────────────

    private String generateNo(String prefix) {
        String dt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = (int) (Math.random() * 1000);
        return prefix + dt + String.format("%03d", rand);
    }
}
