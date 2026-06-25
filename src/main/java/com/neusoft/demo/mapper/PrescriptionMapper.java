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
        SELECT p.id,
               p.prescription_no  AS prescriptionNo,
               p.drug_code        AS drugCode,
               p.drug_name        AS drugName,
               p.dosage,
               p.quantity,
               p.days,
               p.drug_usage       AS drugUsage,
               p.unit_price       AS unitPrice,
               p.total_amount     AS totalAmount,
               p.presc_status     AS prescStatus,
               p.audit_status     AS auditStatus,
               p.pay_status       AS payStatus,
               p.dispense_status  AS dispenseStatus,
               p.create_time      AS createTime,
               d.name             AS doctorName
        FROM prescription p
        LEFT JOIN doctor d ON d.id = p.doctor_id
        WHERE p.patient_id = #{patientId}
        ORDER BY p.create_time DESC
        """)
    List<Map<String, Object>> selectByPatientId(Long patientId);
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

    @Select("""
        SELECT p.id,
               p.prescription_no   AS prescriptionNo,
               p.patient_id        AS patientId,
               p.doctor_id         AS doctorId,
               p.drug_id           AS drugId,
               p.drug_code         AS drugCode,
               p.drug_name         AS drugName,
               p.dosage,
               p.quantity,
               p.days,
               p.drug_usage        AS drugUsage,
               p.unit_price        AS unitPrice,
               p.total_amount      AS totalAmount,
               p.audit_status      AS auditStatus,
               p.audit_result      AS auditResult,
               p.pharmacist_status AS pharmacistStatus,
               p.create_time       AS createTime,
               pat.name            AS patientName,
               doc.name            AS doctorName
        FROM prescription p
        LEFT JOIN pmi_patient pat ON pat.id = p.patient_id
        LEFT JOIN doctor doc      ON doc.id = p.doctor_id
        WHERE p.presc_status = 1
          AND p.audit_status IN (1, 2, 3)
          AND (p.pharmacist_status = 0 OR p.pharmacist_status IS NULL)
        ORDER BY p.create_time DESC
        LIMIT 100
        """)
    List<Map<String, Object>> selectPendingAudit();

    @Select("""
        SELECT p.id,
               p.prescription_no   AS prescriptionNo,
               p.patient_id        AS patientId,
               p.doctor_id         AS doctorId,
               p.drug_id           AS drugId,
               p.drug_code         AS drugCode,
               p.drug_name         AS drugName,
               p.dosage,
               p.quantity,
               p.days,
               p.drug_usage        AS drugUsage,
               p.unit_price        AS unitPrice,
               p.total_amount      AS totalAmount,
               p.create_time       AS createTime,
               pat.name            AS patientName,
               doc.name            AS doctorName
        FROM prescription p
        LEFT JOIN pmi_patient pat ON pat.id = p.patient_id
        LEFT JOIN doctor doc      ON doc.id = p.doctor_id
        WHERE p.pharmacist_status = 1
          AND p.pay_status        = 1
          AND (p.dispense_status  = 0 OR p.dispense_status IS NULL)
        ORDER BY p.create_time ASC
        LIMIT 100
        """)
    List<Map<String, Object>> selectPendingDispense();
    @Select("""
        SELECT p.*, 
               pat.name AS patientName, pat.phone AS patientPhone,
               doc.name AS doctorName
        FROM prescription p
        LEFT JOIN pmi_patient pat ON p.patient_id = pat.id
        LEFT JOIN doctor doc      ON p.doctor_id  = doc.id
        WHERE p.pharmacist_status = 1
          AND p.pay_status = 0
          AND (<if test="keyword != null and keyword != ''">
                 pat.name LIKE CONCAT('%',#{keyword},'%')
                 OR pat.phone LIKE CONCAT('%',#{keyword},'%')
               </if>
               <if test="keyword == null or keyword == ''">1=1</if>)
        ORDER BY p.create_time ASC
        LIMIT 100
        """)
    List<Map<String, Object>> selectPendingPayment(@Param("keyword") String keyword);
}
