package com.neusoft.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.MedicalOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MedicalOrderMapper extends BaseMapper<MedicalOrder> {
}
