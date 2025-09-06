# Flink SQL作业生成请求模板 v2.0

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
- **关联条件**: {join_condition}

**维表结构**:
```sql
{CREATE_TABLE_SQL}
```

### 维表2: {table_name}
- **关联条件**: {join_condition}

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
    
    {RESULT_TABLE} {
        string id PK "主键"
        string field1 "字段1"
        timestamp create_time "创建时间"
    }

    %% 关系定义
    {DOMAIN}_{EVENT_TYPE} }o--|| {DIM_TABLE_1} : "多对一"
    {DIM_TABLE_1} ||--o{ {DIM_TABLE_2} : "一对多"
    
    %% 数据流关系
    {DOMAIN}_{EVENT_TYPE} ||--|| {RESULT_TABLE} : "生成宽表"
    {DIM_TABLE_1} ||--|| {RESULT_TABLE} : "提供维度信息"
```

### 表结构定义
```yaml
tables:
  source_table:
    name: "{domain}_{event_type}"
    type: "source"
    fields:
      - name: "id"
        type: "VARCHAR(255)"
        is_primary_key: true
      - name: "field1"
        type: "VARCHAR(255)"
        is_foreign_key: true
        references: "{dim_table}.id"
        
  dimension_tables:
    - name: "{dim_table_1}"
      type: "dimension"
      fields:
        - name: "id"
          type: "VARCHAR(255)"
          is_primary_key: true
        - name: "name"
          type: "VARCHAR(255)"
          
  result_table:
    name: "{result_table}"
    type: "result"
    fields:
      - name: "id"
        type: "BIGINT"
        is_primary_key: true
      - name: "field1"
        type: "VARCHAR(255)"
```

## 💬 备注说明

### 业务逻辑
- 描述特殊的业务处理规则
- 数据转换逻辑说明

### 数据质量要求
- 必填字段检查
- 数据范围验证
- 业务规则验证
