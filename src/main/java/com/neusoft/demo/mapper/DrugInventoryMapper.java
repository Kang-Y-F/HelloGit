package com.neusoft.demo.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.demo.entity.DrugInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;
@Mapper
public interface DrugInventoryMapper extends BaseMapper<DrugInventory> {

    @Select("""
            SELECT d.*, i.stock_qty, i.safety_qty, i.total_in, i.total_out, i.update_time AS inventoryUpdateTime
            FROM drug d
            LEFT JOIN drug_inventory i ON d.id = i.drug_id
            WHERE d.status = 1
            ORDER BY d.id
            """)
    List<Map<String, Object>> selectAllWithStock();
}