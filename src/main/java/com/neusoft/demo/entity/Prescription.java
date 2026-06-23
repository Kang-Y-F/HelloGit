package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prescription")
public class Prescription {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 处方编号 */
    private String prescriptionNo;

    /** 关联医嘱ID */
    private Long orderId;

    /** 患者ID（冗余） */
    private Long patientId;

    /** 医生ID（冗余） */
    private Long doctorId;

    /** 药品编码 */
    private String drugCode;

    /** 关联药品ID */
    private Long drugId;

    /** 药品名称 */
    private String drugName;

    /** 单次剂量（如 1片 / 0.5g） */
    private String dosage;

    /** 数量（开多少盒/瓶） */
    private Integer quantity;

    /** 天数 */
    private Integer days;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 用法 */
    private String drugUsage;

    /** 处方状态 0草稿 1已开方 */
    private Integer prescStatus;

    /** AI审方状态 0待审 1通过 2警告 3拒绝 */
    private Integer auditStatus;

    /** AI审方意见 */
    private String auditResult;

    /** 药师审核 0待审 1通过 2拒绝 */
    private Integer pharmacistStatus;

    /** 药师备注 */
    private String pharmacistRemark;

    /** 付费状态 0未付 1已付 2已退 */
    private Integer payStatus;

    /** 发药状态 0未发 1已发 */
    private Integer dispenseStatus;

    private LocalDateTime createTime;

    private Long RegisterOrderId;

    private Long MedicalOrderId;


}
