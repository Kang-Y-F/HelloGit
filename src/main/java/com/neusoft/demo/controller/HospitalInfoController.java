package com.neusoft.demo.controller;

import com.neusoft.demo.entity.HospitalInfo;
import com.neusoft.demo.service.HospitalInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hospital-info")
public class HospitalInfoController {

    @Autowired
    private HospitalInfoService service;

    /** 获取医院信息 */
    @GetMapping
    public HospitalInfo get(){

        return service.getInfo();

    }

    /** 修改医院信息 */
    @PutMapping
    public String update(
            @RequestBody HospitalInfo info){

        service.updateInfo(info);

        return "修改成功";

    }

}
