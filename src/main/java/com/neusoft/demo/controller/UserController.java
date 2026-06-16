package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.dto.RegisterDTO;
import com.neusoft.demo.service.PmiPatientService;
import com.neusoft.demo.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private PmiPatientService pmiPatientService;

    @PostMapping("/login")
    public Result<LoginVO> login(
            @RequestBody LoginDTO loginDTO){

        LoginVO loginVO =
                pmiPatientService.login(loginDTO);

        if(loginVO == null){
            return Result.fail("用户不存在");
        }

        return Result.success(loginVO);
    }

    @PostMapping("/register")
    public Result<String> register(
            @RequestBody RegisterDTO registerDTO){

        pmiPatientService.register(registerDTO);

        return Result.success("注册成功");
    }
}