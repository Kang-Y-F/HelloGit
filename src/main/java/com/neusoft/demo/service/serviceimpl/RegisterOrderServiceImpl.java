package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.entity.*;
import com.neusoft.demo.mapper.*;
import com.neusoft.demo.service.RegisterOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.neusoft.demo.service.PatientMessageService;
import com.neusoft.demo.vo.RegisterOrderVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RegisterOrderServiceImpl implements RegisterOrderService {

    @Autowired
    private RegisterOrderMapper registerOrderMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private PatientMessageService messageService;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private PmiPatientMapper patientMapper;

    @Autowired
    private DepartmentMapper departmentMapper;


    @Override
    public RegisterOrder getById(Long id) {
        return registerOrderMapper.selectById(id);
    }

    @Override
    public List<RegisterOrderVO> listByPatient(Long patientId) {

        return registerOrderMapper.listPatientOrders(patientId);

    }

    @Override
    public String addRegisterOrder(
            Long userId,
            Long doctorId,
            Long scheduleId,
            Integer priority
    ) {

        Schedule schedule =
                scheduleMapper.selectById(scheduleId);

        if(schedule == null){
            return "排班不存在";
        }

        if(schedule.getCurrentNum()
                >= schedule.getMaxNum()){

            return "号源已满";
        }

        RegisterOrder order =
                new RegisterOrder();

        order.setOrderNo(
                UUID.randomUUID()
                        .toString()
                        .replace("-","")
        );

        order.setUserId(userId);

        order.setDoctorId(doctorId);

        order.setScheduleId(scheduleId);

        order.setPriority(priority);

        order.setStatus(1);

        order.setPrice(
                new BigDecimal("20")
        );

        order.setCreateTime(
                LocalDateTime.now()
        );

        registerOrderMapper.insert(order);

        schedule.setCurrentNum(
                schedule.getCurrentNum()+1
        );

        scheduleMapper.updateById(schedule);

        // ============ 新增：挂号成功生成站内消息 ============
        Doctor doctor = doctorMapper.selectById(doctorId);
        String doctorName = doctor.getName();
        // 你Schedule实体里存就诊时段的字段是time，直接取
        String time = schedule.getTimeSlot();

        PatientMessage msg = new PatientMessage();
        msg.setPatientId(userId);
        msg.setTitle("挂号成功通知");
        msg.setContent("您已成功预约" + doctorName + "，就诊时段：" + time);
        msg.setMsgType(1);
        msg.setJumpPath("pages/my-orders/my-orders");
        messageService.addMessage(msg);

        return "挂号成功";
    }

    @Override
    public String cancelOrder(Long userId, Long orderId) {

        RegisterOrder order =
                registerOrderMapper.selectById(orderId);

        if (order == null) {
            return "挂号单不存在";
        }

        // 🔒 必须校验是不是自己的订单
        if (!order.getUserId().equals(userId)) {
            return "无权限操作";
        }

        // 已取消的不重复操作
        if (order.getStatus() == 2) {
            return "已取消";
        }

        order.setStatus(2); // 2=取消
        registerOrderMapper.updateById(order);

        return "取消成功";
    }

    @Override
    public RegisterOrderVO patientDetail(Long id) {

        return registerOrderMapper.getPatientDetail(id);

    }
}
