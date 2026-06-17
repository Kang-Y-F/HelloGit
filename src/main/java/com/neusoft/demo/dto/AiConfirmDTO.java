package com.neusoft.demo.dto;

import lombok.Data;

/**
 * 医生确认/修改AI建议请求参数
 */
@Data
public class AiConfirmDTO {

    /** 1已确认 2修改后确认 3驳回 */
    private Integer confirmStatus;

    /** 修改后的诊断（可选，为空则保留AI原文） */
    private String diagnosis;

    /** 修改后的检查建议（可选） */
    private String aiCheckAdvice;

    /** 修改后的用药建议（可选） */
    private String aiDrugAdvice;
}
