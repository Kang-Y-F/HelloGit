package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("check_report")
public class CheckReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long patientId;

    private String imgType;

    /** 掩码文件HTTP URL */
    private String imageUrl;

    /** 原始CT文件HTTP URL */
    private String ctUrl;

    /** 患者端预览图URL（CT中间层叠加掩码后的PNG） */
    private String previewImageUrl;

    private String artifactResult;

    private String reportText;

    /** AI辅助诊断分析原文 */
    private String aiAnalysis;

    /** 医生确认/修改后的最终结论 */
    private String doctorConfirmedText;

    /** AI确认状态：0未生成 1已生成待确认 2已确认 3已修改 4已驳回 */
    private Integer aiConfirmStatus;

    private LocalDateTime createTime;

    private String deartiCtUrl;
}
