package com.neusoft.demo.service;

import com.neusoft.demo.entity.Schedule;
import com.neusoft.demo.vo.ScheduleVO;

import java.util.List;

public interface ScheduleService {

    /** 患者端：获取医生排班信息 */
    List<ScheduleVO> getDoctorSchedule(Long doctorId);

    /** 查询排班列表（支持医生/日期筛选）*/
    List<Schedule> list(Long doctorId, String date);

    /** 新增排班 */
    void addSchedule(Schedule schedule);

    /** 修改排班号源（最大接诊人数）*/
    void updateMaxNum(Long scheduleId, Integer maxNum);

    /** 删除排班 */
    void deleteSchedule(Long scheduleId);

    /** 管理员端：按医生查询排班 */
    List<Schedule> getDoctorScheduleForAdmin(Long doctorId);

    /** 补号源判断 */
    boolean hasQuota(Long scheduleId);

}