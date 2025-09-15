# 错题本修正记录极简配置

## 📊 ER图定义

```plantuml
@startuml
!theme plain
skinparam linetype ortho

' 错题本修正记录实时宽表ER图
title 错题本修正记录实时宽表ER图

' 源表定义 (Kafka)
entity "BusinessEvent" as be <<source>> {
  * domain : string <<业务域>>
  * type : string <<事件类型>>
  * payload : string <<错题修正载荷JSON>>
  * event_time : string <<事件时间>>
  --
  table_type: source
  domain: wrongbook
  topic: wrongbook-events
  connector: kafka
}

' 维表定义 (MySQL)
entity "tower_pattern" as tp <<dimension>> {
  * id : string <<题型ID>> <<PK>>
  * name : string <<题型名称>>
  * subject : string <<学科>>
  * difficulty : decimal(5,3) <<难度系数>>
  --
  table_type: dimension
  database: tower
  ttl: 30min
}

entity "tower_teaching_type_pt" as ttp <<dimension>> {
  * id : bigint <<关联表ID>> <<PK>>
  * teaching_type_id : bigint <<教学类型ID>>
  * pt_id : string <<题型ID>>
  * is_delete : tinyint <<删除标记>>
  --
  table_type: dimension
  database: tower
  ttl: 30min
}

entity "tower_teaching_type" as tt <<dimension>> {
  * id : bigint <<教学类型ID>> <<PK>>
  * teaching_type_name : string <<教学类型名称>>
  * chapter_id : string <<章节ID>>
  * is_delete : tinyint <<删除标记>>
  --
  table_type: dimension
  database: tower
}

' 关联关系
be ||--o{ tp : "payload.patternId = id\n[LEFT JOIN]"
tp ||--o{ ttp : "id = pt_id\n[LEFT JOIN]\nAND is_delete = 0"
ttp ||--o{ tt : "teaching_type_id = id\n[LEFT JOIN]\nAND is_delete = 0"

' 注释说明
note right of be
  Kafka源表
  - 自动配置topic: wrongbook-events
  - 自动过滤: domain = 'wrongbook'
  - 载荷字段: payload.patternId等
end note

note right of tp
  题型维表 (MySQL)
  - database: tower
  - ttl: 30min (自定义缓存时间)
  - FOR SYSTEM_TIME AS OF优化
end note

note bottom
  结果表通过字段映射配置定义
  - 支持指标描述智能生成SQL
  - 自动推断结果表结构
end note

@enduml
```

## 🔄 字段映射定义

```yaml
# 结果表配置
result_table:
  table_name: "dwd_wrong_record_wide_delta"
  table_type: "result"
  connector: "mysql"
  database: "guarder"
  primary_key: ["id"]

# 字段映射配置
field_mapping:
  # 基础字段映射
  id: "payload.fixId"
  wrong_id: "payload.wrongId"
  user_id: "payload.userId"
  subject: "payload.subject"
  question_id: "payload.questionId"
  pattern_id: "payload.patternId"
  fix_id: "payload.fixId"
  fix_result: "payload.fixResult"
  
  # 维表字段映射
  pattern_name: "tower_pattern.name"
  teaching_type_id: "tower_teaching_type.id"
  teaching_type_name: "tower_teaching_type.teaching_type_name"
  
  # 计算字段
  subject_name: "CASE payload.subject WHEN 'ENGLISH' THEN '英语' WHEN 'BIOLOGY' THEN '生物' WHEN 'MATH' THEN '数学' WHEN 'PHYSICS' THEN '物理' WHEN 'CHEMISTRY' THEN '化学' WHEN 'CHINESE' THEN '语文' ELSE '' END"
  fix_result_desc: "CASE payload.fixResult WHEN 1 THEN '订正' WHEN 0 THEN '未订正' ELSE '' END"
  
  # 时间字段转换
  collect_time: "payload.createTime"
  fix_time: "payload.submitTime"
  
  # 智能指标字段 (基于描述生成SQL)
  history_fix_rate: "获取七天内用户订正的正确率"
  chinese_fix_num: "根据payload.subject,实时统计当天语文科目的订正数量"
  difficult_fix_rate: "根据payload.result和tower_pattern.difficulty，实时统计用户当天在难度大于2的题目上的答题正确率"
```
