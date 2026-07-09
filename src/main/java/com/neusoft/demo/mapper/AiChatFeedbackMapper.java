package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.AiChatFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiChatFeedbackMapper extends BaseMapper<AiChatFeedback> {

    @Select("SELECT COUNT(*) FROM ai_chat_feedback WHERE patient_id = #{patientId}")
    int countByPatient(Long patientId);
}