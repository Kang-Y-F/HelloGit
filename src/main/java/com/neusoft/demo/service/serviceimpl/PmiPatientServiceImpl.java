package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.neusoft.demo.common.Result;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PmiPatientServiceImpl implements PmiPatientService {

    @Autowired
    private PmiPatientMapper pmiPatientMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

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

        // =========================
        // 1. 校验验证码（新增）
        // =========================
        String redisCode = (String) redisTemplate.opsForValue()
                .get("REGISTER_CODE:" + registerDTO.getPhone());

        if (redisCode == null) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }

        if (!redisCode.equals(registerDTO.getCode())) {
            throw new RuntimeException("验证码错误");
        }

        // =========================
        // 2. 校验手机号是否已注册（原逻辑）
        // =========================
        PmiPatient patient = pmiPatientMapper.selectOne(
                new LambdaQueryWrapper<PmiPatient>()
                        .eq(PmiPatient::getPhone, registerDTO.getPhone())
        );

        if (patient != null) {
            throw new RuntimeException("手机号已注册");
        }

        // =========================
        // 3. 创建用户（原逻辑）
        // =========================
        PmiPatient p = new PmiPatient();

        p.setPhone(registerDTO.getPhone());
        p.setName(registerDTO.getName());

        p.setPassword(
                PasswordUtil.encode(registerDTO.getPassword())
        );

        pmiPatientMapper.insert(p);

        // =========================
        // 4. 删除验证码（防重复使用，新增）
        // =========================
        redisTemplate.delete("REGISTER_CODE:" + registerDTO.getPhone());
    }

    @Override
    public Result sendCode(String phone) {

        // 1. 生成6位验证码
        String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));

        // 2. 存入Redis（5分钟）
        redisTemplate.opsForValue().set(
                "LOGIN_CODE:" + phone,
                code,
                5,
                TimeUnit.MINUTES
        );

        // 3. 模拟短信发送
        System.out.println("验证码：" + code);

        return Result.success("验证码已发送");
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