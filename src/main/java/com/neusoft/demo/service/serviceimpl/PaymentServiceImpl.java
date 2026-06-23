package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.dto.PaymentDTO;
import com.neusoft.demo.dto.RefundDTO;
import com.neusoft.demo.entity.PaymentRecord;
import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.mapper.CheckOrderMapper;
import com.neusoft.demo.mapper.MedicalOrderMapper;
import com.neusoft.demo.mapper.PaymentRecordMapper;
import com.neusoft.demo.mapper.RegisterOrderMapper;
import com.neusoft.demo.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private RegisterOrderMapper registerOrderMapper;

    @Autowired
    private CheckOrderMapper checkOrderMapper;

    @Autowired
    private MedicalOrderMapper medicalOrderMapper;

    /**
     * 收费（支持挂号费和检查费）
     *
     * billType=1：挂号费
     *   - 校验 register_order 存在且未支付
     *   - 写 payment_record
     *   - 更新 register_order.pay_status=1, status=1（已挂号）
     *
     * billType=2：检查/检验费
     *   - 校验 check_order 存在且 status=0（待缴费）
     *   - 写 payment_record（order_id 填 check_order.id，order_no 填检查单标识）
     *   - 更新 check_order.status=1（已缴费/待执行）
     *   - 更新关联的 medical_order.exec_status=1（执行中）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentRecord collectPayment(PaymentDTO dto, Long operatorId) {

        int billType = dto.getBillType() == null ? 1 : dto.getBillType();

        if (billType == 2) {
            return collectCheckPayment(dto, operatorId);
        } else {
            return collectRegisterPayment(dto, operatorId);
        }
    }

    // ── 挂号费收费 ──────────────────────────────────────────────────────────
    private PaymentRecord collectRegisterPayment(PaymentDTO dto, Long operatorId) {

        RegisterOrder regOrder = registerOrderMapper.selectById(dto.getOrderId());
        if (regOrder == null) {
            throw new RuntimeException("挂号单不存在: " + dto.getOrderId());
        }
        if (regOrder.getPayStatus() != null && regOrder.getPayStatus() == 1) {
            throw new RuntimeException("该挂号单已支付，请勿重复收费");
        }

        // 写收费记录
        PaymentRecord record = buildRecord(dto, operatorId);
        record.setOrderId(regOrder.getId());
        record.setOrderNo(regOrder.getOrderNo());
        record.setPatientId(regOrder.getUserId());
        record.setBillType(1);
        paymentRecordMapper.insert(record);

        // 更新挂号单
        RegisterOrder update = new RegisterOrder();
        update.setId(regOrder.getId());
        update.setPayStatus(1);
        update.setPayMethod(dto.getPayMethod());
        update.setPayTime(LocalDateTime.now());
        update.setStatus(1); // 已挂号
        registerOrderMapper.updateById(update);

        return record;
    }

    // ── 检查/检验费收费 ──────────────────────────────────────────────────────
    private PaymentRecord collectCheckPayment(PaymentDTO dto, Long operatorId) {

        // checkOrderId 对应 check_order.id（即 medical_order.id 的关联记录）
        var checkOrder = checkOrderMapper.selectById(dto.getCheckOrderId());
        if (checkOrder == null) {
            throw new RuntimeException("检查单不存在: " + dto.getCheckOrderId());
        }
        if (checkOrder.getStatus() != 0) {
            throw new RuntimeException("该检查单状态异常，无法缴费（当前状态：" + checkOrder.getStatus() + "）");
        }

        // 查关联的 register_order（通过 medical_order → register_order_id）
        var medOrder = medicalOrderMapper.selectById(checkOrder.getOrderId());
        if (medOrder == null) {
            throw new RuntimeException("关联医嘱不存在");
        }
        var regOrder = registerOrderMapper.selectById(medOrder.getRegisterOrderId());

        // 写收费记录
        PaymentRecord record = buildRecord(dto, operatorId);
        record.setOrderId(checkOrder.getId());
        record.setOrderNo("JC" + checkOrder.getId()); // 检查单号前缀 JC
        record.setPatientId(checkOrder.getUserId());
        record.setBillType(2);
        if (regOrder != null) {
            record.setPatientName(null); // 可从 pmi_patient 查，此处留给 Mapper 层填充
        }
        paymentRecordMapper.insert(record);

        // 更新 check_order.status = 1（已缴费/待执行）
        checkOrderMapper.updateStatus(checkOrder.getId(), 1);

        // 更新 medical_order.exec_status = 1（执行中）
        medicalOrderMapper.updateExecStatus(medOrder.getId(), 1);

        return record;
    }

    // ── 退款 ──────────────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(RefundDTO dto, Long operatorId) {

        RegisterOrder regOrder = registerOrderMapper.selectById(dto.getOrderId());
        if (regOrder == null) throw new RuntimeException("挂号单不存在");

        // 更新挂号单状态为已取消
        RegisterOrder update = new RegisterOrder();
        update.setId(regOrder.getId());
        update.setStatus(2); // 已取消
        if (regOrder.getPayStatus() != null && regOrder.getPayStatus() == 1) {
            update.setPayStatus(2); // 已退款
        }
        registerOrderMapper.updateById(update);

        // 更新收费记录状态
        paymentRecordMapper.updateRefundByOrderId(
            regOrder.getId(), dto.getReason(), LocalDateTime.now()
        );

        return true;
    }

    // ── 收费记录查询 ──────────────────────────────────────────────────────────
    @Override
    public List<PaymentRecord> listPayments(Map<String, Object> filter) {
        return paymentRecordMapper.selectByFilter(filter);
    }

    // ── 今日统计 ──────────────────────────────────────────────────────────────
    @Override
    public Map<String, Object> todayStats(Long operatorId) {
        return paymentRecordMapper.todayStats(operatorId);
    }

    // ── 工具方法：构建收费记录基础字段 ────────────────────────────────────────
    private PaymentRecord buildRecord(PaymentDTO dto, Long operatorId) {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo(generatePaymentNo());
        record.setPayMethod(dto.getPayMethod());
        record.setAmount(dto.getAmount());
        record.setReceived(dto.getReceived() != null ? dto.getReceived() : dto.getAmount());
        BigDecimal change = record.getReceived().subtract(record.getAmount());
        record.setChangeAmount(change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO);
        record.setPayChannel(dto.getPayChannel());
        record.setTransactionId(dto.getTransactionId());
        record.setRemark(dto.getRemark());
        record.setOperatorId(operatorId);
        record.setPayStatus(1); // 1=已支付
        record.setPayTime(LocalDateTime.now());
        return record;
    }

    private String generatePaymentNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = new Random().nextInt(9000) + 1000;
        return "PAY" + ts + rand;
    }
}
