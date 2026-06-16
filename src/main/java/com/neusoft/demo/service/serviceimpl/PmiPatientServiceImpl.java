package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.dto.RegisterDTO;
import com.neusoft.demo.entity.PmiPatient;
import com.neusoft.demo.mapper.PmiPatientMapper;
import com.neusoft.demo.service.PmiPatientService;
import com.neusoft.demo.utils.JwtUtil;
import com.neusoft.demo.utils.PasswordUtil;
import com.neusoft.demo.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PmiPatientServiceImpl implements PmiPatientService {

    @Autowired
    private PmiPatientMapper pmiPatientMapper;

    @Override
    public LoginVO login(LoginDTO loginDTO) {

        PmiPatient patient = pmiPatientMapper.selectOne(
                new LambdaQueryWrapper<PmiPatient>()
                        .eq(PmiPatient::getPhone, loginDTO.getUsername())
        );

        if (patient == null) {
            return null;
        }

        // 密码校验（BCrypt）
        if (!PasswordUtil.matches(loginDTO.getPassword(), patient.getPassword())) {
            return null;
        }

        String token = JwtUtil.createToken(patient.getId(), "PATIENT");

        return new LoginVO(
                patient.getId(),
                patient.getName(),
                "PATIENT",
                token
        );
    }

    @Override
    public void register(RegisterDTO registerDTO) {

        PmiPatient patient = pmiPatientMapper.selectOne(
                new LambdaQueryWrapper<PmiPatient>()
                        .eq(PmiPatient::getPhone,
                                registerDTO.getPhone())
        );

        if(patient != null){
            throw new RuntimeException("手机号已注册");
        }

        PmiPatient p = new PmiPatient();

        p.setPhone(registerDTO.getPhone());

        p.setName(registerDTO.getName());

        p.setPassword(
                PasswordUtil.encode(
                        registerDTO.getPassword()
                )
        );

        pmiPatientMapper.insert(p);
    }
}