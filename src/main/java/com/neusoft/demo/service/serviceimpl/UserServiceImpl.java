package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.dto.RegisterDTO;
import com.neusoft.demo.entity.User;
import com.neusoft.demo.mapper.UserMapper;
import com.neusoft.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(LoginDTO loginDTO) {

        LambdaQueryWrapper<User> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(
                User::getPhone,
                loginDTO.getPhone()
        );

        return userMapper.selectOne(wrapper);
    }

    @Override
    public boolean register(RegisterDTO registerDTO) {

        LambdaQueryWrapper<User> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(
                User::getPhone,
                registerDTO.getPhone()
        );

        User existUser =
                userMapper.selectOne(wrapper);

        if (existUser != null) {
            return false;
        }

        User user = new User();

        user.setPhone(registerDTO.getPhone());
        user.setName(registerDTO.getName());
        user.setIdCard(registerDTO.getIdCard());
        user.setCreateTime(LocalDateTime.now());

        return userMapper.insert(user) > 0;
    }
}