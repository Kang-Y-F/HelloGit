package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.CheckItem;
import com.neusoft.demo.mapper.CheckItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/check-item")
public class CheckItemController {

    @Autowired
    private CheckItemMapper checkItemMapper;

    /**
     * 检查/检验项目列表
     * @param itemType 1=检查 2=检验（可选，不传返回全部）
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer itemType) {
        LambdaQueryWrapper<CheckItem> qw = new LambdaQueryWrapper<>();
        if (itemType != null) qw.eq(CheckItem::getItemType, itemType);
        return Result.success(checkItemMapper.selectList(qw));
    }

    /**
     * 新增检查检验项目
     */
    @PostMapping("/add")
    public String add(@RequestBody CheckItem item) {
        checkItemMapper.insert(item);
        return "添加成功";
    }
    /**
     * 修改检查检验项目价格
     */
    @PutMapping("/price/{id}")
    public String updatePrice(@PathVariable Long id, @RequestParam BigDecimal price){
        CheckItem item = new CheckItem();
        item.setId(id);
        item.setPrice(price);
        checkItemMapper.updateById(item);
        return "修改成功";
    }
    /**
     * 删除检查检验项目
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id){
        checkItemMapper.deleteById(id);
        return "删除成功";
    }


}
