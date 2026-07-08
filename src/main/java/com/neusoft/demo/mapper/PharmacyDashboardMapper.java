package com.neusoft.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 药房数据总览看板 - 统计聚合专用 Mapper
 * 全部用注解写 SQL，不单独维护 XML
 */
@Mapper
public interface PharmacyDashboardMapper {

    /**
     * 今日发药金额 + 处方数
     * presc_status: 0正常 1作废 2已发药
     * 返回 map: { amount: BigDecimal, count: Long }
     */
    @Select("""
        SELECT
            COALESCE(SUM(total_amount), 0) AS amount,
            COUNT(*) AS count
        FROM prescription
        WHERE presc_status = 2
          AND DATE(create_time) = CURDATE()
        """)
    Map<String, Object> todayDispenseSummary();

    /**
     * 库存总价值 + 健康度 + 低库存数量
     * 返回 map: { totalValue: BigDecimal, healthRate: Double, lowStockCount: Long }
     */
    @Select("""
        SELECT
            SUM(i.stock_qty * d.price) AS totalValue,
            SUM(CASE WHEN i.stock_qty >= i.safety_qty THEN 1 ELSE 0 END) / COUNT(*) AS healthRate,
            SUM(CASE WHEN i.stock_qty < i.safety_qty THEN 1 ELSE 0 END) AS lowStockCount
        FROM drug_inventory i
        JOIN drug d ON d.id = i.drug_id
        WHERE d.status = 1
        """)
    Map<String, Object> inventorySummary();

    /**
     * 按药品分类的库存分布
     * 返回 list of map: { category: String, count: Long, value: BigDecimal }
     */
    @Select("""
        SELECT
            d.category AS category,
            COUNT(*)   AS count,
            SUM(i.stock_qty * d.price) AS value
        FROM drug_inventory i
        JOIN drug d ON d.id = i.drug_id
        WHERE d.status = 1
        GROUP BY d.category
        ORDER BY value DESC
        """)
    List<Map<String, Object>> categoryDistribution();

    /**
     * 近7日出入库趋势
     * 用 after_stock - before_stock 的正负号判断方向，不依赖 record_type 具体枚举值
     * 返回 list of map: { date: String, inQty: Long, outQty: Long, inAmount: BigDecimal, outAmount: BigDecimal }
     */
    @Select("""
        SELECT
            DATE(create_time) AS `date`,
            SUM(CASE WHEN after_stock > before_stock THEN (after_stock - before_stock) ELSE 0 END) AS inQty,
            SUM(CASE WHEN after_stock < before_stock THEN (before_stock - after_stock) ELSE 0 END) AS outQty,
            SUM(CASE WHEN after_stock > before_stock THEN total_amount ELSE 0 END) AS inAmount,
            SUM(CASE WHEN after_stock < before_stock THEN total_amount ELSE 0 END) AS outAmount
        FROM drug_inout_record
        WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
        GROUP BY DATE(create_time)
        ORDER BY `date` ASC
        """)
    List<Map<String, Object>> trend7d();

    /**
     * Top10 发药量排行
     * record_type=2 明确是"发药出库"，语义清楚直接用枚举值筛选
     * 返回 list of map: { drugName: String, qty: Long }
     */
    @Select("""
        SELECT
            drug_name AS drugName,
            SUM(quantity) AS qty
        FROM drug_inout_record
        WHERE record_type = 2
        GROUP BY drug_id, drug_name
        ORDER BY qty DESC
        LIMIT 10
        """)
    List<Map<String, Object>> topDrugsOut();

    /**
     * AI 审方结果分布（近30天）
     * audit_status: 1通过 2警告 3拒绝
     * 返回 map: { pass: Long, warn: Long, deny: Long }
     */
    @Select("""
        SELECT
            SUM(CASE WHEN audit_status = 1 THEN 1 ELSE 0 END) AS pass,
            SUM(CASE WHEN audit_status = 2 THEN 1 ELSE 0 END) AS warn,
            SUM(CASE WHEN audit_status = 3 THEN 1 ELSE 0 END) AS deny
        FROM prescription
        WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
        """)
    Map<String, Object> auditStats();

    /**
     * 低库存预警明细列表
     * 返回 list of map: { drugName: String, category: String, stockQty: Integer, safetyQty: Integer }
     */
    @Select("""
        SELECT
            d.drug_name  AS drugName,
            d.category   AS category,
            i.stock_qty  AS stockQty,
            i.safety_qty AS safetyQty
        FROM drug_inventory i
        JOIN drug d ON d.id = i.drug_id
        WHERE i.stock_qty < i.safety_qty
          AND d.status = 1
        ORDER BY (i.stock_qty / i.safety_qty) ASC
        """)
    List<Map<String, Object>> lowStockList();
}