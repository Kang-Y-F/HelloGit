package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.Prescription;
import com.neusoft.demo.vo.PatientPrescriptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {

    /**
     * 查询患者所有用药记录
     * 通过 medical_order 关联找到该患者的处方
     */
    @Select("""
            SELECT p.*, d.name AS doctorName, mo.create_time AS createTime
            FROM prescription p
            LEFT JOIN medical_order mo ON p.order_id = mo.id
            LEFT JOIN doctor d ON mo.doctor_id = d.id
            WHERE mo.patient_id = #{patientId}
            ORDER BY mo.create_time DESC
            """)
    List<Prescription> selectByPatientId(Long patientId);
    /**
     * 根据患者ID，查询该患者全部处方（联查医嘱、病历、医生）
     */
    @Select(
            """
                    SELECT
                        p.id,
                        p.order_id AS medicalOrderId,
                        p.drug_code AS drugCode,
                        p.drug_name AS drugName,
                        p.dosage,
                        p.drug_usage AS drugUsage,
                        p.presc_status AS prescStatus,
                        d.name AS doctorName,
                        DATE_FORMAT(mr.create_time,'%Y-%m-%d') AS visitDate,
                        mr.chief_complaint AS chiefComplaint
                    FROM prescription p
                    LEFT JOIN medical_order mo ON p.order_id = mo.id
                    LEFT JOIN register_order ro ON mo.register_order_id = ro.id
                    LEFT JOIN medical_record mr ON ro.user_id = mr.user_id AND mr.doctor_id = mo.doctor_id
                    LEFT JOIN doctor d ON mr.doctor_id = d.id
                    WHERE mo.patient_id = #{patientId}
                    ORDER BY mr.create_time DESC
                    """
    )
    List<PatientPrescriptionVO> selectPrescriptionByPatientId(@Param("patientId") Long patientId);

    /** 药师待审核（药师人工审核） */
    @Select("""
            SELECT p.*, pat.name AS patientName, doc.name AS doctorName
            FROM prescription p
            LEFT JOIN pmi_patient pat ON p.patient_id = pat.id
            LEFT JOIN doctor doc ON p.doctor_id = doc.id
            WHERE p.audit_status = 1 OR p.audit_status = 2  -- AI通过或警告
              AND p.pharmacist_status = 0                   -- 药师还未审核
            ORDER BY p.create_time DESC
            LIMIT 100
            """)
    List<Map<String, Object>> selectPendingAudit();

    /** 待发药（药师已审核通过 + 已付费 + 未发药） */
    @Select("""
            SELECT p.*, pat.name AS patientName, doc.name AS doctorName
            FROM prescription p
            LEFT JOIN pmi_patient pat ON p.patient_id = pat.id
            LEFT JOIN doctor doc ON p.doctor_id = doc.id
            WHERE p.pharmacist_status = 1
              AND p.pay_status        = 1
              AND p.dispense_status   = 0
            ORDER BY p.create_time ASC
            LIMIT 100
            """)
    List<Map<String, Object>> selectPendingDispense();
}
