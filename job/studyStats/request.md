# 错题本修正记录极简配置

## 📊 ER图定义

```plantuml
@startuml
!theme plain
skinparam linetype ortho

' 一对一学情ER图
title 一对一学情ER图

' 源表定义 (Kafka)
entity "answerSubmittedEvent" as ase <<source>> {
  * domain : string <<业务域>>
  * type : string <<事件类型>>
  * user_id : string <<用户id>>
  * device_id : string <<设备id>>
  * app_id : string <<应用id>>
  * payload : string <<事件数据JSON>>
  * event_time : string <<事件时间>>
  --
  table_type: source
  domain: answer
  type: answer_submitted
}

entity "answerSubmittedPayload" as asp <<payload>> {
  * id : string <<答题主键>>
  * pattern_id : string <<PT_ID>>
  * chapter_id : string <<章节ID>>
  * result : integer <<答题结果(0:错误 1:正确)>>
  * subject : string <<学科>>
  * stage : integer <<学段>>
}

entity "studyTaskFinishedEvent" as stfe <<source>> {
  * domain : string <<业务域>>
  * type : string <<事件类型>>
  * user_id : string <<用户id>>
  * device_id : string <<设备id>>
  * app_id : string <<应用id>>
  * payload : string <<事件数据JSON>>
  * event_time : string <<事件时间>>
  --
  table_type: source
  domain: study
  type: study_interactive_task_studied
}

entity "studyTaskFinishedPayload" as stfp <<payload>> {
  * subject : string <<学科>>
  * stage : integer <<学段>>
  * teaching_type_id : string <<教学类目id>>
  * teaching_type_name : string <<教学类目名称>>
  * task_pt_id : string <<互动任务或者PT的ID>>
  * task_pt_name : string <<互动任务或者PT名称>>
}

entity "studyPtMasteredEvent" as spme <<source>> {
  * domain : string <<业务域>>
  * type : string <<事件类型>>
  * user_id : string <<用户id>>
  * device_id : string <<设备id>>
  * app_id : string <<应用id>>
  * payload : string <<事件数据JSON>>
  * event_time : string <<事件时间>>
  --
  table_type: source
  domain: study
  type: study_pt_mastered
}

entity "studyPtMasteredPayload" as spmp <<payload>> {
  * ptId : string <<PT_ID>>
  * chapterId : string <<章节id>>
  * masterStatus : string <<掌握度 MASTERED-已掌握 WEAK-薄弱 UNSKILLED-不熟练>>
  * subject : string <<学科>>
  * stage : int <<学段>>
}

entity "guarder_app_time" as time <<source>> {
  * id : string <<ID>> <<PK>>
  * statistics_date : string <<统计维度-日期>>
  * user_id : string <<用户id>>
  * app_id : string <<应用ID>>
  * time : int <<时长 秒>>
  --
  table_type: source
  database: guarder
  connector: mysql-cdc
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
  connector: mysql
}

entity "tower_teaching_type_pt" as ttp <<dimension>> {
  * id : bigint <<关联表ID>> <<PK>>
  * teaching_type_id : bigint <<教学类型ID>>
  * pt_id : string <<题型ID>>
  * is_delete : tinyint <<删除标记>>
  --
  table_type: dimension
  database: tower
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
asp ||--o{ ase
stfp ||--o{ stfe
spmp ||--o{ spme
spme ||--o{ tp : "payload.pt_id = id"
stfe ||--o{ tp : "payload.task_pt_id = id"
ase ||--o{ tp : "payload.pattern_id = id"
tp ||--o{ ttp : "id = pt_id AND is_delete = 0"
ttp ||--o{ tt : "teaching_type_id = id AND is_delete = 0"
stfe ||--o{ tt : "payload.teaching_type_id = id"

@enduml
```

## 🔄 字段映射定义

```yaml
# 结果表配置
result_table:
  table_name: "dws_study_stats_delta"
  table_type: "result"
  connector: "mysql"
  database: "guarder"
  primary_key: ["user_id,current_day,subject"]

# 字段映射配置
field_mapping:
  # 基础字段映射
  current_day: "time.statistics_date"
  user_id: "time.userId"
  subject: "(可以在配置中定义appId到subject的维度表) 根据time.appId获取学科
                当appId为
                com.jzx.client.math1v1
                com.jzx.client.math
                则学科为math
                
                当appId为
                com.jzx.client.chinese
                则学科为chinese
                
                当appId为
                com.jzx.client.english
                则学科为english
                当appId为
                com.jzx.client.physics
                com.jzx.client.physics1v1
                则学科为physics
                
                当appId为
                com.jzx.client.chemistry
                com.jzx.client.chemistry1v1
                则学科为chemistry
                
                当appId为
                com.jzx.client.biology
                com.jzx.client.biology1v1
                则学科为biology"
  study_time: "同一个学科下的sum(time.time)"
  answer_cnt: {
    "description": "当天用户的答题数量统计",
    "time_window": "当天",
    "dimensions": ["ase.user_id","ase.statistics_date","ase.payload.subject"],
    "filters": "",
    "aggregation": "COUNT"
  }
  answer_right_cnt: {
    "description": "当天用户的答对数量统计",
    "time_window": "当天",
    "dimensions": ["ase.user_id","ase.statistics_date","ase.payload.subject","ase.payload.id"],
    "filters": "ase.result = 1",
    "aggregation": "COUNT"
  }
  master_pt_cnt: {
    "description": "当天用户的掌握pt数量统计",
    "time_window": "当天",
    "dimensions": ["spme.user_id","spme.statistics_date","spme.payload.subject","spme.payload.ptId"],
    "filters": "spme.payload.masterStatus = 'MASTERED'",
    "aggregation": "COUNT"
  }
  study_task_count: {
    "description": "当天用户的互动任务数量统计",
    "time_window": "当天",
    "dimensions": ["stfe.user_id","stfe.statistics_date","stfe.payload.subject","stfe.payload.task_pt_id"],
    "filters": "stfe.payload.subject in ('chinese','english')",
    "aggregation": "COUNT"              
  }

  study_task_knowledge_explain_count: {
    "description": "当天用户的互动任务的知识点讲解数量统计",
    "time_window": "当天",
    "dimensions": ["stfe.user_id","stfe.statistics_date","stfe.payload.subject","stfe.payload.task_pt_id"],
    "filters": "stfe.payload.subject not in ('chinese','english') and stfe.payload.teaching_type_name = '知识点讲解'",
    "aggregation": "COUNT"
  }

  study_task_example_explain_count: {
    "description": "当天用户的互动任务的例题讲解数量统计",
    "time_window": "当天",
    "dimensions": ["stfe.user_id","stfe.statistics_date","stfe.payload.subject","stfe.payload.task_pt_id"],
    "filters": "stfe.payload.subject not in ('chinese','english') and stfe.payload.teaching_type_name = '例题讲解'",
    "aggregation": "COUNT"
  }

```
