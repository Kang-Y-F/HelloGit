package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.dto.PatientUpdateDTO;
import com.neusoft.demo.dto.RegisterDTO;
import com.neusoft.demo.entity.PmiPatient;
import com.neusoft.demo.service.PmiPatientService;
import com.neusoft.demo.vo.LoginVO;
import com.neusoft.demo.vo.PatientInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/sendCode")
    public Result sendCode(String phone) {
        return pmiPatientService.sendCode(phone);
    }

    @GetMapping("/info")
    public Result<PatientInfoVO> getUserInfo(HttpServletRequest request) {
        // 从JWT拦截器存入的request中获取登录用户ID
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail("用户未登录");
        }
        Long userId = Long.parseLong(userIdObj.toString());

        // 调用业务层获取脱敏信息
        PatientInfoVO patientInfo = pmiPatientService.getPatientInfoById(userId);
        if (patientInfo == null) {
            return Result.fail("用户信息不存在");
        }
        return Result.success(patientInfo);
    }

    /**
     * 修改患者个人资料
     */
    @PutMapping("/update")
    public Result<?> updateUserInfo(@RequestBody PatientUpdateDTO dto, HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        boolean result = pmiPatientService.updatePatientInfo(userId, dto);
        if (result) {
            return Result.success("资料修改成功");
        }
        return Result.fail("资料修改失败");
    }

    /** 查询患者列表 */
    @GetMapping("/list")
    public List<PmiPatient> patientList() {
        return pmiPatientService.list();
    }
}