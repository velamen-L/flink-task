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

entity "tower_chapter" as tc <<dimension>> {
  * chapter_id : bigint <<关联表ID>> <<PK>>
  * name : string <<章节名称>>
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
ase ||--o{ tc : "payload.pattern_id = id"
spme ||--o{ tc : "payload.chapter_id = id"
ase ||--o{ tc : "payload.chapter_id = chapter_id"

@enduml
```

## 🔄 字段映射定义

```yaml
# 结果表配置
result_table:
  table_name: "dws_study_pt_stats_delta"
  table_type: "result"
  connector: "mysql"
  database: "guarder"
  primary_key: ["user_id,current_day,subject,pt_id"]

# 字段映射配置
field_mapping:
  # 基础字段映射
  current_day: "ase.statistics_date"
  user_id: "ase.userId"
  subject: "ase.payload.subject"
  pt_id: "ase.payload.pattern_id"
  pt_name: "tp.name"
  master_status: : {
    "description": "当天用户pt的最新掌握度",
    "time_window": "当天",
    "dimensions": ["spme.user_id","spme.statistics_date","spme.payload.subject","spme.payload.pattern_id"],
    "filters": "如果已存在的状态为MASTERED，则不更新掌握度字段",
    "aggregation": "最新的一条记录的masterStatus"
  }
  difficulty: "tp.difficulty"
  answer_cnt: {
    "description": "当天用户pt下的答题数量统计",
    "time_window": "当天",
    "dimensions": ["ase.user_id","ase.statistics_date","ase.payload.subject","ase.payload.pattern_id"],
    "filters": "",
    "aggregation": "COUNT(DISTINCT ase.id)"
  }
  answer_right_cnt: {
    "description": "当天用户的答对数量统计",
    "time_window": "当天",
    "dimensions": ["ase.user_id","ase.statistics_date","ase.payload.subject","ase.payload.pattern_id"],
    "filters": "ase.result = 1",
    "aggregation": "COUNT(DISTINCT ase.id)"
  }
  chapter_id: "ase.payload.chapter_id"
  chapter_name: "tc.name"

```
