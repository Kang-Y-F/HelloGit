package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;

    private Long orderId;

    private String orderNo;

    private Long patientId;

    private String patientName;

    private BigDecimal amount;

    private BigDecimal received;

    private BigDecimal changeAmount;

    /** 1现金 2扫码 3医保 4银行卡 */
    private Integer payMethod;

    private String payChannel;

    private String transactionId;

    private Long operatorId;

    private String operatorName;

    /** 0待支付 1支付成功 2已退款 */
    private Integer payStatus;

    private LocalDateTime payTime;

    private LocalDateTime refundTime;

    private String refundReason;

    private String remark;
}
