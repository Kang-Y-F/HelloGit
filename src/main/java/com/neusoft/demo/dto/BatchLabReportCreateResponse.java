package com.neusoft.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchLabReportCreateResponse {
    private int successCount;
    private int failCount;
    private List<String> failReasons; // 失败的具体原因，方便排查
}