package com.neusoft.demo.service;

import com.neusoft.demo.dto.DoctorAddDTO;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.vo.LoginVO;

import java.util.List;

public interface DoctorService {
    LoginVO login(LoginDTO loginDTO);

    void addDoctor(DoctorAddDTO dto);

    List<Doctor> list();
}
