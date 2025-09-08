# BusinessEvent 标准事件流表定义

## 📋 表基本信息

```yaml
metadata:
  table_name: "BusinessEvent"
  table_type: "source_stream"
  description: "统一的业务事件流表，所有业务域的事件都通过此表流入"
  version: "1.0"
  last_updated: "2024-12-27T15:30:00Z"
  maintained_by: "platform_team"
  usage_domains: ["wrongbook", "user_stats", "content", "payment"]
```

## 🏗️ 表结构定义

```sql
CREATE TABLE BusinessEvent (
    event_id STRING NOT NULL COMMENT '事件唯一标识',
    domain STRING NOT NULL COMMENT '业务域标识',
    type STRING NOT NULL COMMENT '事件类型',
    payload STRING NOT NULL COMMENT '事件负载JSON数据',
    event_time TIMESTAMP(3) NOT NULL COMMENT '事件发生时间',
    source STRING COMMENT '事件来源系统',
    version STRING COMMENT '事件模式版本',
    trace_id STRING COMMENT '链路追踪ID',
    
    -- 技术字段
    partition_key STRING COMMENT '分区键',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    
    PRIMARY KEY (event_id) NOT ENFORCED
) COMMENT '统一业务事件流表'
WITH (
    'connector' = 'kafka',
    'topic' = 'business-events',
    'properties.bootstrap.servers' = '${kafka.bootstrap.servers}',
    'properties.group.id' = '${kafka.group.id}',
    'format' = 'json',
    'scan.startup.mode' = 'latest-offset'
);
```

## 📊 字段详细说明

| 字段名 | 数据类型 | 约束 | 说明 | 示例值 |
|--------|----------|------|------|--------|
| `event_id` | STRING | NOT NULL, PK | 全局唯一的事件标识 | `evt_20241227_001` |
| `domain` | STRING | NOT NULL | 业务域，用于事件路由 | `wrongbook`, `user_stats` |
| `type` | STRING | NOT NULL | 具体的事件类型 | `wrongbook_fix`, `user_login` |
| `payload` | STRING | NOT NULL | JSON格式的事件数据 | `{"userId":"123","action":"fix"}` |
| `event_time` | TIMESTAMP(3) | NOT NULL | 业务时间（事件实际发生时间） | `2024-12-27 15:30:00.123` |
| `source` | STRING | NULL | 事件来源系统标识 | `mobile_app`, `web_portal` |
| `version` | STRING | NULL | Payload模式版本 | `v1.0`, `v2.1` |
| `trace_id` | STRING | NULL | 分布式链路追踪ID | `trace_abc123def456` |

## 🔗 Payload 结构规范

### 通用Payload约定
```yaml
payload_structure:
  common_fields:
    - name: "timestamp"
      type: "long"
      required: true
      description: "事件时间戳（毫秒）"
      
    - name: "userId"  
      type: "string"
      required: false
      description: "用户ID（如果事件与用户相关）"
      
    - name: "sessionId"
      type: "string" 
      required: false
      description: "会话ID"
      
    - name: "deviceId"
      type: "string"
      required: false
      description: "设备ID"
      
  business_fields:
    description: "各业务域自定义的业务字段"
    validation: "必须符合对应域的Payload Schema"
```

### 业务域Payload示例

#### 错题本域 (wrongbook)
```json
{
  "id": "fix_123456",
  "wrongId": "wrong_789012",
  "userId": "user_345678", 
  "subject": "MATH",
  "questionId": "q_456789",
  "patternId": "pattern_123",
  "createTime": 1703123456000,
  "submitTime": 1703123456789,
  "result": 1,
  "chapterId": "chapter_456",
  "isDelete": 0
}
```

#### 用户统计域 (user_stats)
```json
{
  "userId": "user_123456",
  "actionType": "page_view",
  "pagePath": "/dashboard",
  "duration": 30000,
  "timestamp": 1703123456789,
  "metadata": {
    "referrer": "https://example.com",
    "userAgent": "Mozilla/5.0..."
  }
}
```

## 🔄 事件过滤模式

### 标准过滤条件
```sql
-- 错题本事件过滤
WHERE domain = 'wrongbook' AND type = 'wrongbook_fix'

-- 用户行为事件过滤  
WHERE domain = 'user_stats' AND type IN ('page_view', 'user_action')

-- 时间范围过滤
WHERE event_time >= CURRENT_TIMESTAMP - INTERVAL '1' DAY
```

### 事件路由规则
```yaml
routing_rules:
  by_domain:
    wrongbook: 
      - wrongbook_fix
      - wrongbook_create
      - wrongbook_delete
    
    user_stats:
      - user_login
      - user_logout  
      - page_view
      - user_action
      
  by_priority:
    high: ["payment_success", "user_register"]
    normal: ["wrongbook_fix", "page_view"]
    low: ["debug_event", "metrics_collection"]
```

## 📈 使用统计和监控

### 关键指标
```yaml
monitoring_metrics:
  throughput:
    description: "每秒事件数"
    target: "> 1000 events/sec"
    alert_threshold: "< 100 events/sec"
    
  latency:
    description: "端到端延迟"
    target: "< 5 seconds (P95)"
    alert_threshold: "> 30 seconds"
    
  data_quality:
    description: "数据质量分数"
    target: "> 99%"
    components:
      - valid_json_payload: "> 99.5%"
      - required_fields_present: "> 99.8%"
      - domain_type_consistency: "> 99.9%"
```

### 数据治理
```yaml
data_governance:
  retention_policy:
    raw_events: "30 days"
    aggregated_metrics: "1 year"
    audit_logs: "3 years"
    
  privacy_controls:
    pii_fields: ["userId", "deviceId", "sessionId"]
    anonymization: "hash_with_salt"
    gdpr_compliance: true
    
  quality_controls:
    schema_validation: "strict"
    duplicate_detection: "enabled"
    anomaly_detection: "ml_based"
```

## 🔧 维护和优化

### 性能优化
```yaml
optimization_strategies:
  partitioning:
    strategy: "by_domain_and_hour"
    partition_keys: ["domain", "DATE_FORMAT(event_time, 'yyyy-MM-dd-HH')"]
    
  indexing:
    primary_index: "event_id"
    secondary_indexes: ["domain", "type", "event_time"]
    
  compression:
    payload_compression: "gzip"
    estimated_savings: "60-70%"
```

### 扩展性考虑
```yaml
scalability:
  horizontal_scaling:
    kafka_partitions: "auto_scale based on throughput"
    consumer_groups: "per_domain parallelism"
    
  schema_evolution:
    backward_compatibility: "required"
    version_management: "semantic_versioning"
    migration_strategy: "gradual_rollout"
```

---

## 📚 相关文档

- [事件驱动架构设计](../../../docs/event-driven-architecture.md)
- [Payload Schema管理](../domains/payload-schema-guide.md)
- [数据治理策略](../../../docs/data-governance.md)
- [监控和告警配置](../../../docs/monitoring-setup.md)

---

*此文档是ER知识库的核心组成部分，定义了所有业务域共享的标准事件流表结构*
