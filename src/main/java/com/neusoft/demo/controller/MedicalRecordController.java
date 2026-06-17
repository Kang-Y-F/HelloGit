package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.AiConfirmDTO;
import com.neusoft.demo.dto.MedicalRecordDTO;
import com.neusoft.demo.service.MedicalRecordService;
import com.neusoft.demo.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medical-record")
public class MedicalRecordController {

    @Autowired
    private MedicalRecordService medicalRecordService;

    /** 创建病历 */
    @PostMapping("/create")
    public Result<?> create(@RequestBody MedicalRecordDTO dto, HttpServletRequest request) {
        Long doctorId = parseDoctorId(request);
        Long recordId = medicalRecordService.create(doctorId, dto);
        return Result.success(recordId);
    }

    /** 病历详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(medicalRecordService.getDetail(id));
    }

    /** 患者历史病历（按患者ID） */
    @GetMapping("/patient/{patientId}")
    public Result<?> listByPatient(@PathVariable Long patientId) {
        return Result.success(medicalRecordService.listByPatient(patientId));
    }

    /**
     * 医生自己接诊的所有病历（历史病历页默认展示）
     * keyword 可选，按患者姓名/手机号搜索
     */
    @GetMapping("/my-records")
    public Result<?> myRecords(
            @RequestParam(required = false, defaultValue = "") String keyword,
            HttpServletRequest request
    ) {
        Long doctorId = parseDoctorId(request);
        return Result.success(medicalRecordService.listByDoctor(doctorId, keyword));
    }

    /** 触发AI生成诊断建议 */
    @PostMapping("/{recordId}/ai-advice")
    public Result<?> generateAiAdvice(@PathVariable Long recordId) {
        try {
            return Result.success(medicalRecordService.generateAiAdvice(recordId));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 医生确认/修改/驳回AI建议 */
    @PutMapping("/{recordId}/confirm-ai")
    public Result<?> confirmAi(@PathVariable Long recordId, @RequestBody AiConfirmDTO dto) {
        boolean ok = medicalRecordService.confirmAi(recordId, dto);
        return ok ? Result.success("操作成功") : Result.fail("操作失败");
    }

    private Long parseDoctorId(HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        return claims.get("userId", Long.class);
    }
}
