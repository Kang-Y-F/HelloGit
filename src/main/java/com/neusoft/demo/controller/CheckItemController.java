package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.entity.CheckItem;
import com.neusoft.demo.mapper.CheckItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
