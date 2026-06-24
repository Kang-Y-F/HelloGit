package com.neusoft.demo.dto;

import lombok.Data;

@Data
public class LabReportConfirmDTO {
    /** 3=修改后确认 */
    private Integer auditStatus;
    /** 修改后的内容 */
    private String editedContent;
}
