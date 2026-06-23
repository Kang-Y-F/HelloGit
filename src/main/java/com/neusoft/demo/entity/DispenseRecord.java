package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dispense_record")
public class DispenseRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dispenseNo;
    private Long patientId;
    private String patientName;
    private Long pharmacistId;
    private String pharmacistName;
    /** 关联多个处方ID的JSON数组 */
    private String prescriptionIds;
    private BigDecimal totalAmount;
    private LocalDateTime dispenseTime;
    private Integer status;
    private String remark;
}
