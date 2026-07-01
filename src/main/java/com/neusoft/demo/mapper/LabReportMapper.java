package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.LabReport;
import com.neusoft.demo.vo.CheckOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface LabReportMapper extends BaseMapper<LabReport> {

    /** 待执行检验单（status=1 + orderType=2），带价格 */
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
        WHERE co.status = 1 AND co.order_type = 2
        <if test="keyword != null and keyword != ''">
            AND (p.name LIKE CONCAT('%', #{keyword}, '%')
              OR p.phone LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY co.create_time DESC
        </script>
        """)
    List<CheckOrderVO> selectPendingLabOrders(@Param("keyword") String keyword);

    /**
     * 今日已录入检验报告（带患者姓名）
     * 联表 pmi_patient 取患者姓名
     */
    @Select("""
        SELECT
            lr.*,
            p.name AS patient_name
        FROM lab_report lr
        LEFT JOIN pmi_patient p ON p.id = lr.patient_id
        WHERE lr.operator_id = #{operatorId}
          AND DATE(lr.create_time) = CURDATE()
        ORDER BY lr.create_time DESC
        """)
    List<Map<String, Object>> selectTodayWithPatient(@Param("operatorId") Long operatorId);

    @Select("SELECT DISTINCT item_name FROM lab_report WHERE patient_id = #{patientId} ORDER BY item_name")
    List<String> selectDistinctItems(@Param("patientId") Long patientId);
}
