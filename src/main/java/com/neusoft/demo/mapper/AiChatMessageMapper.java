package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {

    @Select("""
            SELECT * FROM ai_chat_message
            WHERE doctor_id = #{doctorId} AND patient_id = #{patientId}
            ORDER BY create_time ASC
            """)
    List<AiChatMessage> selectByDoctorAndPatient(Long doctorId, Long patientId);
}
