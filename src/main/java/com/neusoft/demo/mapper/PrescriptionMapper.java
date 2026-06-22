package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
}
