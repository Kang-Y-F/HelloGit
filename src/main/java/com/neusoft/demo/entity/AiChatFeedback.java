package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_chat_feedback")
public class AiChatFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private Long doctorId;
    private Long patientId;
    private Integer reward;   // +1 好评 / -1 差评
    private LocalDateTime createTime;
}