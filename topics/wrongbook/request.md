# 错题本系统实时宽表作业需求

## 📋 基本信息
- **Topic名称**: `wrongbook`
- **业务域**: 错题本系统
- **作业类型**: 实时宽表处理
- **处理模式**: DataStream API + Flink SQL 混合架构

## 🎯 业务描述
错题本系统实时宽表作业，用于处理学生错题的收集、订正、删除等事件，构建完整的错题宽表数据，支持实时查询和分析。

### 核心功能
1. **错题收集**: 处理学生答题错误事件，记录错题基础信息
2. **错题订正**: 处理学生订正错题事件，更新订正结果和反馈
3. **错题删除**: 处理错题删除事件，维护数据一致性
4. **宽表构建**: 实时构建包含题目、知识点、订正状态等完整信息的宽表

## 📊 数据表信息

### 源表 (Kafka)
- **表名**: `wrongbook_events`
- **数据库**: `realtime_db` 
- **连接器**: `kafka`
- **Topic**: `wrongbook-events`
- **Schema来源**: `catalog` (自动查询)

### 维表 (MySQL)
#### 1. 题目信息表
- **表名**: `question_info`
- **数据库**: `education_db`
- **连接器**: `mysql`
- **Schema来源**: `catalog`
- **关联字段**: `question_id`

#### 2. 知识点信息表
- **表名**: `pattern_info` 
- **数据库**: `education_db`
- **连接器**: `mysql`
- **Schema来源**: `catalog`
- **关联字段**: `pattern_id`

#### 3. 教学类型表
- **表名**: `teach_type_info`
- **数据库**: `education_db` 
- **连接器**: `mysql`
- **Schema来源**: `catalog`
- **关联字段**: `teach_type_id`

### 结果表 (MySQL)
- **表名**: `dwd_wrong_record_wide_delta`
- **数据库**: `warehouse_db`
- **连接器**: `mysql`
- **Schema来源**: `catalog`