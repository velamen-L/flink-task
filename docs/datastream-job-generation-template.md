# DataStream API作业生成标准输入模板

## 📋 概述

为了将您的Flink SQL作业转换为符合当前架构的DataStream API作业，我需要以下标准化输入信息。请按照此模板提供完整信息。

## 🎯 必需输入信息

### 1. 📊 **作业基本信息**

```yaml
job_info:
  job_name: "错题订正记录宽表作业"                    # 作业名称
  description: "实时处理错题订正记录，关联维表生成宽表数据"   # 业务描述
  domain: "wrongbook"                              # 业务域
  event_type: "wrongbook_fix"                      # 事件类型
  parallelism: 4                                   # 并行度
  checkpoint_interval: 60000                       # checkpoint间隔(ms)
```

### 2. 🗄️ **源表信息**

```yaml
source_table:
  table_name: "test_wrong_question_fix_record"
  connector_type: "mysql-cdc"                     # CDC连接器类型
  primary_key: "id"                               # 主键
  event_time_field: "create_time"                 # 事件时间字段
  watermark_delay: "5 SECOND"                     # 水位线延迟
  startup_mode: "initial"                         # 启动模式：initial/latest/timestamp
  
  # CDC监听的事件类型
  cdc_events:
    - "INSERT"
    - "UPDATE" 
    - "DELETE"
  
  # 字段映射（如果需要重命名）
  field_mapping:
    create_time: "event_time"
    is_delete: "delete_flag"
```

### 3. 🔗 **维表信息**

```yaml
dimension_tables:
  - table_name: "test_wrong_question_record"
    alias: "wqr"
    join_type: "LEFT"                             # JOIN类型
    join_condition:                               # 关联条件
      source_field: "origin_wrong_record_id"
      dim_field: "id"
    filter_condition: "is_delete = 0"             # 过滤条件
    cache_config:
      max_rows: 100000
      ttl: "30 min"
      
  - table_name: "test_tower_pattern"
    alias: "pt"
    join_type: "LEFT"
    join_condition:
      source_field: "pattern_id"
      dim_field: "id"
    
  - table_name: "test_tower_teaching_type_pt"
    alias: "ttp"
    join_type: "LEFT"
    join_condition:
      source_field: "pattern_id"
      dim_field: "pt_id"
    filter_condition: "is_delete = 0"
    
  - table_name: "test_tower_teaching_type"
    alias: "tt"
    join_type: "LEFT"
    join_condition:
      source_field: "ttp.teaching_type_id"
      dim_field: "id"
    filter_condition: "is_delete = 0"
    additional_condition: "tt.chapter_id = wqr.chapter_id"  # 额外关联条件
```

### 4. 🎯 **结果表信息**

```yaml
result_table:
  table_name: "dwd_wrong_record_wide_delta_test"
  connector_type: "odps"                          # 输出连接器类型
  sink_operation: "upsert"                        # 输出操作：insert/upsert/append
  primary_key: "id"
  bucket_num: 16                                  # 分桶数量
  
  # 额外输出配置
  additional_outputs:
    kafka:
      enabled: true
      topic: "wrongbook-fix-wide-table"
      condition: "fix_result = 1"                  # 输出条件
```

### 5. 🔄 **业务逻辑配置**

```yaml
business_logic:
  # 字段映射和转换逻辑
  field_transformations:
    - source_field: "wqfr.id"
      target_field: "id"
      transform_type: "CAST"
      target_type: "BIGINT"
      
    - source_field: "wqr.id"
      target_field: "wrong_id"
      transform_type: "DIRECT"
      
    - source_field: "wqr.subject"
      target_field: "subject_name"
      transform_type: "CASE_WHEN"
      mapping:
        "ENGLISH": "英语"
        "BIOLOGY": "生物"
        "MATH": "数学"
        "math": "数学"
        "PHYSICS": "物理"
        "CHEMISTRY": "化学"
        "AOSHU": "数学思维"
        "SCIENCE": "科学"
        "CHINESE": "语文"
        "default": ""
        
    - source_field: "wqfr.result"
      target_field: "fix_result_desc"
      transform_type: "CASE_WHEN"
      mapping:
        "1": "订正"
        "0": "未订正"
        "default": ""
        
    - source_field: "wqr.create_time"
      target_field: "collect_time"
      transform_type: "TO_TIMESTAMP"
      
    - source_field: "wqfr.submit_time"
      target_field: "fix_time"
      transform_type: "TO_TIMESTAMP"
  
  # 过滤条件
  filter_conditions:
    - "wqfr.is_delete = 0"
    - condition_type: "OR"
      sub_conditions:
        - "wqr.subject NOT IN ('CHINESE', 'ENGLISH')"
        - condition_type: "AND"
          sub_conditions:
            - "wqr.subject IN ('CHINESE', 'ENGLISH')"
            - "tt.chapter_id = wqr.chapter_id"
```

### 6. 🔧 **技术配置**

