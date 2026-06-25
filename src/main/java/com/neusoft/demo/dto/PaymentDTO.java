package com.neusoft.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 收费请求参数
 * 变更：新增 billType、checkOrderId，支持检查单独立缴费
 */
@Data
public class PaymentDTO {

    /**
     * 缴费类型：1=挂号费  2=检查/检验费
     * 不传时默认为 1（向后兼容）
     */
    private Integer billType = 1;

    /** 挂号单ID（billType=1 时必填） */
    private Long orderId;

    /**
     * 检查医嘱ID（billType=2 时必填，对应 medical_order.id）
     * 缴费成功后将对应 check_order.status 改为 1（已缴费/待执行）
     */
    private Long checkOrderId;

    /** 支付方式 1现金 2扫码 3医保 4银行卡 */
    private Integer payMethod;

    /** 应收金额 */
    private BigDecimal amount;

    /** 实收金额（现金可能多于应收） */
    private BigDecimal received;

    /** 支付渠道：wechat/alipay/unionpay（扫码时填） */
    private String payChannel;

    /** 第三方交易号（扫码或银行卡时填） */
    private String transactionId;

    /** 备注 */
    private String remark;

    /** 处方ID（billType=3 时使用） */
    private Long prescriptionId;
}
