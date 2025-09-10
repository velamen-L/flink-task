# 错题本修正记录简化配置

## 📊 ER图定义

### 源表
```yaml
source_table:
  topic: "business-events"                      # BusinessEvent统一事件流
  event_type: "wrongbook_fix"                   # 错题本修正事件
  payload_structure:                            # WrongbookFixPayload结构
    fixId: "string"                             # 修正记录ID
    wrongId: "string"                           # 错题记录ID
    userId: "string"                            # 用户ID
    subject: "string"                           # 学科代码
    questionId: "string"                        # 题目ID
    patternId: "string"                         # 题型ID
    createTime: "bigint"                        # 创建时间
    submitTime: "bigint"                        # 提交时间
    fixResult: "int"                            # 修正结果(0/1)
```

### 维表
```yaml
dimension_tables:
  - table_name: "tower_pattern"
    alias: "pt"
    primary_key: "id"
    fields:
      id: "string"
      name: "string"
      type: "int"
      subject: "string"
      difficulty: "decimal(5,3)"
      modify_time: "bigint"
    connector_config:
      connector: "jdbc"
      cache_ttl: "30min"
      cache_max_rows: "100000"
      url: "jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower"
      table_name: "tower_pattern"
      username: "zstt_server"
      password: "******"
    
  - table_name: "tower_teaching_type_pt"
    alias: "ttp"
    primary_key: "id"
    fields:
      id: "bigint"
      teaching_type_id: "bigint"
      pt_id: "string"
      order_num: "int"
      is_delete: "tinyint"
      modify_time: "timestamp"
    connector_config:
      connector: "jdbc"
      cache_ttl: "30min"
      cache_max_rows: "100000"
      url: "jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower"
      table_name: "tower_teaching_type_pt"
      username: "zstt_server"
      password: "******"
      
  - table_name: "tower_teaching_type"
    alias: "tt"
    primary_key: "id"
    fields:
      id: "bigint"
      chapter_id: "string"
      teaching_type_name: "string"
      is_delete: "tinyint"
      modify_time: "timestamp"
    connector_config:
      connector: "jdbc"
      cache_ttl: "30min"
      cache_max_rows: "100000"
      url: "jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower"
      table_name: "tower_teaching_type"
      username: "zstt_server"
      password: "******"
```

### 表关联关系
```yaml
join_relationships:
  - join_type: "LEFT JOIN"
    source_table: "source"
    source_field: "payload.patternId"
    target_table: "pt"
    target_field: "id"
    
  - join_type: "LEFT JOIN"
    source_table: "pt"
    source_field: "id"
    target_table: "ttp"
    target_field: "pt_id"
    additional_condition: "ttp.is_delete = 0"
    
  - join_type: "LEFT JOIN"
    source_table: "ttp"
    source_field: "teaching_type_id"
    target_table: "tt"
    target_field: "id"
    additional_condition: "tt.is_delete = 0"
```

## 🎯 结果表定义

```yaml
result_table:
  table_name: "dwd_wrong_record_wide_delta"
  primary_key: "id"
  fields:
    id: "bigint"
    wrong_id: "string"
    user_id: "string"
    subject: "string"
    subject_name: "string"
    question_id: "string"
    question: "string"
    pattern_id: "string"
    pattern_name: "string"
    teaching_type_id: "string"
    teaching_type_name: "string"
    collect_time: "timestamp"
    fix_id: "string"
    fix_time: "timestamp"
    fix_result: "bigint"
    fix_result_desc: "string"
  connector_config:
    connector: "odps"
    project: "zstt"
    table_name: "dwd_wrong_record_wide_delta"
    access_id: "LTAI5tHvJUm7fEzCfrFT3oam"
    access_key: "******"
    endpoint: "http://service.cn-hangzhou.maxcompute.aliyun.com/api"
    enable_upsert: "true"
    operation: "upsert"
    bucket_num: "16"
```

## 🔄 字段映射定义

```yaml
field_mapping:
  # 基础字段 - 直接从payload映射
  id:
    source: "payload.fixId"
    transform: "CAST(JSON_VALUE(se.payload, '$.fixId') AS BIGINT)"
    description: "修正记录ID作为主键"
    
  wrong_id:
    source: "payload.wrongId"
    transform: "JSON_VALUE(se.payload, '$.wrongId')"
    description: "原错题记录ID"
    
  user_id:
    source: "payload.userId"
    transform: "JSON_VALUE(se.payload, '$.userId')"
    description: "用户ID"
    
  subject:
    source: "payload.subject"
    transform: "JSON_VALUE(se.payload, '$.subject')"
    description: "学科代码"
    
  question_id:
    source: "payload.questionId"
    transform: "JSON_VALUE(se.payload, '$.questionId')"
    description: "题目ID"
    
  pattern_id:
    source: "payload.patternId"
    transform: "JSON_VALUE(se.payload, '$.patternId')"
    description: "题型ID"
    
  fix_id:
    source: "payload.fixId"
    transform: "JSON_VALUE(se.payload, '$.fixId')"
    description: "修正ID"
    
  fix_result:
    source: "payload.fixResult"
    transform: "CAST(JSON_VALUE(se.payload, '$.fixResult') AS BIGINT)"
    description: "修正结果(0/1)"
    
  # 维表字段映射
  pattern_name:
    source: "pt.name"
    transform: "pt.name"
    description: "题型名称"
    
  teaching_type_id:
    source: "tt.id"
    transform: "CAST(tt.id AS STRING)"
    description: "教学类型ID"
    
  teaching_type_name:
    source: "tt.teaching_type_name"
    transform: "tt.teaching_type_name"
    description: "教学类型名称"
    
  # 计算字段
  subject_name:
    source: "calculated"
    transform: |
      CASE JSON_VALUE(se.payload, '$.subject')
        WHEN 'ENGLISH' THEN '英语'
        WHEN 'BIOLOGY' THEN '生物'
        WHEN 'math' THEN '数学'
        WHEN 'MATH' THEN '数学'
        WHEN 'PHYSICS' THEN '物理'
        WHEN 'CHEMISTRY' THEN '化学'
        WHEN 'AOSHU' THEN '数学思维'
        WHEN 'SCIENCE' THEN '科学'
        WHEN 'CHINESE' THEN '语文'
        ELSE ''
      END
    description: "学科中文名称"
    
  question:
    source: "calculated"
    transform: "CAST(NULL AS STRING)"
    description: "题目内容(暂为空)"
    
  fix_result_desc:
    source: "calculated"
    transform: |
      CASE CAST(JSON_VALUE(se.payload, '$.fixResult') AS INT)
        WHEN 1 THEN '订正'
        WHEN 0 THEN '未订正'
        ELSE ''
      END
    description: "修正结果描述"
    
  # 时间字段转换
  collect_time:
    source: "payload.createTime"
    transform: "TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(se.payload, '$.createTime') AS BIGINT), 0)"
    description: "错题收集时间"
    
  fix_time:
    source: "payload.submitTime"
    transform: "TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(se.payload, '$.submitTime') AS BIGINT), 0)"
    description: "修正提交时间"
```
