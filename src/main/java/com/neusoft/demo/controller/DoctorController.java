package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.DoctorAddDTO;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.service.DoctorService;
import com.neusoft.demo.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctor")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @PostMapping("/login")
    public Result<LoginVO> login(
            @RequestBody LoginDTO loginDTO){

        LoginVO loginVO =
                doctorService.login(loginDTO);

        if(loginVO == null){
            return Result.fail("账号或密码错误");
        }

        return Result.success(loginVO);
    }

    @PostMapping("/add")
    public Result<String> addDoctor(
            @RequestBody DoctorAddDTO dto){

        doctorService.addDoctor(dto);

        return Result.success("新增医生成功");
    }

    @GetMapping("/list")
    public Result<List<Doctor>> list(){

        List<Doctor> list = doctorService.list();

        return Result.success(list);

    }
}