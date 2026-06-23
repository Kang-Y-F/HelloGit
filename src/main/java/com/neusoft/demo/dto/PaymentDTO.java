package com.neusoft.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentDTO {
    
    /** 挂号单ID */
    private Long orderId;
    
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
}
