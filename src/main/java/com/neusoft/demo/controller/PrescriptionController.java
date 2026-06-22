package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.Prescription;
import com.neusoft.demo.mapper.PrescriptionMapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prescription")
public class PrescriptionController {

    @Autowired
    private PrescriptionMapper prescriptionMapper;

    /**
     * 查询患者所有用药记录
     * 关联 medical_order 通过 order_id 找到该患者的所有处方
     */
    @GetMapping("/patient/{patientId}")
    public Result<?> getByPatient(@PathVariable Long patientId) {
        List<Prescription> list = prescriptionMapper.selectByPatientId(patientId);
        return Result.success(list);
    }
}
