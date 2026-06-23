package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.entity.CheckOrder;
import com.neusoft.demo.mapper.CheckOrderMapper;
import com.neusoft.demo.mapper.MedicalOrderMapper;
import com.neusoft.demo.service.CheckOrderService;
import com.neusoft.demo.vo.CheckOrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CheckOrderServiceImpl implements CheckOrderService {

    @Autowired
    private CheckOrderMapper checkOrderMapper;

    @Autowired
    private MedicalOrderMapper medicalOrderMapper;

    /**
     * 待缴费检查单（status=0）
     * Mapper 层需做联表查询，返回患者姓名、检查项目名、医生名
     */
    @Override
    public List<CheckOrderVO> listPendingPayment(Long patientId, String keyword) {
        return checkOrderMapper.selectPendingPayment(patientId, keyword);
    }

    /**
     * 状态流转规则：
     *   0(待缴费)        → 仅可 → 2(已取消)
     *   1(已缴费/待执行) → 可  → 2(已取消) 或 3(执行中)
     *   3(执行中)        → 仅可 → 4(已完成)
     *   2/4              → 终态，不可再变更
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long checkOrderId, Integer newStatus) {
        CheckOrder checkOrder = checkOrderMapper.selectById(checkOrderId);
        if (checkOrder == null) {
            throw new RuntimeException("检查单不存在：" + checkOrderId);
        }

        int cur = checkOrder.getStatus();
        validateTransition(cur, newStatus);

        checkOrderMapper.updateStatus(checkOrderId, newStatus);

        // 同步更新关联的 medical_order.exec_status
        if (checkOrder.getOrderId() != null) {
            int execStatus = toExecStatus(newStatus);
            medicalOrderMapper.updateExecStatus(checkOrder.getOrderId(), execStatus);
        }
    }

    /** 按挂号单查所有检查单（通过 medical_order.register_order_id 关联） */
    @Override
    public List<CheckOrderVO> listByRegisterOrder(Long registerOrderId) {
        return checkOrderMapper.selectByRegisterOrderId(registerOrderId);
    }

    // ── 私有方法 ──────────────────────────────────────────────────────────────

    private void validateTransition(int cur, int next) {
        boolean valid = switch (cur) {
            case 0 -> next == 2;                  // 待缴费 → 取消
            case 1 -> next == 2 || next == 3;     // 已缴费 → 取消 或 执行中
            case 3 -> next == 4;                  // 执行中 → 已完成
            default -> false;                     // 2(已取消)/4(已完成) 为终态
        };
        if (!valid) {
            throw new RuntimeException(
                String.format("非法状态流转：%s → %s", statusName(cur), statusName(next))
            );
        }
    }

    /**
     * check_order.status → medical_order.exec_status 映射
     *   check 1(已缴费/待执行) → exec 0(待执行)
     *   check 2(已取消)        → exec 3(已作废)
     *   check 3(执行中)        → exec 1(执行中)
     *   check 4(已完成)        → exec 2(已完成)
     */
    private int toExecStatus(int checkStatus) {
        return switch (checkStatus) {
            case 1 -> 0;
            case 2 -> 3;
            case 3 -> 1;
            case 4 -> 2;
            default -> 0;
        };
    }

    private String statusName(int s) {
        return switch (s) {
            case 0 -> "待缴费";
            case 1 -> "已缴费/待执行";
            case 2 -> "已取消";
            case 3 -> "执行中";
            case 4 -> "已完成";
            default -> "未知(" + s + ")";
        };
    }
}
