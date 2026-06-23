package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("prescription_audit")
public class PrescriptionAudit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long prescriptionId;
    /** 1=AI 2=药师 */
    private Integer auditType;
    /** 0拒绝 1通过 2警告 */
    private Integer auditResult;
    private String auditContent;
    /** 0无 1低 2中 3高 */
    private Integer riskLevel;
    private Long auditorId;
    private String auditorName;
    private LocalDateTime createTime;
}
