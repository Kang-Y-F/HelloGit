package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.Schedule;
import com.neusoft.demo.mapper.ScheduleMapper;
import com.neusoft.demo.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleMapper scheduleMapper;

    /**
     * 查询医生未来30天排班
     */
    @GetMapping("/my")
    public Result<?> mySchedule(HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        Long doctorId = claims.get("userId", Long.class);

        List<Schedule> list = scheduleMapper.selectList(
                new LambdaQueryWrapper<Schedule>()
                        .eq(Schedule::getDoctorId, doctorId)
                        .ge(Schedule::getWorkDate, LocalDate.now().minusDays(7))
                        .le(Schedule::getWorkDate, LocalDate.now().plusDays(30))
                        .orderByAsc(Schedule::getWorkDate)
        );
        return Result.success(list);
    }
}
