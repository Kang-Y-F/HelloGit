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

    /** 患者ID */
    private Long patientId;

    /** 检验套餐名（血脂四项/肾功能五项等），单指标时也存这里 */
    private String itemName;

    /** 子项目名称（肌酐/尿素等），单指标时为 NULL */
    private String subItemName;

    /** 检测值 */
    private String testValue;

    /** 参考范围 */
    private String referenceRange;

    /** 0正常 1异常 */
    private Integer abnormalFlag;

    /** 0待审核 1已审核 2驳回 3修改后确认 */
    private Integer auditStatus;

    /** 详细内容（JSON，如 {"desc":"AI解读文字"} ） */
    private String reportContent;

    /** 套餐分组ID，同一次套餐提交的多条记录共享同一个UUID；单指标时为 NULL */
    private String suiteGroup;

    /** 录入人ID */
    private Long operatorId;

    private LocalDateTime createTime;
}