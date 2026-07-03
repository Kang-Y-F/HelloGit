package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_comprehensive_report")
public class AiComprehensiveReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;
    private String patientName;

    private String reportJson;
    private String chainStepsJson;
    private String dataSummaryJson;

    private LocalDateTime createTime;
}