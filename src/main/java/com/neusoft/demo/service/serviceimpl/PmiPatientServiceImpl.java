package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.dto.PatientUpdateDTO;
import com.neusoft.demo.dto.RegisterDTO;
import com.neusoft.demo.entity.PmiPatient;
import com.neusoft.demo.mapper.PmiPatientMapper;
import com.neusoft.demo.service.PmiPatientService;
import com.neusoft.demo.utils.JwtUtil;
import com.neusoft.demo.utils.PasswordUtil;
import com.neusoft.demo.vo.LoginVO;
import com.neusoft.demo.vo.PatientInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    public PatientInfoVO getPatientInfoById(Long userId) {
        // 1. 根据ID查询患者实体
        PmiPatient patient = pmiPatientMapper.selectById(userId);
        if (patient == null) {
            return null;
        }
        // 2. 实体转VO，脱敏返回
        PatientInfoVO vo = new PatientInfoVO();
        vo.setId(patient.getId());
        vo.setName(patient.getName());
        vo.setPhone(patient.getPhone());
        vo.setAvatar(patient.getAvatar()); // 设置头像URL
        return vo;
    }

    @Override
    public boolean updatePatientInfo(Long userId, PatientUpdateDTO dto) {
        LambdaUpdateWrapper<PmiPatient> wrapper = new LambdaUpdateWrapper<>();
        wrapper
                .eq(PmiPatient::getId, userId)
                .set(PmiPatient::getName, dto.getName())
                .set(PmiPatient::getAvatar, dto.getAvatar());
        // mybatis-plus update 返回受影响行数
        int rows = pmiPatientMapper.update(null, wrapper);
        return rows > 0;
    }

    @Override
    public List<PmiPatient> list() {
        return pmiPatientMapper.selectList(null);
    }
}