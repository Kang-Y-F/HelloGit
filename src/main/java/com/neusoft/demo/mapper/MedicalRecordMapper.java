package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.MedicalRecord;
import com.neusoft.demo.vo.MedicalRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MedicalRecordMapper extends BaseMapper<MedicalRecord> {

    /** 查询病历详情（联表患者、医生） */
    @Select("""
            SELECT
                mr.*,
                p.name   AS patientName,
                p.phone  AS patientPhone,
                p.gender AS gender,
                d.name   AS doctorName
            FROM medical_record mr
            LEFT JOIN pmi_patient p ON mr.user_id   = p.id
            LEFT JOIN doctor      d ON mr.doctor_id = d.id
            WHERE mr.id = #{id}
            """)
    MedicalRecordVO selectDetailById(Long id);

    /** 查询患者所有历史病历 */
    @Select("""
            SELECT
                mr.*,
                p.name   AS patientName,
                p.phone  AS patientPhone,
                p.gender AS gender,
                d.name   AS doctorName
            FROM medical_record mr
            LEFT JOIN pmi_patient p ON mr.user_id   = p.id
            LEFT JOIN doctor      d ON mr.doctor_id = d.id
            WHERE mr.user_id = #{patientId}
            ORDER BY mr.create_time DESC
            """)
    List<MedicalRecordVO> selectByPatientId(Long patientId);

    /**
     * 查询医生接诊的所有病历（支持按患者名模糊搜索）
     * keyword 为空时查全部
     */
    @Select("""
            SELECT
                mr.*,
                p.name   AS patientName,
                p.phone  AS patientPhone,
                p.gender AS gender,
                d.name   AS doctorName
            FROM medical_record mr
            LEFT JOIN pmi_patient p ON mr.user_id   = p.id
            LEFT JOIN doctor      d ON mr.doctor_id = d.id
            WHERE mr.doctor_id = #{doctorId}
              AND (
                #{keyword} IS NULL OR #{keyword} = ''
                OR p.name  LIKE CONCAT('%', #{keyword}, '%')
                OR p.phone LIKE CONCAT('%', #{keyword}, '%')
              )
            ORDER BY mr.create_time DESC
            LIMIT 50
            """)
    List<MedicalRecordVO> selectByDoctorId(
            @Param("doctorId") Long doctorId,
            @Param("keyword")  String keyword
    );
}
