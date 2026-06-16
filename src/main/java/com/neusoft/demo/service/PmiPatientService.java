package com.neusoft.demo.service;

import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.dto.RegisterDTO;
import com.neusoft.demo.entity.PmiPatient;
import com.neusoft.demo.vo.LoginVO;

public interface PmiPatientService {

    LoginVO login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);
}