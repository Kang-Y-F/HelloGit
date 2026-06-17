package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.AiOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiOperationLogMapper extends BaseMapper<AiOperationLog> {

    @Select("SELECT * FROM ai_operation_log WHERE record_id = #{recordId} ORDER BY operate_time DESC")
    List<AiOperationLog> selectByRecordId(Long recordId);
}
