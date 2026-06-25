package com.neusoft.demo.service;

import java.util.List;
import java.util.Map;

public interface RegisterMonitorService {
    /** 今日挂号总量 */
    Long todayCount();

    /** 科室挂号排行 */
    List<Map<String, Object>> deptRank();

    /** 医生负载统计 */
    List<Map<String, Object>> doctorLoad();
}
