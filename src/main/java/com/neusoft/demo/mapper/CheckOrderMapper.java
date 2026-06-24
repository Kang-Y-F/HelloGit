package com.neusoft.demo.mapper;

import com.neusoft.demo.entity.CheckOrder;
import com.neusoft.demo.vo.CheckOrderVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CheckOrderMapper {

    @Select("SELECT * FROM check_order WHERE user_id = #{patientId} ORDER BY create_time DESC")
    List<CheckOrder> selectByPatientId(Long patientId);

    @Select("SELECT * FROM check_order WHERE id = #{id}")
    CheckOrder selectById(Long id);

    @Insert("""
        INSERT INTO check_order
          (record_id, order_id, user_id, doctor_id, item_id, order_type, status, create_time)
        VALUES
          (#{recordId}, #{orderId}, #{userId}, #{doctorId}, #{itemId}, #{orderType}, #{status}, #{createTime})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CheckOrder checkOrder);

    @Update("UPDATE check_order SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 待缴费检查单（带价格） */
    @Select("""
        <script>
        SELECT
            co.id, co.record_id AS recordId, co.order_id AS orderId,
            co.user_id AS userId, p.name AS patientName, p.phone AS patientPhone,
            co.doctor_id AS doctorId, a.name AS doctorName,
            co.item_id AS itemId, ci.name AS itemName, ci.price AS itemPrice,
            co.order_type AS orderType, co.status, co.create_time AS createTime,
            ro.order_no AS orderNo, ro.id AS registerOrderId
        FROM check_order co
        LEFT JOIN pmi_patient    p  ON p.id  = co.user_id
        LEFT JOIN admin          a  ON a.id  = co.doctor_id
        LEFT JOIN check_item     ci ON ci.id = co.item_id
        LEFT JOIN medical_order  mo ON mo.id = co.order_id
        LEFT JOIN register_order ro ON ro.id = mo.register_order_id
        WHERE co.status = 0
        <if test="patientId != null"> AND co.user_id = #{patientId} </if>
        <if test="keyword != null and keyword != ''">
            AND (p.name LIKE CONCAT('%', #{keyword}, '%')
              OR p.phone LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY co.create_time DESC
        </script>
        """)
    List<CheckOrderVO> selectPendingPayment(
            @Param("patientId") Long patientId,
            @Param("keyword") String keyword);

    /** 按挂号单查检查单（带价格） */
    @Select("""
        SELECT
            co.id, co.record_id AS recordId, co.order_id AS orderId,
            co.user_id AS userId, p.name AS patientName, p.phone AS patientPhone,
            co.doctor_id AS doctorId, a.name AS doctorName,
            co.item_id AS itemId, ci.name AS itemName, ci.price AS itemPrice,
            co.order_type AS orderType, co.status, co.create_time AS createTime,
            ro.order_no AS orderNo, ro.id AS registerOrderId
        FROM check_order co
        LEFT JOIN pmi_patient    p  ON p.id  = co.user_id
        LEFT JOIN admin          a  ON a.id  = co.doctor_id
        LEFT JOIN check_item     ci ON ci.id = co.item_id
        LEFT JOIN medical_order  mo ON mo.id = co.order_id
        LEFT JOIN register_order ro ON ro.id = mo.register_order_id
        WHERE mo.register_order_id = #{registerOrderId}
        ORDER BY co.create_time DESC
        """)
    List<CheckOrderVO> selectByRegisterOrderId(Long registerOrderId);

    // CheckOrderMapper.java 新增：
    @Select("SELECT * FROM check_order WHERE order_id = #{orderId}")
    List<CheckOrder> selectByOrderId(Long orderId);
}
