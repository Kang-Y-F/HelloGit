package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.neusoft.demo.dto.PaymentDTO;
import com.neusoft.demo.dto.RefundDTO;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.entity.PaymentRecord;
import com.neusoft.demo.entity.PmiPatient;
import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.entity.Schedule;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.mapper.PaymentRecordMapper;
import com.neusoft.demo.mapper.PmiPatientMapper;
import com.neusoft.demo.mapper.RegisterOrderMapper;
import com.neusoft.demo.mapper.ScheduleMapper;
import com.neusoft.demo.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired private PaymentRecordMapper  paymentRecordMapper;
    @Autowired private RegisterOrderMapper  registerOrderMapper;
    @Autowired private PmiPatientMapper     pmiPatientMapper;
    @Autowired private DoctorMapper         doctorMapper;
    @Autowired private ScheduleMapper       scheduleMapper;

    @Override
    @Transactional
    public PaymentRecord collectPayment(PaymentDTO dto, Long operatorId) {
        // 1. 校验挂号单
        RegisterOrder order = registerOrderMapper.selectById(dto.getOrderId());
        if (order == null) throw new RuntimeException("挂号单不存在");
        if (order.getPayStatus() != null && order.getPayStatus() == 1)
            throw new RuntimeException("该挂号单已支付，请勿重复收费");
        if (order.getStatus() != 0)
            throw new RuntimeException("订单状态异常，无法收费");

        // 2. 校验金额
        if (dto.getReceived() == null || dto.getAmount() == null)
            throw new RuntimeException("金额参数缺失");
        if (dto.getReceived().compareTo(dto.getAmount()) < 0)
            throw new RuntimeException("实收金额不足");

        // 3. 校验支付方式
        Integer method = dto.getPayMethod();
        if (method == null || method < 1 || method > 4)
            throw new RuntimeException("支付方式无效");

        // 扫码 / 银行卡 必须有交易号
        if ((method == 2 || method == 4) &&
            (dto.getTransactionId() == null || dto.getTransactionId().isBlank()))
            throw new RuntimeException("电子支付必须提供交易号");

        // 4. 计算找零（现金）
        BigDecimal change = method == 1 
            ? dto.getReceived().subtract(dto.getAmount())
            : BigDecimal.ZERO;

        // 5. 查询关联信息（冗余字段）
        PmiPatient patient = pmiPatientMapper.selectById(order.getUserId());
        Doctor operator    = doctorMapper.selectById(operatorId);

        // 6. 写收费记录
        PaymentRecord rec = new PaymentRecord();
        rec.setPaymentNo(generatePaymentNo());
        rec.setOrderId(order.getId());
        rec.setOrderNo(order.getOrderNo());
        rec.setPatientId(order.getUserId());
        rec.setPatientName(patient != null ? patient.getName() : null);
        rec.setAmount(dto.getAmount());
        rec.setReceived(dto.getReceived());
        rec.setChangeAmount(change);
        rec.setPayMethod(method);
        rec.setPayChannel(dto.getPayChannel());
        rec.setTransactionId(dto.getTransactionId());
        rec.setOperatorId(operatorId);
        rec.setOperatorName(operator != null ? operator.getName() : null);
        rec.setPayStatus(1);
        rec.setPayTime(LocalDateTime.now());
        rec.setRemark(dto.getRemark());
        paymentRecordMapper.insert(rec);

        // 7. 更新挂号单：支付状态 + 订单状态变为已挂号
        registerOrderMapper.update(null,
                new LambdaUpdateWrapper<RegisterOrder>()
                        .eq(RegisterOrder::getId, order.getId())
                        .set(RegisterOrder::getStatus,    1)             // 1 = 已挂号
                        .set(RegisterOrder::getPayStatus, 1)             // 已支付
                        .set(RegisterOrder::getPayMethod, method)
                        .set(RegisterOrder::getPayTime,   LocalDateTime.now())
        );

        return rec;
    }

    @Override
    @Transactional
    public boolean refund(RefundDTO dto, Long operatorId) {
        RegisterOrder order = registerOrderMapper.selectById(dto.getOrderId());
        if (order == null) throw new RuntimeException("挂号单不存在");

        // 只有待支付/已挂号才能取消（已就诊或已完成的不允许）
        if (order.getStatus() != 0 && order.getStatus() != 1)
            throw new RuntimeException("当前状态不允许取消");

        // 查最近一笔收费记录
        PaymentRecord lastPay = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderId, dto.getOrderId())
                        .eq(PaymentRecord::getPayStatus, 1)
                        .orderByDesc(PaymentRecord::getPayTime)
                        .last("LIMIT 1")
        );

        // 1. 如果已支付，做退款
        if (lastPay != null) {
            paymentRecordMapper.update(null,
                    new LambdaUpdateWrapper<PaymentRecord>()
                            .eq(PaymentRecord::getId, lastPay.getId())
                            .set(PaymentRecord::getPayStatus,   2)
                            .set(PaymentRecord::getRefundTime,  LocalDateTime.now())
                            .set(PaymentRecord::getRefundReason, dto.getReason())
            );
        }

        // 2. 更新挂号单状态 2 = 已取消
        registerOrderMapper.update(null,
                new LambdaUpdateWrapper<RegisterOrder>()
                        .eq(RegisterOrder::getId, order.getId())
                        .set(RegisterOrder::getStatus,    2)
                        .set(RegisterOrder::getPayStatus, lastPay != null ? 2 : 0)
        );

        // 3. 释放排班名额
        if (order.getScheduleId() != null) {
            Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
            if (schedule != null && schedule.getCurrentNum() != null && schedule.getCurrentNum() > 0) {
                scheduleMapper.update(null,
                        new LambdaUpdateWrapper<Schedule>()
                                .eq(Schedule::getId, order.getScheduleId())
                                .set(Schedule::getCurrentNum, schedule.getCurrentNum() - 1)
                );
            }
        }

        return true;
    }

    @Override
    public List<PaymentRecord> listPayments(Map<String, Object> filter) {
        LambdaQueryWrapper<PaymentRecord> w = new LambdaQueryWrapper<>();

        if (filter.get("orderId")    != null) w.eq(PaymentRecord::getOrderId,    filter.get("orderId"));
        if (filter.get("patientId")  != null) w.eq(PaymentRecord::getPatientId,  filter.get("patientId"));
        if (filter.get("operatorId") != null) w.eq(PaymentRecord::getOperatorId, filter.get("operatorId"));
        if (filter.get("payMethod")  != null) w.eq(PaymentRecord::getPayMethod,  filter.get("payMethod"));
        if (filter.get("payStatus")  != null) w.eq(PaymentRecord::getPayStatus,  filter.get("payStatus"));
        if (filter.get("keyword")    != null) {
            String kw = (String) filter.get("keyword");
            w.and(q -> q.like(PaymentRecord::getPaymentNo, kw)
                    .or().like(PaymentRecord::getOrderNo,     kw)
                    .or().like(PaymentRecord::getPatientName, kw));
        }

        w.orderByDesc(PaymentRecord::getPayTime).last("LIMIT 200");
        return paymentRecordMapper.selectList(w);
    }

    @Override
    public Map<String, Object> todayStats(Long operatorId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<PaymentRecord> w = new LambdaQueryWrapper<PaymentRecord>()
                .ge(PaymentRecord::getPayTime, today.atStartOfDay())
                .lt(PaymentRecord::getPayTime, today.plusDays(1).atStartOfDay())
                .eq(PaymentRecord::getPayStatus, 1);
        if (operatorId != null) w.eq(PaymentRecord::getOperatorId, operatorId);

        List<PaymentRecord> list = paymentRecordMapper.selectList(w);

        BigDecimal total = list.stream()
                .map(PaymentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long countCash    = list.stream().filter(r -> r.getPayMethod() == 1).count();
        long countQr      = list.stream().filter(r -> r.getPayMethod() == 2).count();
        long countMedical = list.stream().filter(r -> r.getPayMethod() == 3).count();
        long countCard    = list.stream().filter(r -> r.getPayMethod() == 4).count();

        BigDecimal sumCash    = sumBy(list, 1);
        BigDecimal sumQr      = sumBy(list, 2);
        BigDecimal sumMedical = sumBy(list, 3);
        BigDecimal sumCard    = sumBy(list, 4);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount",   list.size());
        result.put("totalAmount",  total);
        result.put("cashCount",    countCash);
        result.put("cashAmount",   sumCash);
        result.put("qrCount",      countQr);
        result.put("qrAmount",     sumQr);
        result.put("medicalCount", countMedical);
        result.put("medicalAmount", sumMedical);
        result.put("cardCount",    countCard);
        result.put("cardAmount",   sumCard);
        return result;
    }

    // ── 工具 ─────────────────────────────────────────────

    private String generatePaymentNo() {
        String dt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = (int) (Math.random() * 10000);
        return "PAY" + dt + String.format("%04d", rand);
    }

    private BigDecimal sumBy(List<PaymentRecord> list, int method) {
        return list.stream()
                .filter(r -> r.getPayMethod() != null && r.getPayMethod() == method)
                .map(PaymentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
