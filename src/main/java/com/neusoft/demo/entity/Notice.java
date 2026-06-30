package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 发布人ID */
    private Long creatorId;

    /** 发布时间 */
    private LocalDateTime createTime;

    /** 公告对象 1医生 2患者 3全部 */
    private Integer targetType;

}