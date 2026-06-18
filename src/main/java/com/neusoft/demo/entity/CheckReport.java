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

    /** 掩码文件HTTP URL（伪影分割结果，红色叠加层） */
    private String imageUrl;

    /** 原始CT文件HTTP URL（用于医生端四视图渲染） */
    private String ctUrl;

    private String artifactResult;

    private String reportText;

    private LocalDateTime createTime;
}
