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

    /** 关联 check_order.id */
    private Long orderId;

    /** 关联 medical_order.id */
    private Long orderMainId;

    /** 患者ID（新增，方便按患者查询） */
    private Long patientId;

    /** 检验项目名（血常规/血脂四项等） */
    private String itemName;

    /** 检测值 */
    private String testValue;

    /** 参考范围 */
    private String referenceRange;

    /** 0正常 1异常 */
    private Integer abnormalFlag;

    /** 0待审核 1已审核 2驳回 */
    private Integer auditStatus;

    /** 详细内容（JSON，如 {"desc":"白细胞偏高"} ） */
    private String reportContent;

    /** 录入人ID（检验科技师） */
    private Long operatorId;

    private LocalDateTime createTime;
}
