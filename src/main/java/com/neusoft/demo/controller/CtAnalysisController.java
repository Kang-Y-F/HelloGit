package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.CtAiConfirmDTO;
import com.neusoft.demo.service.CtAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report/ct")
public class CtAnalysisController {

    @Autowired
    private CtAnalysisService ctAnalysisService;

    /**
     * 生成AI辅助诊断分析
     * 输入：报告ID
     * 行为：收集伪影数据+病历+检验报告，调大模型生成结构化分析
     * 返回：CT影像报告（包含 aiAnalysis 字段）
     */
    @PostMapping("/ai-analyze/{reportId}")
    public Result<?> generateAiAnalysis(@PathVariable Long reportId) {
        return Result.success(ctAnalysisService.generateAiAnalysis(reportId));
    }

    /**
     * 医生确认/修改/驳回AI分析
     * status: 2=确认 3=修改 4=驳回
     */
    @PutMapping("/confirm/{reportId}")
    public Result<?> confirmAiAnalysis(
            @PathVariable Long reportId,
            @RequestBody CtAiConfirmDTO dto
    ) {
        boolean ok = ctAnalysisService.confirmAiAnalysis(reportId, dto);
        return ok ? Result.success("操作成功") : Result.fail("操作失败");
    }
}
