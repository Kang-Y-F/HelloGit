package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.Drug;
import com.neusoft.demo.mapper.DrugMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/drug")
public class DrugController {

    @Autowired
    private DrugMapper drugMapper;

    /**
     * 管理员查询药品目录
     */
    @GetMapping("/list")
    public Result<?> list(){
        return Result.success(drugMapper.selectList(null));
    }

    /**
     * 管理员修改药品价格
     */
    @PutMapping("/price/{id}")
    public String updatePrice(@PathVariable Long id, @RequestParam BigDecimal price){
        Drug drug = new Drug();
        drug.setId(id);
        drug.setPrice(price);
        drugMapper.updateById(drug);
        return "修改成功";
    }

    /**
     * 管理员启用或停用药品
     */
    @PutMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam Integer status){
        Drug drug = new Drug();
        drug.setId(id);
        drug.setStatus(status);
        drugMapper.updateById(drug);
        return "操作成功";
    }

}
