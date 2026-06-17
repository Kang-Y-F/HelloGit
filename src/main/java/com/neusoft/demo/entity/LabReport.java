// LabReport.java
package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("lab_report")
public class LabReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long orderMainId;

    private String itemName;

    private String testValue;

    private String referenceRange;

    /** 0正常 1异常 */
    private Integer abnormalFlag;

    /** 0待审核 1已审核 2驳回 */
    private Integer auditStatus;

    private String reportContent;

    private LocalDateTime createTime;
}
