# Flink SQL作业生成请求模板 v3.0

## 📋 作业基本信息

```yaml
job_info:
  name: "作业名称"
  description: "作业描述"
  domain: "业务域"
  event_type: "事件类型"
  author: "开发者"
  version: "1.0.0"
  create_date: "2024-12-27"
```

## 🗄️ 源表配置

### BusinessEvent标准事件流
- **源表名**: BusinessEvent (统一事件流表)
- **事件过滤**: domain = '{domain}' AND type = '{event_type}'
- **Payload结构**: {PayloadClass}

**{PayloadClass}数据结构**:
```java
public class {PayloadClass} {
    // 在此定义payload字段
    private String id;
    private String userId;
    // ... 其他业务字段
}
```

## 🔗 维表配置

### 维表1: {table_name}
- **过滤条件**: {filter_condition} (可选)

**维表结构**:
```sql
{CREATE_TABLE_SQL}
```

### 维表2: {table_name}
- **过滤条件**: {filter_condition} (可选)

**维表结构**:
```sql
{CREATE_TABLE_SQL}
```

## 🎯 结果表配置

### 表名: {result_table_name}
- **操作类型**: INSERT
- **主键**: {primary_key}

**结果表结构**:
```sql
{CREATE_TABLE_SQL}
```

## 🔄 字段映射配置

### 基础字段映射
```yaml
field_mapping:
  # 从payload映射的字段
  {result_field}: payload.{payload_field}
  
  # 从维表映射的字段  
  {result_field}: {dim_table_alias}.{dim_field}
  
  # 计算字段
  {result_field}: {calculation_expression}
```

## 🗺️ ER图定义

### 实体关系图 (Mermaid格式)
```mermaid
erDiagram
    {DOMAIN}_{EVENT_TYPE} {
        string id PK "主键"
        string field1 "字段1"
        string field2 FK "外键字段"
    }
    
    {DIM_TABLE_1} {
        string id PK "主键"
        string name "名称"
        string field1 "字段1"
    }
    
    {DIM_TABLE_2} {
        string id PK "主键"
        string field1 "字段1"
    }

    %% 关系定义 - 提供JOIN关联条件
    {DOMAIN}_{EVENT_TYPE} }o--|| {DIM_TABLE_1} : "payload.field2 = dim1.id"
    {DIM_TABLE_1} ||--o{ {DIM_TABLE_2} : "dim1.field1 = dim2.id"
```

### 关联关系定义
```yaml
join_relationships:
  # 源表到维表的关联
  source_to_dim1:
    source_table: "{domain}_{event_type}"
    source_field: "payload.{field_name}"
    target_table: "{dim_table_1}"
    target_field: "id"
    join_type: "LEFT JOIN"
    
  # 维表之间的关联
  dim1_to_dim2:
    source_table: "{dim_table_1}"
    source_field: "{field_name}"
    target_table: "{dim_table_2}"
    target_field: "id"
    join_type: "LEFT JOIN"
```

## 💬 备注说明

### 业务逻辑
- 描述特殊的业务处理规则
- 数据转换逻辑说明

### 数据质量要求
- 必填字段检查
- 数据范围验证
- 业务规则验证
