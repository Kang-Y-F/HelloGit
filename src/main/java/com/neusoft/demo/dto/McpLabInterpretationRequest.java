package com.neusoft.demo.dto;

import lombok.Data;
import java.util.List;

/**
 * MCP检验报告解读请求DTO
 */
@Data
public class McpLabInterpretationRequest {
    private List<String> abnormalItems;
    private String symptoms;
}
