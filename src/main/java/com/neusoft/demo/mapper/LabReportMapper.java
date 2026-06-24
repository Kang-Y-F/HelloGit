package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.LabReport;
import com.neusoft.demo.vo.CheckOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LabReportMapper extends BaseMapper<LabReport> {

    /**
     * 待执行检验单
     * 条件：check_order.status=1（已缴费/待执行）且 order_type=2（检验）
     * 联表取患者姓名、医生姓名、挂号单号
     */
    @Select("""
        <script>
        SELECT
            co.id,
            co.record_id        AS recordId,
            co.order_id         AS orderId,
            co.user_id          AS userId,
            p.name              AS patientName,
            p.phone             AS patientPhone,
            co.doctor_id        AS doctorId,
            a.name              AS doctorName,
            co.item_id          AS itemId,
            ci.name             AS itemName,
            co.order_type       AS orderType,
            co.status,
            co.create_time      AS createTime,
            ro.order_no         AS orderNo,
            ro.id               AS registerOrderId
        FROM check_order co
        LEFT JOIN pmi_patient    p  ON p.id  = co.user_id
        LEFT JOIN admin          a  ON a.id  = co.doctor_id
        LEFT JOIN check_item     ci ON ci.id = co.item_id
        LEFT JOIN medical_order  mo ON mo.id = co.order_id
        LEFT JOIN register_order ro ON ro.id = mo.register_order_id
        WHERE co.status = 1
          AND co.order_type = 2
        <if test="keyword != null and keyword != ''">
            AND (p.name LIKE CONCAT('%', #{keyword}, '%')
              OR p.phone LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY co.create_time DESC
        </script>
        """)
    List<CheckOrderVO> selectPendingLabOrders(@Param("keyword") String keyword);
}
