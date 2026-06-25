package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.demo.entity.Schedule;
import com.neusoft.demo.mapper.ScheduleMapper;
import com.neusoft.demo.service.ScheduleService;
import com.neusoft.demo.vo.ScheduleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Override
    public List<ScheduleVO> getDoctorSchedule(Long doctorId) {

        QueryWrapper<Schedule> wrapper = new QueryWrapper<>();

        wrapper.eq("doctor_id", doctorId);

        List<Schedule> scheduleList = scheduleMapper.selectList(wrapper);

        List<ScheduleVO> result = new ArrayList<>();

        for (Schedule s : scheduleList) {

            ScheduleVO vo = new ScheduleVO();

            vo.setScheduleId(s.getId());

            vo.setFullDate(s.getWorkDate().toString());

            vo.setDate(s.getWorkDate().getMonthValue()
                            + "月"
                            + s.getWorkDate().getDayOfMonth()
                            + "日"
            );

            DayOfWeek day = s.getWorkDate().getDayOfWeek();

            vo.setWeek(convertWeek(day));

            vo.setTime(s.getTimeSlot());

            vo.setFee(20.0);

            vo.setRemaining(s.getMaxNum() - s.getCurrentNum());

            result.add(vo);
        }

        return result;
    }

    private String convertWeek(
            DayOfWeek day
    ) {

        switch (day) {

            case MONDAY:
                return "周一";

            case TUESDAY:
                return "周二";

            case WEDNESDAY:
                return "周三";

            case THURSDAY:
                return "周四";

            case FRIDAY:
                return "周五";

            case SATURDAY:
                return "周六";

            default:
                return "周日";
        }
    }

    /** 查询排班列表（可按医生/日期筛选）*/
    @Override
    public List<Schedule> list(Long doctorId, String date) {
        QueryWrapper<Schedule> wrapper = new QueryWrapper<>();

        if (doctorId != null) {
            wrapper.eq("doctor_id", doctorId);
        }

        if (date != null) {
            wrapper.eq("work_date", date);
        }

        return scheduleMapper.selectList(wrapper);
    }

    /** 新增排班 */
    @Override
    public void addSchedule(Schedule schedule) {
        schedule.setCurrentNum(0);
        scheduleMapper.insert(schedule);
    }

    /** 修改最大号源 */
    @Override
    public void updateMaxNum(Long scheduleId, Integer maxNum) {
        Schedule s = new Schedule();
        s.setId(scheduleId);
        s.setMaxNum(maxNum);

        scheduleMapper.updateById(s);
    }

    /** 删除排班 */
    @Override
    public void deleteSchedule(Long scheduleId) {
        scheduleMapper.deleteById(scheduleId);
    }

    /** 按医生查询排班 */
    @Override
    public List<Schedule> getDoctorScheduleForAdmin(Long doctorId) {
        return scheduleMapper.selectList(
                new QueryWrapper<Schedule>().eq("doctor_id", doctorId)
        );
    }

    /** 补号源判断 */
    @Override
    public boolean hasQuota(Long scheduleId) {

        Schedule schedule = scheduleMapper.selectById(scheduleId);

        if (schedule == null) {
            return false;
        }

        return schedule.getCurrentNum() < schedule.getMaxNum();
    }
}