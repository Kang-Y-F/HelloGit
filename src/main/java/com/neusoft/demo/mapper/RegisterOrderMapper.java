package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.vo.QueueItemVO;
import com.neusoft.demo.vo.RegisterOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RegisterOrderMapper extends BaseMapper<RegisterOrder> {

    /**
     * 查询医生候诊队列（最近7天，急诊优先，同优先级按挂号时间升序）
     * 开发期间放宽日期限制方便测试，上线前改回 CURDATE()
     */
    @Select("""
            SELECT
                ro.id            AS registerOrderId,
                ro.order_no      AS orderNo,
                ro.user_id       AS patientId,
                p.name           AS patientName,
                p.phone          AS patientPhone,
                p.gender         AS gender,
                ro.priority      AS priority,
                ro.status        AS status,
                ro.create_time   AS createTime
            FROM register_order ro
            LEFT JOIN pmi_patient p ON ro.user_id = p.id
            WHERE ro.doctor_id = #{doctorId}
              AND ro.create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
              AND ro.status IN (1, 3)
            ORDER BY ro.priority DESC, ro.create_time ASC
            """)
    List<QueueItemVO> selectTodayQueue(Long doctorId);

    List<RegisterOrderVO> listPatientOrders(Long patientId);

    RegisterOrderVO getPatientDetail(Long id);

    List<Map<String, Object>> deptRank();

    List<Map<String, Object>> doctorLoad();

    /** 挂号状态分布（环形图用） */
    @Select("""
            SELECT status, COUNT(*) AS cnt
            FROM register_order
            GROUP BY status
            """)
    List<Map<String, Object>> selectStatusDistribution();

    /** 近7日挂号量趋势（柱状图用） */
    @Select("""
            SELECT DATE(create_time) AS day, COUNT(*) AS cnt
            FROM register_order
            WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE(create_time)
            ORDER BY day ASC
            """)
    List<Map<String, Object>> selectTrend7Days();

    /** Top5 医生挂号量排行 */
    @Select("""
            SELECT d.name AS doctorName, COUNT(*) AS cnt
            FROM register_order ro
            LEFT JOIN doctor d ON ro.doctor_id = d.id
            GROUP BY ro.doctor_id, d.name
            ORDER BY cnt DESC
            LIMIT 5
            """)
    List<Map<String, Object>> selectDoctorRankTop5();
}
