# 错题本系统Flink SQL作业配置说明

## 📋 概述

本文档描述了基于智能作业生成器生成的错题本系统Flink SQL作业的完整配置和部署方案。

## 🏗️ 作业架构

### 架构模式
- **模式**: 混合架构（Flink SQL + 动态路由）
- **数据流**: Kafka → Flink SQL → MySQL
- **维表关联**: 4个维表（错题记录、题目模式、教学类型关联、教学类型）

### 核心特性
- ✅ **动态路由**: 支持事件类型动态路由
- ✅ **热更新**: 支持配置热更新
- ✅ **故障隔离**: 支持故障隔离机制
- ✅ **监控告警**: 完整的监控和告警体系

## 📊 表结构设计

### 1. 源表：错题本事件源表
```sql
CREATE TABLE `wrongbook_event_source` (
  `domain` STRING COMMENT '业务域',
  `type` STRING COMMENT '事件类型',
  `timestamp` BIGINT COMMENT '事件时间戳',
  `eventId` STRING COMMENT '事件ID',
  `payload` STRING COMMENT '载荷数据(JSON)',
  `version` STRING COMMENT '数据版本',
  `source` STRING COMMENT '来源系统',
  `proc_time` AS PROCTIME() COMMENT '处理时间',
  `event_time` AS TO_TIMESTAMP_LTZ(`timestamp`, 3) COMMENT '事件时间',
  WATERMARK FOR `event_time` AS `event_time` - INTERVAL '5' SECOND
)
```

### 2. 维表配置

#### 维表1：错题记录表
- **数据库**: shuxue
- **表名**: wrong_question_record
- **缓存配置**: 10分钟TTL，10万行缓存
- **关联键**: id

#### 维表2：题目模式表
- **数据库**: tower
- **表名**: tower_pattern
- **缓存配置**: 30分钟TTL，10万行缓存
- **关联键**: id

#### 维表3：教学类型关联表
- **数据库**: tower
- **表名**: tower_teaching_type_pt
- **缓存配置**: 30分钟TTL，10万行缓存
- **关联键**: pt_id

#### 维表4：教学类型表
- **数据库**: tower
- **表名**: tower_teaching_type
- **缓存配置**: 30分钟TTL，10万行缓存
- **关联键**: id

### 3. 结果表：错题记录实时宽表
```sql
CREATE TABLE `dwd_wrong_record_wide_delta` (
  `id` BIGINT NOT NULL,
  `wrong_id` STRING,
  `user_id` STRING,
  `subject` STRING,
  `subject_name` STRING,
  `question_id` STRING,
  `question` STRING,
  `pattern_id` STRING,
  `pattern_name` STRING,
  `teach_type_id` STRING,
  `teach_type_name` STRING,
  `collect_time` TIMESTAMP(3),
  `fix_id` STRING,
  `fix_time` TIMESTAMP(3),
  `fix_result` BIGINT,
  `fix_result_desc` STRING,
  `event_id` STRING,
  `event_type` STRING,
  `process_time` TIMESTAMP(3),
  PRIMARY KEY (id) NOT ENFORCED
)
```

## 🔄 业务逻辑

### 事件类型处理

#### 1. 错题添加事件 (wrongbook_add)
```sql
-- 解析payload字段
JSON_VALUE(e.payload, '$.wrong_id') AS wrong_id,
JSON_VALUE(e.payload, '$.user_id') AS user_id,
JSON_VALUE(e.payload, '$.question_id') AS question_id,
JSON_VALUE(e.payload, '$.pattern_id') AS pattern_id,
JSON_VALUE(e.payload, '$.subject') AS subject,
JSON_VALUE(e.payload, '$.question') AS question,
JSON_VALUE(e.payload, '$.create_time') AS create_time
```

#### 2. 错题订正事件 (wrongbook_fix)
```sql
-- 解析payload字段
JSON_VALUE(e.payload, '$.wrong_id') AS wrong_id,
JSON_VALUE(e.payload, '$.user_id') AS user_id,
JSON_VALUE(e.payload, '$.question_id') AS question_id,
JSON_VALUE(e.payload, '$.pattern_id') AS pattern_id,
JSON_VALUE(e.payload, '$.fix_id') AS fix_id,
JSON_VALUE(e.payload, '$.fix_time') AS fix_time,
JSON_VALUE(e.payload, '$.fix_result') AS fix_result,
JSON_VALUE(e.payload, '$.fix_result_desc') AS fix_result_desc
```

### 维表关联逻辑
1. **错题记录表**: 通过 `wrong_id` 关联
2. **题目模式表**: 通过 `pattern_id` 关联
3. **教学类型关联表**: 通过 `pattern_id` 关联，过滤 `is_delete = 0`
4. **教学类型表**: 通过 `teaching_type_id` 关联，过滤 `is_delete = 0`

