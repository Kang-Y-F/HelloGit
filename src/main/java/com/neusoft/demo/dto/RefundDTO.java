package com.neusoft.demo.dto;

import lombok.Data;

@Data
public class RefundDTO {
    private Long orderId;
    private String reason;
}
