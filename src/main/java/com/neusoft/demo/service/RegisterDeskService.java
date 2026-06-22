package com.neusoft.demo.service;

import com.neusoft.demo.dto.RegisterCreateDTO;
import com.neusoft.demo.entity.RegisterOrder;

public interface RegisterDeskService {
    RegisterOrder createRegisterOrder(RegisterCreateDTO dto);
}