package com.neusoft.demo.service;

import com.neusoft.demo.dto.AdminAddDTO;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.vo.LoginVO;

public interface AdminService {

    LoginVO login(LoginDTO loginDTO);

    void addAdmin(AdminAddDTO dto);

}