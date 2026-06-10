package com.neusoft.demo.service;

import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.dto.RegisterDTO;
import com.neusoft.demo.entity.User;

public interface UserService {

    User login(LoginDTO loginDTO);


    boolean register(RegisterDTO registerDTO);

}