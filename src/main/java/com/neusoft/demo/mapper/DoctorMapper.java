package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.entity.PmiPatient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {

    /**
     * 按姓名或手机号模糊搜索患者
     */
    @Select("""
            SELECT id, name, phone, gender, birth_date, id_card, avatar, create_time
            FROM pmi_patient
            WHERE name LIKE CONCAT('%', #{keyword}, '%')
               OR phone LIKE CONCAT('%', #{keyword}, '%')
            LIMIT 10
            """)
    List<PmiPatient> searchPatient(@Param("keyword") String keyword);
}
