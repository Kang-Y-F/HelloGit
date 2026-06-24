package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.MedicalOrderDTO;
import com.neusoft.demo.mapper.MedicalOrderMapper;
import com.neusoft.demo.service.MedicalOrderService;
import com.neusoft.demo.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medical-order")
public class MedicalOrderController {

    @Autowired
    private MedicalOrderService medicalOrderService;

    @Autowired
    private MedicalOrderMapper medicalOrderMapper;

    /** 开医嘱（含处方） */
    @PostMapping("/create")
    public Result<?> create(@RequestBody MedicalOrderDTO dto, HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        Long doctorId = claims.get("userId", Long.class);
        Long orderId = medicalOrderService.create(doctorId, dto);
        return Result.success(orderId);
    }

    /** 查询挂号单下所有医嘱（带项目名和价格） */
    @GetMapping("/list/{registerOrderId}")
    public Result<?> list(@PathVariable Long registerOrderId) {
        return Result.success(
            medicalOrderMapper.selectByRegisterOrderIdWithItem(registerOrderId)
        );
    }
}
