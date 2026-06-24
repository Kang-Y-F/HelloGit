package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_operation_log")
public class AiOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private String aiOriginal;

    private String doctorModify;

    /**
     * 0查看 1修改 2确认 3驳回
     */
    private Integer operateType;

    private LocalDateTime operateTime;
}
