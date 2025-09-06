# 错题本作业域配置

## 📋 作业基本信息

```yaml
job_metadata:
  job_name: "wrongbook-wide-table-job"
  job_type: "SINGLE_DOMAIN"
  description: "错题本记录宽表生成作业"
  version: "1.0.0"
  business_domains: ["wrongbook"]
  business_goals: 
    - "错题记录宽表生成"
    - "订正效果分析"
    - "知识点掌握度统计"
  author: "数据开发团队"
  create_date: "2024-12-27"
```

## 🗄️ 事件源配置

### 错题本域事件源
- **源表名**: biz_statistic_wrongbook
- **事件域**: wrongbook
- **关心的事件类型**: 
  - wrongbook_fix (错题订正)
  - wrongbook_collect (错题收集)

```yaml
event_sources:
  - source_name: "wrongbook_events"
    event_domain: "wrongbook"
    topic_name: "biz_statistic_wrongbook"
    interested_event_types:
      - "wrongbook_fix"
      - "wrongbook_collect"
    filter:
      basic_filters:
        domain: "wrongbook"
      custom_filters:
        - "JSON_VALUE(payload, '$.userId') IS NOT NULL"
        - "JSON_VALUE(payload, '$.questionId') IS NOT NULL"
    consumer:
      group_id: "wrongbook-job-consumer"
      startup_mode: "latest"
```

## 🔗 维表配置

### 知识点维表 (tower_pattern)
```yaml
dim_tables:
  - table_name: "tower_pattern"
    table_alias: "pt"
    connector: "jdbc"
    join_conditions:
      - event_source: "wrongbook_events"
        join_condition: "pt.id = JSON_VALUE(wrongbook_events.payload, '$.patternId')"
        join_type: "LEFT"
    cache:
      ttl: "30min"
      max_rows: 100000
```

### 教学类型映射维表 (tower_teaching_type_pt)
```yaml
  - table_name: "tower_teaching_type_pt"
    table_alias: "ttp"
    connector: "jdbc"
    join_conditions:
      - event_source: "wrongbook_events"
        join_condition: "ttp.pt_id = JSON_VALUE(wrongbook_events.payload, '$.patternId')"
        join_type: "LEFT"
        additional_conditions: "ttp.is_delete = 0"
```

### 教学类型维表 (tower_teaching_type)
```yaml
  - table_name: "tower_teaching_type"
    table_alias: "tt"
    connector: "jdbc"
    join_conditions:
      - event_source: "wrongbook_events"
        join_condition: "tt.id = ttp.teaching_type_id"
        join_type: "LEFT"
        additional_conditions: "tt.is_delete = 0"
```

## 🎯 结果表配置

### 错题记录宽表
```yaml
outputs:
  - output_name: "wrongbook-wide-table"
    output_type: "TABLE"
    target_name: "dwd_wrong_record_wide_delta"
    properties:
      connector: "odps"
      operation: "upsert"
```

## 🔄 字段映射配置

### 基础字段
- `id`: payload.fixId - 使用订正记录ID作为主键
- `wrong_id`: payload.wrongId - 原始错题记录ID
- `user_id`: payload.userId - 用户ID
- `subject`: payload.subject - 科目代码
- `question_id`: payload.questionId - 题目ID
- `pattern_id`: payload.patternId - 知识点ID
- `fix_result`: payload.fixResult - 订正结果状态

### 转换字段
- `subject_name`: 科目名称中文转换
  - 'ENGLISH' -> '英语'
  - 'MATH' -> '数学'
  - 'CHINESE' -> '语文'
  - 其他 -> ''

- `fix_result_desc`: 订正结果描述转换
  - 1 -> '订正'
  - 0 -> '未订正'
  - 其他 -> ''

### 关联字段
- `pattern_name`: pt.name - 知识点名称
- `teach_type_id`: CAST(tt.id AS STRING) - 教学类型ID
- `teach_type_name`: tt.teaching_type_name - 教学类型名称

### 时间字段
- `collect_time`: TO_TIMESTAMP_LTZ(payload.createTime, 3) - 错题收集时间
- `fix_time`: TO_TIMESTAMP_LTZ(payload.submitTime, 3) - 订正提交时间

## 📊 业务逻辑配置

### 处理策略
```yaml
processing_strategy:
  processing_mode: "UNION"  # 合并同域的多种事件
  time_alignment_strategy: "EVENT_TIME"
```

### 复杂业务规则
```sql
-- 科目特殊处理规则：语文英语需要额外章节匹配
(
  payload.subject NOT IN ('CHINESE', 'ENGLISH')
  OR (
    payload.subject IN ('CHINESE', 'ENGLISH') 
    AND tt.chapter_id = payload.chapterId
  )
)
```

### 数据质量规则
```yaml
data_quality:
  required_fields:
    - "payload.fixId"
    - "payload.wrongId"
    - "payload.userId"
  value_ranges:
    - field: "payload.fixResult"
      values: [0, 1]
  completeness_threshold: 0.99
```

## 📈 监控配置

### 业务指标
- 处理事件数量 (wrongbook_events_processed_total)
- JOIN成功率 (wrongbook_join_success_rate)
- 数据质量通过率 (wrongbook_data_quality_rate)
- 处理延迟 (wrongbook_processing_latency)

### 告警阈值
- JOIN成功率 < 95%
- 数据质量通过率 < 99%
- 处理延迟 P95 > 5分钟

## 💬 备注说明

### 特殊处理逻辑
- question字段暂时设为NULL，后续可能需要从其他源补充
- 语文英语科目需要额外的章节ID匹配逻辑
- 时间字段统一转换为TIMESTAMP_LTZ类型

### 扩展计划
- 支持错题推荐算法集成
- 增加学习路径分析
- 支持实时知识点掌握度评估
