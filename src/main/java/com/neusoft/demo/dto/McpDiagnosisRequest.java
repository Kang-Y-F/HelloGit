package com.neusoft.demo.dto;

import lombok.Data;

/**
 * MCP辅助诊断请求DTO
 */
@Data
public class McpDiagnosisRequest {
    private Long patientId;
    private String symptoms;
}
