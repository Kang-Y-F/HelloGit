package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.CheckReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CheckReportMapper extends BaseMapper<CheckReport> {

    @Select("SELECT * FROM check_report WHERE patient_id = #{patientId} ORDER BY create_time DESC")
    List<CheckReport> selectByPatientId(Long patientId);
}
