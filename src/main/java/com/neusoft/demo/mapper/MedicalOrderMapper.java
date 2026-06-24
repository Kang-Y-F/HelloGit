package com.neusoft.demo.mapper;

import com.neusoft.demo.entity.MedicalOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

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

    /**
     * 查询挂号单下所有医嘱（联表查 check_order → check_item 取项目名和价格）
     *
     * 返回字段在原 MedicalOrder 基础上额外带：
     *   itemName  — 检查/检验项目名（如"糖化血红蛋白"）
     *   itemPrice — 项目价格
     *   itemId    — 项目ID
     */
    @Select("""
        SELECT
            mo.*,
            ci.name   AS itemName,
            ci.price  AS itemPrice,
            co.item_id AS itemId
        FROM medical_order mo
        LEFT JOIN check_order co ON co.order_id = mo.id
        LEFT JOIN check_item  ci ON ci.id = co.item_id
        WHERE mo.register_order_id = #{registerOrderId}
        ORDER BY mo.create_time
        """)
    List<Map<String, Object>> selectByRegisterOrderIdWithItem(Long registerOrderId);

    /** 原有：不联表的简单查询（保留兼容） */
    @Select("SELECT * FROM medical_order WHERE register_order_id = #{registerOrderId} ORDER BY create_time")
    List<MedicalOrder> selectByRegisterOrderId(Long registerOrderId);

    @Select("SELECT * FROM medical_order WHERE id = #{id}")
    MedicalOrder selectById(Long id);

    @Update("UPDATE medical_order SET exec_status = #{execStatus} WHERE id = #{id}")
    int updateExecStatus(@Param("id") Long id, @Param("execStatus") Integer execStatus);
}
