package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.dto.AdminAddDTO;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.entity.Admin;
import com.neusoft.demo.mapper.AdminMapper;
import com.neusoft.demo.service.AdminService;
import com.neusoft.demo.utils.JwtUtil;
import com.neusoft.demo.utils.PasswordUtil;
import com.neusoft.demo.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public LoginVO login(LoginDTO loginDTO) {

        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>()
                        .eq(
                                Admin::getUsername,
                                loginDTO.getUsername()
                        )
        );

        // 用户不存在
        if (admin == null) {
            return null;
        }

        // 密码校验
        boolean match = PasswordUtil.matches(
                loginDTO.getPassword(),
                admin.getPassword()
        );

        if (!match) {
            return null;
        }

        String token = JwtUtil.createToken(
                admin.getId(),
                "ADMIN"
        );

        return new LoginVO(
                admin.getId(),
                admin.getName(),
                "ADMIN",
                token
        );
    }

    @Override
    public void addAdmin(AdminAddDTO dto) {

        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getUsername,
                                dto.getUsername())
        );

        if(admin != null){
            throw new RuntimeException("管理员已存在");
        }

        Admin a = new Admin();

        a.setUsername(dto.getUsername());

        a.setName(dto.getName());

        a.setRole("admin");

        a.setPassword(
                PasswordUtil.encode(
                        dto.getPassword()
                )
        );

        adminMapper.insert(a);
    }
}