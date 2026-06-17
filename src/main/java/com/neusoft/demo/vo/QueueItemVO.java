package com.neusoft.demo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 候诊队列单条记录
 */
@Data
public class QueueItemVO {

    private Long registerOrderId;

    private String orderNo;

    private Long patientId;

    private String patientName;

    private String patientPhone;

    /** 0女 1男 */
    private Integer gender;

    /** 0普通 1急诊 */
    private Integer priority;

    /** 1候诊 3就诊中 */
    private Integer status;

    private LocalDateTime createTime;
}
