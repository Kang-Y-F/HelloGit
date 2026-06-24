
package com.neusoft.demo.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 医嘱展示对象（联表带检查项目名和价格）
 */
@Data
public class MedicalOrderVO {

    private Long id;
    private Long registerOrderId;
    private Long patientId;
    private Long doctorId;

    /** 1检查 2检验 3用药 */
    private Integer orderType;

    /** 0待执行 1执行中 2已完成 3已作废 */
    private Integer execStatus;

    private LocalDateTime createTime;

    /** 检查/检验项目名称（联表 check_item） */
    private String itemName;

    /** 检查/检验项目价格（联表 check_item） */
    private BigDecimal itemPrice;

    /** 检查/检验项目ID */
    private Long itemId;
}
