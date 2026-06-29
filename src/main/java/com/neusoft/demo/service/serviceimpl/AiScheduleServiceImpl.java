package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.demo.dto.AiScheduleDTO;
import com.neusoft.demo.entity.AiSchedulePlan;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.entity.Schedule;
import com.neusoft.demo.mapper.AiSchedulePlanMapper;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.mapper.RegisterOrderMapper;
import com.neusoft.demo.mapper.ScheduleMapper;
import com.neusoft.demo.service.AiScheduleService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiScheduleServiceImpl implements AiScheduleService {
    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private RegisterOrderMapper registerOrderMapper;

    @Autowired
    private AiSchedulePlanMapper aiSchedulePlanMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private ChatClient chatClient;

    @Override
    public List<AiSchedulePlan> generatePlan() {

        List<Doctor> doctors = doctorMapper.selectList(null);

        StringBuilder doctorData = new StringBuilder();

        for (Doctor d : doctors) {

            Long todayLoad = registerOrderMapper.selectCount(
                    new QueryWrapper<RegisterOrder>()
                            .eq("doctor_id", d.getId())
                            .apply("DATE(create_time)=CURDATE()")
            );

            doctorData.append(
                    "医生ID：" + d.getId() +
                            "，医生姓名：" + d.getName() +
                            "，职称：" + d.getTitle() +
                            "，科室ID：" + d.getDeptId() +
                            "，今日接诊：" + todayLoad +
                            "\n"
            );
        }

        // ===== Prompt（改成医生端同款风格）=====
        String prompt =
                "你是医院排班AI系统，请根据医生负载生成排班建议。\n" +
                        "规则：\n" +
                        "1. 接诊多的医生减少排班\n" +
                        "2. 主任医师优先上午\n" +
                        "3. 必须基于医生ID进行排班\n\n" +
                        "输出格式必须严格如下（不要JSON）：\n" +
                        "医生ID：xxx\n" +
                        "时间段：上午/下午\n" +
                        "建议负载：xxx\n" +
                        "原因：xxx\n" +
                        "---（每个医生一段，用---分隔）\n\n" +
                        "医生数据如下：\n" +
                        doctorData;

        // ===== 调用AI（ChatClient标准写法）=====
        String aiResult = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // ===== 解析AI返回（不用JSON，避免报错）=====
        List<AiSchedulePlan> result = new ArrayList<>();

        String[] blocks = aiResult.split("---");

        for (String block : blocks) {

            if (block == null || block.isBlank()) continue;

            Long doctorId = null;
            String timeSlot = null;
            Integer expectedLoad = null;

            for (String line : block.split("\n")) {

                line = line.trim();

                if (line.startsWith("医生ID")) {
                    doctorId = Long.valueOf(line.replaceAll("\\D+", ""));
                }

                if (line.startsWith("时间段")) {
                    timeSlot = line.replace("时间段：", "").trim();
                }
                if (line.startsWith("建议负载")) {

                    expectedLoad = Integer.valueOf(line.replaceAll("\\D+", ""));
                }
            }

            if (doctorId == null) continue;

            Doctor doctor = doctorMapper.selectById(doctorId);
            if (doctor == null) continue;

            AiSchedulePlan plan = new AiSchedulePlan();
            plan.setDoctorId(doctorId);
            plan.setPlanDate(LocalDate.now());
            plan.setTimeSlot(timeSlot);
            plan.setStatus(0);
            plan.setCreateTime(LocalDateTime.now());
            plan.setExpectedLoad(expectedLoad);

            aiSchedulePlanMapper.insert(plan);
            result.add(plan);
        }

        return result;
    }

    @Override
    public List<AiSchedulePlan> list() {
        return aiSchedulePlanMapper.selectList(null);
    }

    @Override
    public void approve(Long id) {
        AiSchedulePlan plan = aiSchedulePlanMapper.selectById(id);
        plan.setStatus(1);
        aiSchedulePlanMapper.updateById(plan);
    }

    @Override
    public void reject(Long id) {
        AiSchedulePlan plan = aiSchedulePlanMapper.selectById(id);
        plan.setStatus(2);
        aiSchedulePlanMapper.updateById(plan);
    }

    @Override
    public void commitToSchedule() {

        List<AiSchedulePlan> list = aiSchedulePlanMapper.selectList(
                new QueryWrapper<AiSchedulePlan>().eq("status", 1)
        );

        for (AiSchedulePlan plan : list) {

            Schedule schedule = new Schedule();
            schedule.setDoctorId(plan.getDoctorId());
            schedule.setWorkDate(plan.getPlanDate());

            schedule.setTimeSlot(plan.getTimeSlot());

            schedule.setMaxNum(20);
            schedule.setCurrentNum(0);

            scheduleMapper.insert(schedule);

            // 标记已写入
            plan.setStatus(1);
            aiSchedulePlanMapper.updateById(plan);
        }
    }
}
