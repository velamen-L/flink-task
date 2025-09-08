# Flink SQL AI 生成模板

基于你调整后的 `flink-sql-request-v3.md` 格式，AI 应该理解和生成符合以下规范的 Flink SQL 代码。

## 🔍 输入识别规则

### YAML 配置识别
AI 遇到以下 YAML 结构时，应识别为 Flink SQL 生成任务：

```yaml
job_info:
  name: "作业名称"
  domain: "业务域"
  event_type: "事件类型"

field_mapping:
  id: "CAST(payload.id AS BIGINT)"
  business_field: "payload.business_field"

join_relationships:
  source_to_dim:
    source_field: "payload.field"
    target_table: "dimension_table"
    join_type: "LEFT JOIN"
```

### 关键元素识别
- `BusinessEvent` - 标准事件流表
- `payload.` - payload 字段引用，需转换为 `JSON_VALUE(be.payload, '$.field')`
- `FOR SYSTEM_TIME AS OF PROCTIME()` - 维表时间旅行查询
- `field_mapping:` - YAML 字段映射配置
- 多行 CASE WHEN 表达式

## 🏗️ SQL 生成规则

### 1. INSERT INTO 结构
```sql
INSERT INTO {catalog}.{database}.{result_table}
SELECT
  -- 字段列表
FROM {source_table} {alias}
LEFT JOIN {dim_table} FOR SYSTEM_TIME AS OF PROCTIME() {dim_alias}
  ON {join_condition}
WHERE {filter_condition}
```

### 2. Payload 字段转换
```yaml
# 输入配置
field_mapping:
  id: "CAST(payload.id AS BIGINT)"
  user_id: "payload.user_id"
```

转换为：
```sql
-- AI 自动转换
CAST(JSON_VALUE(be.payload, '$.id') AS BIGINT) AS id,
JSON_VALUE(be.payload, '$.user_id') AS user_id
```

### 3. 复杂表达式处理
```yaml
# YAML 多行表达式
subject_name: |
  CASE payload.subject
      WHEN 'ENGLISH' THEN '英语'
      WHEN 'MATH' THEN '数学'
      ELSE ''
  END
```

转换为：
```sql
CASE JSON_VALUE(be.payload, '$.subject')
  WHEN 'ENGLISH' THEN '英语'
  WHEN 'MATH' THEN '数学'
  ELSE ''
END AS subject_name
```

### 4. 时间字段处理
```yaml
create_time: "TO_TIMESTAMP_LTZ(payload.create_time, 0)"
```

转换为：
```sql
TO_TIMESTAMP_LTZ(JSON_VALUE(be.payload, '$.create_time'), 0) AS create_time
```

## 🔗 关联关系处理

### JOIN 生成规则
基于 `join_relationships` 配置：

```yaml
join_relationships:
  source_to_pattern:
    source_field: "payload.pattern_id"
    target_table: "tower_pattern"
    target_field: "id"
    join_type: "LEFT JOIN"
    additional_condition: null
```

生成：
```sql
LEFT JOIN tower_pattern FOR SYSTEM_TIME AS OF PROCTIME() pt 
    ON pt.id = JSON_VALUE(be.payload, '$.pattern_id')
```

### 过滤条件处理
```yaml
additional_condition: "pt.is_delete = 0"
```

追加到 JOIN 条件：
```sql
LEFT JOIN tower_pattern FOR SYSTEM_TIME AS OF PROCTIME() pt 
    ON pt.id = JSON_VALUE(be.payload, '$.pattern_id')
    AND pt.is_delete = 0
```

## 🎯 结果表配置

### 基于结果表结构生成 SELECT
AI 应该根据结果表的字段结构，匹配 `field_mapping` 中的配置生成相应的 SELECT 字段。

### 数据类型处理
- `BIGINT` 字段：使用 `CAST(...AS BIGINT)`
- `STRING` 字段：直接映射或使用字符串函数
- `TIMESTAMP(3)` 字段：使用 `TO_TIMESTAMP_LTZ(..., 0)`
- `DECIMAL` 字段：保持原有精度

## 📝 注释规范

生成的 SQL 应包含：
```sql
-- ================================================================
-- Flink SQL作业: {job_name}
-- 业务描述: {description}
-- 生成时间: {current_date}
-- ================================================================
```

## ⚡ 性能优化规则

1. **维表查询优化**：自动添加 `FOR SYSTEM_TIME AS OF PROCTIME()`
2. **过滤条件前置**：将过滤条件尽可能靠近数据源
3. **字段裁剪**：只 SELECT 需要的字段
4. **JOIN 顺序**：小表在右，大表在左

## 🔍 业务规则识别

### 特殊条件处理
```yaml
special_conditions:
  subject_chapter_matching:
    condition: |
      (payload.subject NOT IN ('CHINESE', 'ENGLISH')
       OR (payload.subject IN ('CHINESE', 'ENGLISH') AND tt.chapter_id = payload.chapter_id))
```

转换为 WHERE 子句：
```sql
WHERE JSON_VALUE(be.payload, '$.isDelete') = '0'
  AND (JSON_VALUE(be.payload, '$.subject') NOT IN ('CHINESE', 'ENGLISH')
       OR (JSON_VALUE(be.payload, '$.subject') IN ('CHINESE', 'ENGLISH') 
           AND tt.chapter_id = JSON_VALUE(be.payload, '$.chapter_id')))
```

## ✅ 生成验证规则

AI 生成的 SQL 应该：
1. 语法正确，可直接在 Flink 中执行
2. 包含所有必要的 JSON_VALUE 转换
3. 维表 JOIN 使用时间旅行查询
4. 过滤条件完整且正确
5. 字段类型转换正确
6. 注释清晰易懂
