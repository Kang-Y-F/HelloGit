package com.neusoft.demo.dto;

import lombok.Data;
import java.util.List;

/**
 * 套餐检验结果录入请求体
 * 单指标检验时 subItems 只有一条，subItemName 等于 itemName
 */
@Data
public class LabReportSuiteDTO {

    /** 关联的 check_order.id */
    private Long checkOrderId;

    /** 套餐名称，如"肾功能五项" */
    private String itemName;

    /** 各子项结果列表 */
    private List<SubItem> subItems;

    @Data
    public static class SubItem {
        /** 子项名称，如"肌酐"；单指标时等于 itemName */
        private String subItemName;
        private String testValue;
        private String referenceRange;
        private String description;
    }
}