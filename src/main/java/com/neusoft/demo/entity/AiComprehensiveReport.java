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

    private String langgraphThreadId;   // 新增：对应 langgraph_thread_id
    private String genStatus;           // 新增：对应 gen_status

    private LocalDateTime createTime;

    // 审核状态：pending / confirmed
    private String reviewStatus;

    // 是否被医生编辑过
    private Boolean isEdited;

    private String confirmedBy;
    private LocalDateTime confirmedAt;

    private String lastEditedBy;
    private LocalDateTime lastEditedAt;
}