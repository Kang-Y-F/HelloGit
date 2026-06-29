package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.neusoft.demo.dto.RegisterCreateDTO;
import com.neusoft.demo.entity.PmiPatient;
import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.entity.Schedule;
import com.neusoft.demo.mapper.PmiPatientMapper;
import com.neusoft.demo.mapper.RegisterOrderMapper;
import com.neusoft.demo.mapper.ScheduleMapper;
import com.neusoft.demo.service.RegisterDeskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class RegisterDeskServiceImpl implements RegisterDeskService {

    @Autowired private RegisterOrderMapper registerOrderMapper;
    @Autowired private ScheduleMapper      scheduleMapper;
    @Autowired private PmiPatientMapper    pmiPatientMapper;

    @Override
    @Transactional
    public RegisterOrder createRegisterOrder(RegisterCreateDTO dto) {
        if (dto.getPatientId() == null)  throw new RuntimeException("请选择患者");
        if (dto.getDoctorId()  == null)  throw new RuntimeException("请选择医生");
        if (dto.getScheduleId() == null) throw new RuntimeException("请选择时段");

        PmiPatient patient = pmiPatientMapper.selectById(dto.getPatientId());
        if (patient == null) throw new RuntimeException("患者不存在");

        Schedule schedule = scheduleMapper.selectById(dto.getScheduleId());
        if (schedule == null) throw new RuntimeException("排班不存在");
        if (!schedule.getDoctorId().equals(dto.getDoctorId()))
            throw new RuntimeException("该时段与所选医生不匹配");
        if (schedule.getCurrentNum() >= schedule.getMaxNum())
            throw new RuntimeException("该时段已约满");

        // 乐观锁更新名额
        int affected = scheduleMapper.update(null,
                new LambdaUpdateWrapper<Schedule>()
                        .eq(Schedule::getId, dto.getScheduleId())
                        .eq(Schedule::getCurrentNum, schedule.getCurrentNum())
                        .set(Schedule::getCurrentNum, schedule.getCurrentNum() + 1)
        );
        if (affected == 0) throw new RuntimeException("挂号失败，请重试");

        // 生成挂号单
        RegisterOrder order = new RegisterOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(dto.getPatientId());
        order.setDoctorId(dto.getDoctorId());
        order.setScheduleId(dto.getScheduleId());
        order.setStatus(0);  // 0 = 待支付（挂号员代办，等收费后改 1）
        order.setPrice(dto.getPrice() != null
                ? java.math.BigDecimal.valueOf(dto.getPrice())
                : new java.math.BigDecimal("15.00"));
        order.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        order.setSource("offline");  // 标记为线下挂号（挂号员）
        order.setCreateTime(LocalDateTime.now());
        registerOrderMapper.insert(order);

        return order;
    }

    private String generateOrderNo() {
        String dt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = (int) (Math.random() * 1000);
        return "GH" + dt + String.format("%03d", rand);
    }
}
