# 用户日统计作业域配置

## 📋 作业基本信息

```yaml
job_metadata:
  job_name: "user-daily-stats-job"
  job_type: "MULTI_DOMAIN"
  description: "用户日活跃度统计作业"
  version: "1.0.0"
  business_domains: ["wrongbook", "answer", "user"]
  business_goals: 
    - "用户日活跃度统计"
    - "跨域学习行为分析"
    - "用户画像数据生成"
  author: "数据开发团队"
  create_date: "2024-12-27"
```

## 🗄️ 事件源配置

### 多域事件源
```yaml
event_sources:
  # 错题本域事件
  - source_name: "wrongbook_events"
    event_domain: "wrongbook"
    topic_name: "biz_statistic_wrongbook"
    interested_event_types:
      - "wrongbook_fix"
    filter:
      basic_filters:
        domain: "wrongbook"
        type: "wrongbook_fix"
      custom_filters:
        - "JSON_VALUE(payload, '$.userId') IS NOT NULL"
    consumer:
      group_id: "user-daily-stats-wrongbook-consumer"
      
  # 答题域事件
  - source_name: "answer_events"
    event_domain: "answer"
    topic_name: "biz_statistic_answer"
    interested_event_types:
      - "answer_submit"
    filter:
      basic_filters:
        domain: "answer"
        type: "answer_submit"
      custom_filters:
        - "JSON_VALUE(payload, '$.userId') IS NOT NULL"
        - "JSON_VALUE(payload, '$.score') IS NOT NULL"
    consumer:
      group_id: "user-daily-stats-answer-consumer"
      
  # 用户域事件
  - source_name: "user_events"
    event_domain: "user"
    topic_name: "biz_statistic_user"
    interested_event_types:
      - "user_login"
    filter:
      basic_filters:
        domain: "user"
        type: "user_login"
      custom_filters:
        - "JSON_VALUE(payload, '$.userId') IS NOT NULL"
    consumer:
      group_id: "user-daily-stats-user-consumer"
```

## 🔗 维表配置

### 用户画像维表
```yaml
dim_tables:
  - table_name: "user_profile"
    table_alias: "up"
    connector: "jdbc"
    join_conditions:
      - event_source: "wrongbook_events"
        join_condition: "up.user_id = JSON_VALUE(wrongbook_events.payload, '$.userId')"
        join_type: "LEFT"
      - event_source: "answer_events"
        join_condition: "up.user_id = JSON_VALUE(answer_events.payload, '$.userId')"
        join_type: "LEFT"
      - event_source: "user_events"
        join_condition: "up.user_id = JSON_VALUE(user_events.payload, '$.userId')"
        join_type: "LEFT"
    cache:
      ttl: "1hour"
      max_rows: 500000
```

## 🎯 结果表配置

### 用户日统计表
```yaml
outputs:
  - output_name: "user-daily-stats"
    output_type: "TABLE"
    target_name: "dws_user_daily_stats"
    properties:
      connector: "odps"
      operation: "upsert"
```

## 🔄 字段映射配置

### 基础字段
- `user_id`: COALESCE(w.user_id, a.user_id, u.user_id) - 用户ID
- `stat_date`: DATE(COALESCE(w.event_time, a.event_time, u.event_time)) - 统计日期

### 错题本域指标
- `wrongbook_fix_count`: COUNT(w.domain) - 错题订正次数
- `fix_success_count`: COUNT(CASE WHEN JSON_VALUE(w.payload, '$.fixResult') = '1' THEN 1 END) - 订正成功次数
- `fix_success_rate`: fix_success_count / wrongbook_fix_count - 订正成功率

### 答题域指标
- `answer_submit_count`: COUNT(a.domain) - 答题提交次数
- `avg_score`: AVG(CAST(JSON_VALUE(a.payload, '$.score') AS DOUBLE)) - 平均得分
- `high_score_count`: COUNT(CASE WHEN CAST(JSON_VALUE(a.payload, '$.score') AS DOUBLE) >= 80 THEN 1 END) - 高分次数

### 用户域指标
- `login_count`: COUNT(u.domain) - 登录次数
- `first_login_time`: MIN(u.event_time) - 首次登录时间
- `last_login_time`: MAX(u.event_time) - 最后登录时间
- `online_duration`: TIMESTAMPDIFF(MINUTE, MIN(u.event_time), MAX(u.event_time)) - 在线时长(分钟)

### 综合指标
- `total_activity_count`: wrongbook_fix_count + answer_submit_count + login_count - 总活跃次数
- `learning_engagement_score`: (fix_success_rate * 0.4) + (avg_score/100 * 0.6) - 学习参与度得分

## 📊 业务逻辑配置

### 处理策略
```yaml
processing_strategy:
  processing_mode: "JOIN"  # 多域事件关联
  time_alignment_strategy: "EVENT_TIME"
  late_data:
    allowed_lateness: 300000  # 5分钟
    late_data_handling: "SIDE_OUTPUT"
```

### 跨域JOIN逻辑
```sql
-- 按用户ID和日期进行FULL OUTER JOIN
FROM wrongbook_events w
FULL OUTER JOIN answer_events a 
    ON w.user_id = a.user_id 
    AND DATE(w.event_time) = DATE(a.event_time)
FULL OUTER JOIN user_events u 
    ON COALESCE(w.user_id, a.user_id) = u.user_id 
    AND DATE(COALESCE(w.event_time, a.event_time)) = DATE(u.event_time)
WHERE COALESCE(w.user_id, a.user_id, u.user_id) IS NOT NULL
GROUP BY 
    COALESCE(w.user_id, a.user_id, u.user_id),
    DATE(COALESCE(w.event_time, a.event_time, u.event_time))
```

### 数据质量规则
```yaml
data_quality:
  required_fields:
    - "user_id"
    - "stat_date"
  completeness_threshold: 0.95
  business_rules:
    - "total_activity_count > 0"  # 必须有活跃行为
    - "stat_date >= CURRENT_DATE - INTERVAL 7 DAY"  # 只处理最近7天数据
```

## 📈 监控配置

### 业务指标
- 处理用户数 (daily_stats_users_processed_total)
- 跨域JOIN成功率 (cross_domain_join_success_rate)
- 数据延迟 (user_daily_stats_processing_delay)
- 活跃用户占比 (active_users_ratio)

### 告警阈值
- 跨域JOIN成功率 < 90%
- 处理延迟 P95 > 10分钟
- 活跃用户数异常波动 > 30%

## 💬 备注说明

### 特殊处理逻辑
- 使用FULL OUTER JOIN确保不遗漏任何域的用户活动
- 对于只在单个域活跃的用户，其他域指标为0
- 时间对齐基于EVENT_TIME，确保统计的准确性

### 扩展计划
- 支持用户行为序列分析
- 增加用户流失预警
- 支持个性化推荐数据准备
