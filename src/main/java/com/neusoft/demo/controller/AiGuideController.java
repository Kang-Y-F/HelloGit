package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.GuideAnalyzeDTO;
import com.neusoft.demo.service.AiGuideService;
import com.neusoft.demo.vo.GuideAnalyzeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/ai/guide")
public class AiGuideController {

    @Autowired
    private AiGuideService aiGuideService;

    /**
     * P0 智能导诊AI分析
     * POST /ai/guide/analyze
     */
    @PostMapping("/analyze")
    public Result<GuideAnalyzeVO> analyze(@RequestBody GuideAnalyzeDTO dto, HttpServletRequest request) {
        // 简单登录校验（仅鉴权，ID不用传业务）
        if (request.getAttribute("userId") == null) {
            return Result.fail("请先登录");
        }
        // 取出登录患者ID传给service
        Long patientId = Long.valueOf(request.getAttribute("userId").toString());
        GuideAnalyzeVO vo = aiGuideService.analyzeSymptoms(dto, patientId);
        return Result.success(vo);
    }
}