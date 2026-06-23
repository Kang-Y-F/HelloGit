package com.neusoft.demo.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 检查/检验单展示对象（供挂号台收费、医生查询流程状态使用）
 */
@Data
public class CheckOrderVO {

    /** check_order.id */
    private Long id;

    /** 关联病历ID */
    private Long recordId;

    /** 关联医嘱ID（medical_order.id） */
    private Long orderId;

    /** 患者ID */
    private Long userId;

    /** 患者姓名（联表查询） */
    private String patientName;

    /** 患者手机号 */
    private String patientPhone;

    /** 开单医生ID */
    private Long doctorId;

    /** 开单医生姓名（联表查询） */
    private String doctorName;

    /** 检查项目ID */
    private Long itemId;

    /** 检查项目名称（联表查询） */
    private String itemName;

    /**
     * 医嘱类型：1=检查  2=检验
     */
    private Integer orderType;

    /**
     * 检查单状态：
     *   0=待缴费
     *   1=已缴费/待执行
     *   2=已取消
     *   3=执行中
     *   4=已完成（结果已回传）
     */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 关联挂号单号（通过 medical_order.register_order_id 联表取得，方便前端展示） */
    private String orderNo;

    /** 关联挂号单ID */
    private Long registerOrderId;
}
