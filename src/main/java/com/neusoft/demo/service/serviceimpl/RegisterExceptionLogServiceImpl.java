package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.entity.RegisterExceptionLog;
import com.neusoft.demo.mapper.RegisterExceptionLogMapper;
import com.neusoft.demo.service.RegisterExceptionLogService;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RegisterExceptionLogServiceImpl implements RegisterExceptionLogService {

    @Autowired
    private RegisterExceptionLogMapper mapper;

    /**
     * 保存异常记录
     */
    @Override
    public void saveException(RegisterExceptionLog log) {
        mapper.insert(log);
    }

    /**
     * 查询异常记录
     */
    @Override
    public List<RegisterExceptionLog> list() {
        return mapper.selectList(null);
    }
}