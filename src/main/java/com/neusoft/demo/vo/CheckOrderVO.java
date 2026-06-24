package com.neusoft.demo.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检查/检验单展示对象
 */
@Data
public class CheckOrderVO {

    private Long id;
    private Long recordId;
    private Long orderId;
    private Long userId;
    private String patientName;
    private String patientPhone;
    private Long doctorId;
    private String doctorName;
    private Long itemId;
    private String itemName;

    /** 项目价格（从 check_item 联表取得，供收费弹窗自动填入） */
    private BigDecimal itemPrice;

    /** 1=检查 2=检验 */
    private Integer orderType;

    /** 0=待缴费 1=已缴费/待执行 2=已取消 3=执行中 4=已完成 */
    private Integer status;

    private LocalDateTime createTime;
    private String orderNo;
    private Long registerOrderId;
}
