package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.entity.HospitalInfo;
import com.neusoft.demo.mapper.HospitalInfoMapper;
import com.neusoft.demo.service.HospitalInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HospitalInfoServiceImpl implements HospitalInfoService {

    @Autowired
    private HospitalInfoMapper mapper;

    /** 查询医院信息 */
    @Override
    public HospitalInfo getInfo() {

        return mapper.selectOne(null);

    }

    /** 修改医院信息 */
    @Override
    public void updateInfo(HospitalInfo info) {

        mapper.updateById(info);

    }

}