```yaml
technical_config:
  # 状态配置
  state_config:
    backend: "rocksdb"                            # 状态后端
    checkpoint_storage: "oss"                     # checkpoint存储
    
  # 性能配置
  performance_config:
    async_lookups: true                           # 异步维表查询
    batch_size: 1000                             # 批量大小
    buffer_timeout: 5000                         # 缓冲超时(ms)
    
  # 监控配置
  monitoring_config:
    metrics_enabled: true
    latency_tracking: true
    throughput_tracking: true
```

### 7. 📦 **部署配置**

```yaml
deployment_config:
  # 资源配置
  resources:
    task_manager_memory: "2048m"
    job_manager_memory: "1024m"
    task_slots: 2
    
  # 环境配置
  environment:
    flink_version: "1.17"
    scala_version: "2.12"
    
  # 依赖配置
  dependencies:
    - "flink-connector-mysql-cdc"
    - "flink-connector-odps"
    - "flink-connector-kafka"
```

## 📝 **完整示例**

基于您提供的SQL作业，以下是完整的输入示例：

```yaml
# ============ 作业基本信息 ============
job_info:
  job_name: "错题订正记录宽表作业"
  description: "实时处理错题订正记录，通过CDC监听订正事件，关联错题记录、知识点、教学类型等维表，生成宽表数据输出到ODPS"
  domain: "wrongbook"
  event_type: "wrongbook_fix"
  parallelism: 4
  checkpoint_interval: 60000

# ============ 源表信息 ============
source_table:
  table_name: "test_wrong_question_fix_record"
  connector_type: "mysql-cdc"
  primary_key: "id"
  event_time_field: "create_time"
  watermark_delay: "5 SECOND"
  startup_mode: "initial"
  cdc_events: ["INSERT", "UPDATE", "DELETE"]

# ============ 维表信息 ============  
dimension_tables:
  - table_name: "test_wrong_question_record"
    alias: "wqr"
    join_type: "LEFT"
    join_condition:
      source_field: "origin_wrong_record_id"
      dim_field: "id"
    
  - table_name: "test_tower_pattern"
    alias: "pt" 
    join_type: "LEFT"
    join_condition:
      source_field: "pattern_id"
      dim_field: "id"
      
  - table_name: "test_tower_teaching_type_pt"
    alias: "ttp"
    join_type: "LEFT"
    join_condition:
      source_field: "pattern_id"
      dim_field: "pt_id"
    filter_condition: "is_delete = 0"
    
  - table_name: "test_tower_teaching_type"
    alias: "tt"
    join_type: "LEFT"
    join_condition:
      source_field: "ttp.teaching_type_id"
      dim_field: "id"
    filter_condition: "is_delete = 0"

# ============ 结果表信息 ============
result_table:
  table_name: "dwd_wrong_record_wide_delta_test"
  connector_type: "odps"
  sink_operation: "upsert"
  primary_key: "id"
  bucket_num: 16

# ============ 业务逻辑配置 ============
business_logic:
  field_transformations:
    - source_field: "wqfr.id"
      target_field: "id"
      transform_type: "CAST"
      target_type: "BIGINT"
      
    - source_field: "wqr.subject"
      target_field: "subject_name"
      transform_type: "CASE_WHEN"
      mapping:
        "ENGLISH": "英语"
        "BIOLOGY": "生物"
        "MATH": "数学"
        "math": "数学"
        "PHYSICS": "物理"
        "CHEMISTRY": "化学"
        "AOSHU": "数学思维"
        "SCIENCE": "科学"
        "CHINESE": "语文"
        "default": ""
        
    - source_field: "wqfr.result"
      target_field: "fix_result_desc"
      transform_type: "CASE_WHEN"
      mapping:
        "1": "订正"
        "0": "未订正"
        "default": ""
  
  filter_conditions:
    - "wqfr.is_delete = 0"
    - condition_type: "OR"
      sub_conditions:
        - "wqr.subject NOT IN ('CHINESE', 'ENGLISH')"
        - condition_type: "AND"
          sub_conditions:
            - "wqr.subject IN ('CHINESE', 'ENGLISH')"
            - "tt.chapter_id = wqr.chapter_id"
```

## 🚀 **生成输出**

提供完整输入后，我将生成：

### 📦 **1. Java代码文件**
- **Payload类**: `WrongbookFixPayload.java`
- **处理器类**: `WrongbookFixProcessor.java`  
- **MySQL构建器**: `WrongbookFixMySQLBuilder.java`
- **App入口类**: `WrongbookFixWideTableApp.java`

### 📊 **2. 配置文件**
- **路由配置SQL**: 数据库路由配置插入语句
- **application.properties**: 应用配置
- **deployment.yaml**: 部署配置

### 📋 **3. 文档文件**
- **README.md**: 作业说明和部署指南
- **ARCHITECTURE.md**: 架构设计说明
- **MONITORING.md**: 监控和运维指南

## ❓ **还需要哪些信息？**

请根据此模板提供：

1. ✅ **已提供**: 源表、维表、结果表的DDL
2. ⚠️ **需要补充**: 
   - 具体的字段转换逻辑
   - 过滤条件的详细规则
   - 输出目标配置（是否需要Kafka输出）
   - 性能和资源要求
   - 监控需求

**请将您的具体配置按照上述YAML格式整理后提供给我，我将生成完整的DataStream API作业！** 🎯