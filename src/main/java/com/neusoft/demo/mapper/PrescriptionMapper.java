package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.Prescription;
import com.neusoft.demo.vo.PatientPrescriptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {
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
}
