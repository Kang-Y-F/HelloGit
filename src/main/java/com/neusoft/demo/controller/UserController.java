package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.dto.RegisterDTO;
import com.neusoft.demo.entity.User;
import com.neusoft.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDTO){

        User user = userService.login(loginDTO);

        if(user == null){
            return Result.fail("用户不存在");
        }

        return Result.success(user);
    }

    @PostMapping("/register")
    public Result<?> register(
            @RequestBody RegisterDTO registerDTO
    ) {

        boolean result =
                userService.register(registerDTO);

        if (!result) {

            return Result.fail("手机号已存在");
        }

        return Result.success("注册成功");
    }
}
