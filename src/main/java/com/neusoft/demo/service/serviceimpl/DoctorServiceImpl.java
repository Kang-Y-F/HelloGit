package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.dto.DoctorAddDTO;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.service.DoctorService;
import com.neusoft.demo.utils.JwtUtil;
import com.neusoft.demo.utils.PasswordUtil;
import com.neusoft.demo.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorMapper doctorMapper;

    @Override
    public LoginVO login(LoginDTO loginDTO) {

        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>()
                        .eq(
                                Doctor::getUsername,
                                loginDTO.getUsername()
                        )
        );

        // 用户不存在
        if (doctor == null) {
            return null;
        }

        // 密码校验
        boolean match = PasswordUtil.matches(
                loginDTO.getPassword(),
                doctor.getPassword()
        );

        if (!match) {
            return null;
        }

        String token = JwtUtil.createToken(
                doctor.getId(),
                "DOCTOR"
        );

        return new LoginVO(
                doctor.getId(),
                doctor.getName(),
                "DOCTOR",
                token
        );
    }

    @Override
    public void addDoctor(DoctorAddDTO dto) {

        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>()
                        .eq(Doctor::getUsername,
                                dto.getUsername())
        );

        if(doctor != null){
            throw new RuntimeException("账号已存在");
        }

        Doctor d = new Doctor();

        d.setName(dto.getName());

        d.setUsername(dto.getUsername());

        d.setPassword(
                PasswordUtil.encode(
                        dto.getPassword()
                )
        );

        d.setDeptId(dto.getDeptId());

        d.setTitle(dto.getTitle());

        d.setSkills(dto.getSkills());

        doctorMapper.insert(d);
    }

    @Override
    public List<Doctor> list() {

        return doctorMapper.selectList(null);

    }
}