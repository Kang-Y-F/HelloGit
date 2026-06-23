package com.neusoft.demo.mapper;

import com.neusoft.demo.entity.MedicalOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * MedicalOrderMapper
 * 新增方法：updateExecStatus
 */
@Mapper
public interface MedicalOrderMapper {

    @Insert("""
        INSERT INTO medical_order
          (register_order_id, patient_id, doctor_id, order_type, exec_status, create_time)
        VALUES
          (#{registerOrderId}, #{patientId}, #{doctorId}, #{orderType}, #{execStatus}, #{createTime})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MedicalOrder order);

    @Select("SELECT * FROM medical_order WHERE register_order_id = #{registerOrderId} ORDER BY create_time")
    List<MedicalOrder> selectByRegisterOrderId(Long registerOrderId);

    @Select("SELECT * FROM medical_order WHERE id = #{id}")
    MedicalOrder selectById(Long id);

    /**
     * 更新医嘱执行状态（新增）
     *
     * exec_status 定义（与前端 ConsultView 保持一致）：
     *   0=待执行  1=执行中  2=已完成  3=已作废
     */
    @Update("UPDATE medical_order SET exec_status = #{execStatus} WHERE id = #{id}")
    int updateExecStatus(@Param("id") Long id, @Param("execStatus") Integer execStatus);
}
