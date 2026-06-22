package com.neusoft.demo.service;

import com.neusoft.demo.entity.PatientMessage;
import com.neusoft.demo.vo.PatientMessageVO;
import java.util.List;

public interface PatientMessageService {
    // 查询我的全部消息
    List<PatientMessageVO> getMyMessage(Long patientId);

    // 标记消息为已读（校验只能操作自己的消息）
    boolean markRead(Long msgId, Long patientId);

    // 新增站内消息（业务调用：挂号成功、AI导诊完成等场景）
    void addMessage(PatientMessage message);
}