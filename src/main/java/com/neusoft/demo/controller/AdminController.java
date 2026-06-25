package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.AdminAddDTO;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.service.AdminService;
import com.neusoft.demo.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ===================== 基础功能模块 =====================
    @PostMapping("/login")
    public Result<LoginVO> login(
            @RequestBody LoginDTO loginDTO){

        LoginVO loginVO =
                adminService.login(loginDTO);

        if(loginVO == null){
            return Result.fail("账号或密码错误");
        }

        return Result.success(loginVO);
    }

    @PostMapping("/add")
    public Result<String> addAdmin(
            @RequestBody AdminAddDTO dto){

        adminService.addAdmin(dto);

        return Result.success("新增管理员成功");
    }


}