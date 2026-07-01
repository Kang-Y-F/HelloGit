package com.neusoft.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchLabReportCreateRequest {
    private List<LabReportItem> items;

    @Data
    public static class LabReportItem {
        private Long orderId;          // 对应 check_order.id，原来写成 checkOrderId 是错的
        private Long orderMainId;      // 对应 medical_order.id，可选
        private Long patientId;        // 必传，后续趋势/CGM查询都靠这个
        private String itemName;
        private String testValue;
        private String referenceRange;
        private String description;    // 这个仍然保留在DTO里收前端传来的"原始描述"，但不直接存进实体的某个字段，要拼进 reportContent
        private String testTime;
        private Long operatorId;       // 可选，仿真数据可以传一个固定的"系统/仿真"操作员ID
    }
}