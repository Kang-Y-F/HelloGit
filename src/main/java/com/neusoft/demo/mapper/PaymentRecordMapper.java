package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.PaymentRecord;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    /**
     * 按条件查询收费记录
     * 支持 filter key: orderId / patientId / operatorId / payMethod / payStatus / keyword(患者姓名)
     */
    @Select("""
        <script>
        SELECT * FROM payment_record
        WHERE 1=1
        <if test="orderId != null">    AND order_id    = #{orderId}    </if>
        <if test="patientId != null">  AND patient_id  = #{patientId}  </if>
        <if test="operatorId != null"> AND operator_id = #{operatorId} </if>
        <if test="payMethod != null">  AND pay_method  = #{payMethod}  </if>
        <if test="payStatus != null">  AND pay_status  = #{payStatus}  </if>
        <if test="keyword != null and keyword != ''">
            AND patient_name LIKE CONCAT('%', #{keyword}, '%')
        </if>
        ORDER BY pay_time DESC
        </script>
        """)
    List<PaymentRecord> selectByFilter(Map<String, Object> filter);

    /**
     * 今日收费统计（operatorId 为 null 时统计全院）
     */
    @Select("""
        <script>
        SELECT
            COUNT(*)                                            AS totalCount,
            IFNULL(SUM(amount), 0)                             AS totalAmount,
            IFNULL(SUM(CASE WHEN pay_method=1 THEN amount END), 0) AS cashAmount,
            COUNT(CASE WHEN pay_method=1 THEN 1 END)           AS cashCount,
            IFNULL(SUM(CASE WHEN pay_method=2 THEN amount END), 0) AS qrAmount,
            COUNT(CASE WHEN pay_method=2 THEN 1 END)           AS qrCount,
            IFNULL(SUM(CASE WHEN pay_method=3 THEN amount END), 0) AS medicalAmount,
            COUNT(CASE WHEN pay_method=3 THEN 1 END)           AS medicalCount,
            IFNULL(SUM(CASE WHEN pay_method=4 THEN amount END), 0) AS cardAmount,
            COUNT(CASE WHEN pay_method=4 THEN 1 END)           AS cardCount
        FROM payment_record
        WHERE pay_status = 1
          AND DATE(pay_time) = CURDATE()
        <if test="operatorId != null"> AND operator_id = #{operatorId} </if>
        </script>
        """)
    Map<String, Object> todayStats(@Param("operatorId") Long operatorId);

    /**
     * 退款时更新收费记录状态
     */
    @Update("""
        UPDATE payment_record
        SET pay_status   = 2,
            refund_time  = #{refundTime},
            refund_reason = #{reason}
        WHERE order_id  = #{orderId}
          AND pay_status = 1
        """)
    int updateRefundByOrderId(
            @Param("orderId")    Long orderId,
            @Param("reason")     String reason,
            @Param("refundTime") LocalDateTime refundTime
    );
}
