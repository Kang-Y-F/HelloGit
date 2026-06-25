package com.neusoft.demo.service;

import com.neusoft.demo.entity.HospitalInfo;

public interface HospitalInfoService {
    /** 查询医院信息 */
    HospitalInfo getInfo();

    /** 修改医院信息 */
    void updateInfo(HospitalInfo info);
}
