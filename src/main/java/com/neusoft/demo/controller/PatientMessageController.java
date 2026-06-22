package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.service.PatientMessageService;
import com.neusoft.demo.vo.PatientMessageVO;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/message")
public class PatientMessageController {

    @Autowired
    private PatientMessageService messageService;

    /**
     * GET /message/my-list
     * 查询我的站内消息列表
     */
    @GetMapping("/my-list")
    public Result<List<PatientMessageVO>> myMessageList(HttpServletRequest request) {
        Long patientId = Long.valueOf(request.getAttribute("userId").toString());
        List<PatientMessageVO> list = messageService.getMyMessage(patientId);
        return Result.success(list);
    }

    /**
     * PUT /message/read/{msgId}
     * 将指定消息标记为已读
     */
    @PutMapping("/read/{msgId}")
    public Result<String> readMsg(@PathVariable Long msgId, HttpServletRequest request) {
        Long patientId = Long.valueOf(request.getAttribute("userId").toString());
        boolean ok = messageService.markRead(msgId, patientId);
        if (ok) {
            return Result.success("标记已读成功");
        }
        return Result.fail("操作失败，无此消息或无权操作");
    }
}