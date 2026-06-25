package com.neusoft.demo.service;

import com.neusoft.demo.entity.RegisterExceptionLog;

import java.util.List;

public interface RegisterExceptionLogService {

    /** 记录挂号异常 */
    void saveException(RegisterExceptionLog log);

    /** 查询异常列表 */
    List<RegisterExceptionLog> list();
}