## 📈 监控和告警

### 监控视图

#### 1. 实时处理统计
```sql
CREATE VIEW `wrongbook_processing_stats` AS
SELECT 
    event_type,
    COUNT(*) AS event_count,
    COUNT(DISTINCT user_id) AS user_count,
    COUNT(DISTINCT wrong_id) AS wrong_count,
    TUMBLE_START(event_time, INTERVAL '5' MINUTE) AS window_start,
    TUMBLE_END(event_time, INTERVAL '5' MINUTE) AS window_end
FROM `wrongbook_event_source`
GROUP BY event_type, TUMBLE(event_time, INTERVAL '5' MINUTE);
```

#### 2. 订正成功率统计
```sql
CREATE VIEW `fix_success_rate` AS
SELECT 
    COUNT(CASE WHEN fix_result = 1 THEN 1 END) * 100.0 / COUNT(*) AS success_rate,
    COUNT(*) AS total_fixes,
    COUNT(CASE WHEN fix_result = 1 THEN 1 END) AS successful_fixes,
    TUMBLE_START(event_time, INTERVAL '10' MINUTE) AS window_start,
    TUMBLE_END(event_time, INTERVAL '10' MINUTE) AS window_end
FROM (
    SELECT 
        e.event_time,
        CAST(JSON_VALUE(e.payload, '$.fix_result') AS INT) AS fix_result
    FROM `wrongbook_event_source` e
    WHERE e.type = 'wrongbook_fix'
)
GROUP BY TUMBLE(event_time, INTERVAL '10' MINUTE);
```

### 告警配置
- **处理延迟告警**: 超过30秒触发告警
- **错误率告警**: 超过5%触发告警
- **维表缓存命中率告警**: 低于80%触发告警

## ⚙️ 作业配置

### 基础配置
```sql
SET 'table.dynamic-table-options.enabled' = 'true';
SET 'pipeline.name' = 'WrongbookWideTableSQLJob';
SET 'parallelism.default' = '4';
SET 'checkpoint.interval' = '60000';
```

### 性能优化配置
1. **并行度**: 4个并行度
2. **检查点间隔**: 60秒
3. **维表缓存**: 分层缓存策略
4. **输出批量**: 1000行/批次，2秒刷新间隔

## 🚀 部署指南

### 1. 环境准备
- **Flink版本**: 1.17.1
- **Java版本**: JDK 17
- **资源配置**: 4核8GB内存

### 2. 部署步骤
1. **创建Catalog**: 执行SQL文件中的表创建语句
2. **配置连接器**: 更新数据库连接信息
3. **启动作业**: 提交SQL作业到Flink集群
4. **监控验证**: 检查监控指标和告警

### 3. 扩展性配置
- **自动扩缩容**: 支持2-8个并行度自动调整
- **故障恢复**: 支持作业自动重启和状态恢复
- **配置热更新**: 支持运行时配置更新

## 📝 作业配置文件

### JSON配置示例
```json
{
  "job_name": "WrongbookWideTableJob",
  "description": "错题本系统实时宽表作业",
  "parallelism": 4,
  "checkpoint_interval": 60000,
  "tables": {
    "source_table": {
      "database": "wrongbook",
      "table": "wrongbook_event_source",
      "connector": "kafka",
      "properties": {
        "topic": "wrongbook-events",
        "bootstrap.servers": "localhost:9092"
      }
    },
    "dim_tables": [
      {
        "database": "shuxue",
        "table": "wrong_question_record",
        "connector": "jdbc",
        "join_key": "id"
      }
    ],
    "result_table": {
      "database": "shuxue",
      "table": "dwd_wrong_record_wide_delta",
      "connector": "jdbc"
    }
  },
  "business_logic": {
    "event_types": [
      {
        "type": "wrongbook_add",
        "description": "错题添加事件"
      },
      {
        "type": "wrongbook_fix",
        "description": "错题订正事件"
      }
    ]
  }
}
```

## 🔧 运维建议

### 1. 性能监控
- 监控事件处理延迟
- 监控维表查询性能
- 监控内存和CPU使用率

### 2. 数据质量
- 监控数据完整性
- 监控维表关联成功率
- 监控JSON解析错误率

### 3. 故障处理
- 配置自动重启策略
- 设置合理的重试次数
- 建立故障告警机制

## 📚 相关文档

- [Flink SQL作业文件](./sql/wrongbook_wide_table_hybrid.sql)
- [Payload数据结构说明](./错题本Payload数据结构说明.md)
- [Java处理器实现](../src/main/java/com/flink/realtime/processor/impl/)
- [AI作业生成器配置](../scripts/ai-job-generator.py)
