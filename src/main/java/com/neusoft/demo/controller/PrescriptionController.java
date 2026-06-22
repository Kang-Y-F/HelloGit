package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.Prescription;
import com.neusoft.demo.mapper.PrescriptionMapper;
import org.apache.ibatis.annotations.Select;
import com.neusoft.demo.service.PrescriptionService;
import com.neusoft.demo.vo.PatientPrescriptionVO;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/prescription")
public class PrescriptionController {

    @Autowired
    private PrescriptionMapper prescriptionMapper;
    private PrescriptionService prescriptionService;

    /**
     * 查询患者所有用药记录
     * 关联 medical_order 通过 order_id 找到该患者的所有处方
     * P2 患者端：我的处方列表
     * GET /prescription/my-list
     * 需要JWT登录鉴权，自动从token取患者id
     */
    @GetMapping("/patient/{patientId}")
    public Result<?> getByPatient(@PathVariable Long patientId) {
        List<Prescription> list = prescriptionMapper.selectByPatientId(patientId);
    @GetMapping("/my-list")
    public Result<List<PatientPrescriptionVO>> myPrescription(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if(userIdObj == null) {
            return Result.fail("请先登录");
        }
        Long patientId = Long.parseLong(userIdObj.toString());
        List<PatientPrescriptionVO> list = prescriptionService.getMyPrescription(patientId);
        return Result.success(list);
    }
}
