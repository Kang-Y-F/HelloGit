package com.neusoft.demo.service;

import com.neusoft.demo.dto.*;
import com.neusoft.demo.entity.DispenseRecord;
import com.neusoft.demo.entity.DrugInoutRecord;

import java.util.List;
import java.util.Map;

public interface PharmacyService {

    // ── 药品库存 ──────────────────────────────
    List<Map<String, Object>> listAllDrugsWithStock();
    List<Map<String, Object>> listLowStockDrugs();

    // ── 入库出库 ──────────────────────────────
    DrugInoutRecord drugIn(DrugInDTO dto, Long operatorId);
    DrugInoutRecord drugOut(DrugOutDTO dto, Long operatorId);
    List<DrugInoutRecord> listInoutRecords(Map<String, Object> filter);

    // ── AI审方 ──────────────────────────────
    Map<String, Object> aiAuditPrescription(AiAuditPrescriptionDTO dto);

    // ── 药师审核 ────────────────────────────
    List<Map<String, Object>> listPendingAudit();
    boolean pharmacistAudit(PharmacistAuditDTO dto, Long pharmacistId);

    // ── 发药 ───────────────────────────────
    List<Map<String, Object>> listPendingDispense();
    DispenseRecord dispense(DispenseDTO dto, Long pharmacistId);
    List<DispenseRecord> listDispenseRecords(Map<String, Object> filter);

    // ── 统计 ───────────────────────────────
    Map<String, Object> todayStats(Long pharmacistId);
}
