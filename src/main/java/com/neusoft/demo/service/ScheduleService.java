package com.neusoft.demo.service;

import com.neusoft.demo.vo.ScheduleVO;

import java.util.List;

public interface ScheduleService {

    List<ScheduleVO> getDoctorSchedule(
            Long doctorId
    );

}