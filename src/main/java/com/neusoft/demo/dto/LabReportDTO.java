package com.neusoft.demo.dto;

import lombok.Data;

/**
 * 检验报告录入请求参数
 */
@Data
public class LabReportDTO {

    /** check_order.id（从待执行检验单选择） */
    private Long checkOrderId;

    /** 检验项目名 */
    private String itemName;

    /** 检测值 */
    private String testValue;

    /** 参考范围 */
    private String referenceRange;

    /** 详细描述 */
    private String description;
}
