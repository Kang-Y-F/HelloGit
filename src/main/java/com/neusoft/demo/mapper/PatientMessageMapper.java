package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.PatientMessage;
import com.neusoft.demo.vo.PatientMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PatientMessageMapper extends BaseMapper<PatientMessage> {

    /**
     * 根据患者ID查询自己所有消息
     */
    @Select("""
        SELECT
            id, title, content, msg_type AS msgType, read_status AS readStatus, jump_path AS jumpPath,
            DATE_FORMAT(create_time, '%Y-%m-%d %H:%m') AS createTime
        FROM patient_message
        WHERE patient_id = #{patientId}
        ORDER BY create_time DESC
        """)
    List<PatientMessageVO> listByPatientId(@Param("patientId") Long patientId);
}