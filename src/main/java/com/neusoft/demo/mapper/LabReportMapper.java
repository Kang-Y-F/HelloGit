package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.LabReport;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface LabReportMapper extends BaseMapper<LabReport> {

    /**
     * 查询待执行的检验单（联表 pmi_patient + check_item 取患者姓名和项目名）
     */
    @Select("""
        SELECT
            co.id,
            co.user_id      AS userId,
            co.order_id     AS orderId,
            co.item_id      AS itemId,
            ci.name         AS itemName,
            p.name          AS patientName,
            p.phone         AS patientPhone,
            d.name          AS doctorName,
            CONCAT('GH', DATE_FORMAT(co.create_time,'%Y%m%d'),
                   LPAD(co.id,6,'0'))  AS orderNo,
            co.create_time  AS createTime
        FROM check_order co
        LEFT JOIN check_item   ci ON ci.id = co.item_id
        LEFT JOIN pmi_patient  p  ON p.id  = co.user_id
        LEFT JOIN doctor       d  ON d.id  = co.doctor_id
        WHERE co.order_type = 2
          AND co.status     = 1
          AND ci.item_type  = 2
          AND (
              #{keyword} IS NULL
              OR #{keyword} = ''
              OR p.name  LIKE CONCAT('%', #{keyword}, '%')
              OR p.phone LIKE CONCAT('%', #{keyword}, '%')
          )
        ORDER BY co.create_time DESC
        """)
    List<Map<String, Object>> selectPendingLabOrders(@Param("keyword") String keyword);

    /**
     * 今日已录入检验报告（带患者姓名）
     *
     * 核心逻辑：
     *   - 套餐记录（suite_group 不为 NULL）：按 suite_group 分组，合并为一行
     *   - 单项记录（suite_group 为 NULL）  ：每条记录单独一行
     *
     * 用 COALESCE(suite_group, CAST(id AS CHAR)) 作为分组键：
     *   - 套餐行：suite_group 值相同的多条记录 → 合并成一张卡片
     *   - 单项行：id 各不相同                   → 各自独立一张卡片
     */
    @Select("""
        SELECT
            MIN(lr.id)                                    AS id,
            lr.suite_group,
            lr.patient_id,
            lr.order_id,
            lr.item_name,
            -- 异常标志：套餐中任一子项异常则整张卡片标异常
            MAX(lr.abnormal_flag)                         AS abnormal_flag,
            -- 审核状态：取第一条记录的状态（套餐共享）
            MIN(lr.audit_status)                          AS audit_status,
            -- AI解读：存在第一条记录的 report_content 里
            (SELECT report_content
             FROM   lab_report
             WHERE  id = MIN(lr.id))                      AS report_content,
            MIN(lr.create_time)                           AS create_time,
            -- 检测值展示：
            --   单项 → 直接显示检测值
            --   套餐 → 拼接"子项名:值"列表，前端按 \\n 拆分渲染
            CASE
                WHEN lr.suite_group IS NULL
                THEN MIN(lr.test_value)
                ELSE GROUP_CONCAT(
                         CONCAT(COALESCE(lr.sub_item_name, lr.item_name), ':', lr.test_value)
                         ORDER BY lr.id
                         SEPARATOR '\\n'
                     )
            END                                           AS test_value,
            -- 参考范围：单项显示自身范围，套餐显示第一条（前端可自行解析）
            MIN(lr.reference_range)                       AS reference_range,
            -- 子项数量（前端判断是否是套餐）
            COUNT(*)                                      AS sub_item_count,
            p.name                                        AS patient_name
        FROM  lab_report  lr
        LEFT JOIN pmi_patient p ON p.id = lr.patient_id
        WHERE lr.operator_id = #{operatorId}
          AND DATE(lr.create_time) = CURDATE()
        GROUP BY
            COALESCE(lr.suite_group, CAST(lr.id AS CHAR)),
            lr.suite_group,
            lr.patient_id,
            lr.order_id,
            lr.item_name,
            p.name
        ORDER BY MIN(lr.create_time) DESC
        """)
    List<Map<String, Object>> selectTodayWithPatient(@Param("operatorId") Long operatorId);

    /**
     * 查询患者有历史记录的检验项目（去重）
     */
    @Select("""
        SELECT DISTINCT item_name
        FROM   lab_report
        WHERE  patient_id = #{patientId}
        ORDER BY item_name
        """)
    List<String> selectDistinctItems(@Param("patientId") Long patientId);
}