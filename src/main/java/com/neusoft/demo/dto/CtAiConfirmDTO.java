
package com.neusoft.demo.dto;

import lombok.Data;

@Data
public class CtAiConfirmDTO {
    /** 2=确认 3=修改 4=驳回 */
    private Integer confirmStatus;

    /** 医生最终结论（修改时填写） */
    private String confirmedText;
}
