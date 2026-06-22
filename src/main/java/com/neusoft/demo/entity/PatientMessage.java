package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("patient_message")
public class PatientMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    // 接收消息的患者ID
    private Long patientId;
    // 消息标题
    private String title;
    // 消息内容
    private String content;
    // 消息类型 1挂号 2报告 3AI导诊 4公告
    private Integer msgType;
    // 0未读 1已读
    private Integer readStatus;
    // 小程序跳转页面
    private String jumpPath;
    // 创建时间
    private LocalDateTime createTime;
}