package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.Schedule;
import com.neusoft.demo.mapper.ScheduleMapper;
import com.neusoft.demo.service.ScheduleService;
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

    @Autowired
    private ScheduleService scheduleService;

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

    @GetMapping("/list")
    public List<Schedule> scheduleList(@RequestParam(required = false) Long doctorId,
                                       @RequestParam(required = false) String date) {
        return scheduleService.list(doctorId, date);
    }

    @PostMapping("/add")
    public String addSchedule(@RequestBody Schedule schedule) {
        scheduleService.addSchedule(schedule);
        return "添加成功";
    }

    @PutMapping("/max/{id}")
    public String updateMaxNum(@PathVariable Long id,
                               @RequestParam Integer maxNum) {

        scheduleService.updateMaxNum(id, maxNum);
        return "更新成功";
    }

    @DeleteMapping("/delete/{id}")
    public String deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return "删除成功";
    }

    @GetMapping("/doctor/{doctorId}")
    public List<Schedule> getDoctorScheduleForAdmin(@PathVariable Long doctorId) {
        return scheduleService.getDoctorScheduleForAdmin(doctorId);
    }

    /** 判断排班是否还有号源（辅助功能，后续不用的话可以删） */
    @GetMapping("/quota/{id}")
    public boolean hasQuota(@PathVariable Long id) {
        return scheduleService.hasQuota(id);
    }
}

