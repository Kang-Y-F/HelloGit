package com.neusoft.demo.service;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.dto.PatientUpdateDTO;
import com.neusoft.demo.dto.RegisterDTO;
import com.neusoft.demo.entity.PmiPatient;
import com.neusoft.demo.vo.LoginVO;
import com.neusoft.demo.vo.PatientInfoVO;

import java.util.List;

public interface PmiPatientService {

    LoginVO login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    PatientInfoVO getPatientInfoById(Long userId);

    boolean updatePatientInfo(Long userId, PatientUpdateDTO dto);

    /** 查询所有患者列表 */
    List<PmiPatient> list();

    Result sendCode(String phone);
}