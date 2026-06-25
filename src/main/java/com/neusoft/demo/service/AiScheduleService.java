package com.neusoft.demo.service;

import com.neusoft.demo.entity.AiSchedulePlan;

import java.util.List;

public interface AiScheduleService {
    /**
     * AI生成排班建议
     */
    List<AiSchedulePlan> generatePlan();

    /**
     * 查询AI排班建议
     */
    List<AiSchedulePlan> list();

    /**
     * 通过AI排班
     */
    void approve(Long id);

    /**
     * 驳回AI排班
     */
    void reject(Long id);

    /**
     * 写入正式排班表
     */
    void commitToSchedule();
}
