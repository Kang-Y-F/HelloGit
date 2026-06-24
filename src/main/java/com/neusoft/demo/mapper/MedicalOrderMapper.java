package com.neusoft.demo.mapper;

import com.neusoft.demo.entity.MedicalOrder;
import com.neusoft.demo.vo.MedicalOrderVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

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
     * 查询挂号单下所有医嘱，联表带项目名和价格
     * 用 @Results 显式映射下划线到驼峰
     */
    @Select("""
        SELECT
            mo.id,
            mo.register_order_id,
            mo.patient_id,
            mo.doctor_id,
            mo.order_type,
            mo.exec_status,
            mo.create_time,
            ci.name   AS item_name,
            ci.price  AS item_price,
            co.item_id AS item_id
        FROM medical_order mo
        LEFT JOIN check_order co ON co.order_id = mo.id
        LEFT JOIN check_item  ci ON ci.id = co.item_id
        WHERE mo.register_order_id = #{registerOrderId}
        ORDER BY mo.create_time
        """)
    @Results({
        @Result(column = "id",                property = "id"),
        @Result(column = "register_order_id", property = "registerOrderId"),
        @Result(column = "patient_id",        property = "patientId"),
        @Result(column = "doctor_id",         property = "doctorId"),
        @Result(column = "order_type",        property = "orderType"),
        @Result(column = "exec_status",       property = "execStatus"),
        @Result(column = "create_time",       property = "createTime"),
        @Result(column = "item_name",         property = "itemName"),
        @Result(column = "item_price",        property = "itemPrice"),
        @Result(column = "item_id",           property = "itemId")
    })
    List<MedicalOrderVO> selectByRegisterOrderIdWithItem(Long registerOrderId);

    @Select("SELECT * FROM medical_order WHERE register_order_id = #{registerOrderId} ORDER BY create_time")
    List<MedicalOrder> selectByRegisterOrderId(Long registerOrderId);

    @Select("SELECT * FROM medical_order WHERE id = #{id}")
    MedicalOrder selectById(Long id);

    @Update("UPDATE medical_order SET exec_status = #{execStatus} WHERE id = #{id}")
    int updateExecStatus(@Param("id") Long id, @Param("execStatus") Integer execStatus);
}
