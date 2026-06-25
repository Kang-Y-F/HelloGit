package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.neusoft.demo.entity.PatientMessage;
import com.neusoft.demo.mapper.PatientMessageMapper;
import com.neusoft.demo.service.PatientMessageService;
import com.neusoft.demo.vo.PatientMessageVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PatientMessageServiceImpl implements PatientMessageService {

    @Autowired
    private PatientMessageMapper messageMapper;

    @Override
    public List<PatientMessageVO> getMyMessage(Long patientId) {
        return messageMapper.listByPatientId(patientId);
    }

    @Override
    public boolean markRead(Long msgId, Long patientId) {
        LambdaUpdateWrapper<PatientMessage> wrapper = new LambdaUpdateWrapper<>();
        // 双重校验：消息ID + 当前患者ID，防止修改别人消息
        wrapper.eq(PatientMessage::getId, msgId)
                .eq(PatientMessage::getPatientId, patientId)
                .set(PatientMessage::getReadStatus, 1);
        return messageMapper.update(null, wrapper) > 0;
    }

    @Override
    public void addMessage(PatientMessage message) {
        // 默认未读
        message.setReadStatus(0);
        messageMapper.insert(message);
    }

    @Override
    public boolean deleteMessage(Long msgId, Long patientId) {

        LambdaUpdateWrapper<PatientMessage> wrapper = new LambdaUpdateWrapper<>();

        wrapper.eq(PatientMessage::getId, msgId)
                .eq(PatientMessage::getPatientId, patientId)
                .set(PatientMessage::getReadStatus, 2); // 2=已删除

        return messageMapper.update(null, wrapper) > 0;
    }
}