package com.neusoft.demo.controller;

import com.neusoft.demo.entity.AiSchedulePlan;
import com.neusoft.demo.service.AiScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/schedule-ai")
public class AiScheduleController {

    @Autowired
    private AiScheduleService aiScheduleService;

    /**
     * AI生成排班
     */
    @PostMapping("/generate")
    public List<AiSchedulePlan> generate() {
        return aiScheduleService.generatePlan();
    }

    /**
     * 查询AI排班
     */
    @GetMapping("/list")
    public List<AiSchedulePlan> list() {
        return aiScheduleService.list();
    }

    /**
     * 通过AI排班
     */
    @PutMapping("/approve/{id}")
    public String approve(@PathVariable Long id) {
        aiScheduleService.approve(id);
        return "已通过";
    }

    /**
     * 驳回AI排班
     */
    @PutMapping("/reject/{id}")
    public String reject(@PathVariable Long id) {
        aiScheduleService.reject(id);
        return "已驳回";
    }

    /**
     * 写入正式排班
     */
    @PostMapping("/commit")
    public String commit() {
        aiScheduleService.commitToSchedule();
        return "已生效";
    }
}