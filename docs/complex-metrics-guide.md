# 复杂指标规范化生成指南

## 📋 概述

为了解决复杂指标SQL生成中输入标准不统一的问题，我们设计了结构化的输入规范，确保生成的SQL更加准确和标准化。

## 🎯 核心问题解决

### **问题**：
- 不同人输入的复杂指标描述格式不一致
- 生成的SQL质量和准确性不稳定
- 缺乏时间窗口、统计维度、过滤条件的明确约束

### **解决方案**：
- 结构化输入格式
- 智能解析规则
- 标准化SQL模板
- 强制生成约束

## 📝 标准输入格式

### **基础格式**
```yaml
field_name: {
  "description": "业务描述",
  "time_window": "时间窗口类型", 
  "dimensions": ["统计维度字段"],
  "filters": "过滤条件",
  "aggregation": "聚合类型"
}
```

### **简化格式（向后兼容）**
```yaml
field_name: "完整业务描述（包含时间窗口、过滤条件等信息）"
```

## 🔧 实际应用示例

### **示例1：当天统计类指标**

**输入**：
```yaml
chinese_fix_num: {
  "description": "语文科目订正数量统计",
  "time_window": "当天",
  "dimensions": ["user_id"],
  "filters": "subject = 'CHINESE'", 
  "aggregation": "COUNT"
}
```

**或简化输入**：
```yaml
chinese_fix_num: "根据payload.subject,实时统计当天语文科目的订正数量"
```

**生成SQL**：
```sql
-- 临时视图：当天语文科目订正统计
CREATE TEMPORARY VIEW chinese_fix_stats AS
SELECT 
    JSON_VALUE(payload, '$.userId') as user_id,
    COUNT(*) as chinese_fix_count
FROM BusinessEvent
WHERE domain = 'wrongbook'
    AND JSON_VALUE(payload, '$.subject') = 'CHINESE'
    AND DATE_FORMAT(processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')
GROUP BY JSON_VALUE(payload, '$.userId');

-- 主查询中关联
LEFT JOIN chinese_fix_stats cfs
    ON cfs.user_id = JSON_VALUE(be.payload, '$.userId')
```

### **示例2：当天正确率类指标**

**输入**：
```yaml
difficult_fix_rate: {
  "description": "难度题目正确率",
  "time_window": "当天",
  "dimensions": ["user_id"], 
  "filters": "difficulty > 2.0",
  "aggregation": "SUM/COUNT*100"
}
```

**生成SQL**：
```sql
-- 临时视图：难度题目正确率统计
CREATE TEMPORARY VIEW difficult_fix_stats AS
SELECT 
    JSON_VALUE(be.payload, '$.userId') as user_id,
    CASE 
        WHEN COUNT(*) > 0 
        THEN ROUND(SUM(CASE WHEN JSON_VALUE(be.payload, '$.fixResult') = '1' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2)
        ELSE 0 
    END as difficult_fix_rate
FROM BusinessEvent be
LEFT JOIN tower_pattern tp FOR SYSTEM_TIME AS OF be.processing_time
    ON tp.id = JSON_VALUE(be.payload, '$.patternId')
WHERE be.domain = 'wrongbook'
    AND tp.difficulty > 2.0
    AND DATE_FORMAT(be.processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')
GROUP BY JSON_VALUE(be.payload, '$.userId');
```

### **示例3：历史数据查询类指标**

**输入**：
```yaml
history_fix_rate: {
  "description": "历史修正正确率",
  "time_window": "七天内", 
  "dimensions": ["user_id"],
  "aggregation": "SUM/COUNT*100"
}
```

**生成SQL**：
```sql
-- 在主查询中使用子查询
(SELECT COALESCE(
    ROUND(
        SUM(CASE WHEN JSON_VALUE(h.payload, '$.fixResult') = '1' THEN 1 ELSE 0 END) * 100.0 
        / NULLIF(COUNT(*), 0), 2
    ), 0
)
FROM BusinessEvent h 
WHERE h.domain = 'wrongbook' 
    AND JSON_VALUE(h.payload, '$.userId') = JSON_VALUE(be.payload, '$.userId')
    AND h.processing_time >= be.processing_time - INTERVAL '7' DAY
    AND h.processing_time <= be.processing_time
) as history_fix_rate
```

## 🧠 智能解析规则

### **时间窗口识别**
| 关键词 | 生成SQL |
|--------|---------|
| "当天"、"今日" | `DATE_FORMAT(processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')` |
| "七天内"、"最近7天" | `processing_time >= be.processing_time - INTERVAL '7' DAY` |
| "实时"、"最近N小时" | `HOP(processing_time, INTERVAL '10' MINUTE, INTERVAL 'N' HOUR)` |

### **聚合类型识别**
| 关键词 | 生成SQL |
|--------|---------|
| "数量"、"个数" | `COUNT(*)` |
| "正确率"、"成功率" | `ROUND(SUM(CASE...)/COUNT(*)*100, 2)` |
| "平均值" | `AVG(...)` |
| "总和" | `SUM(...)` |

### **过滤条件识别**
| 关键词 | 生成SQL |
|--------|---------|
| "语文"、"CHINESE" | `JSON_VALUE(payload, '$.subject') = 'CHINESE'` |
| "难度大于N" | `difficulty > N` |
| "修正成功" | `JSON_VALUE(payload, '$.fixResult') = '1'` |

## ⚡ 生成规则

### **临时视图 vs 子查询选择**

**创建临时视图的场景**：
- ✅ 当天统计 + 聚合计算
- ✅ 滑动窗口统计
- ✅ 需要关联维表的过滤条件

**使用子查询的场景**：
- ✅ 历史数据查询（如"七天内"）
- ✅ 需要相对当前记录的时间范围

### **命名规范**
- 临时视图：`{field_name}_stats`
- 关联字段：`user_id`  
- 结果字段：`{field_name}`

## 🚀 优势对比

| 方面 | 之前方式 | 新规范方式 |
|------|----------|------------|
| **输入标准** | ❌ 自由文本，格式不统一 | ✅ 结构化格式，标准明确 |
| **SQL质量** | ❌ 生成结果不稳定 | ✅ 模板化生成，质量一致 |
| **可维护性** | ❌ 难以调试和优化 | ✅ 规则清晰，易于维护 |
| **扩展性** | ❌ 新场景需要重新适配 | ✅ 模板化，易于扩展 |
| **准确性** | ❌ 依赖描述理解 | ✅ 结构化解析，准确度高 |

## 📚 最佳实践

1. **优先使用结构化格式**：提供更准确的生成结果
2. **明确时间窗口**：避免歧义，确保SQL正确性
3. **指定统计维度**：确保GROUP BY和关联正确
4. **详细过滤条件**：提高查询精度
5. **选择合适聚合类型**：确保业务逻辑正确

## 🔄 迁移指南

### **现有指标迁移**

**原始格式**：
```yaml
efficiency_score: "实时统计最近一小时的学习效率"
```

**建议迁移为**：
```yaml
efficiency_score: {
  "description": "学习效率统计",
  "time_window": "最近1小时",
  "dimensions": ["user_id"],
  "aggregation": "SUM/COUNT*100"
}
```

这样可以获得更标准化、更准确的SQL生成结果！🎯
