package com.neusoft.demo.controller;

import com.neusoft.demo.service.RegisterMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/register-monitor")
public class RegisterMonitorController {

    @Autowired
    private RegisterMonitorService registerMonitorService;

    /** 今日挂号量 */
    @GetMapping("/today")
    public Long today() {
        return registerMonitorService.todayCount();
    }

    /** 科室排行 */
    @GetMapping("/dept-rank")
    public List<Map<String, Object>> deptRank() {
        return registerMonitorService.deptRank();
    }

    /** 医生负载 */
    @GetMapping("/doctor-load")
    public List<Map<String, Object>> doctorLoad() {
        return registerMonitorService.doctorLoad();
    }
}
