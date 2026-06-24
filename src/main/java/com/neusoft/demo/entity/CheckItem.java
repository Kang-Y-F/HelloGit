package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("check_item")
public class CheckItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目名称 */
    private String name;

    /** 价格 */
    private BigDecimal price;

    /** 所属科室ID */
    private Long deptId;

    /** 项目类型：1=检查（CT等影像） 2=检验（血常规等） */
    private Integer itemType;
}
