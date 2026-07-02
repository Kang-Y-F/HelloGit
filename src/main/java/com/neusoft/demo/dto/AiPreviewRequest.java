package com.neusoft.demo.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 解读预判请求（录入前，不落库）
 */
@Data
public class AiPreviewRequest {
    private Long patientId;
    private String itemName;          // 套餐名 或 单项名
    private String testValue;         // 单项时用
    private String referenceRange;    // 单项时用
    private List<SubItem> subItems;   // 套餐时用，单项不传/传 null 即可

    @Data
    public static class SubItem {
        private String subItemName;
        private String testValue;
        private String referenceRange;
    }
}