-- ============================================================================
-- Flink SQL作业: 用户日统计作业
-- 业务描述: 跨域用户日活跃度统计，整合错题本、答题、用户登录数据
-- 业务域: user-daily-stats (多域统计)
-- 生成时间: 2024-12-27
-- AI生成: intelligent-sql-job-generator.mdc v3.0
-- ============================================================================

-- 源表定义：错题本事件流
CREATE TEMPORARY TABLE wrongbook_events (
    domain STRING,
    type STRING,
    payload STRING,
    event_time TIMESTAMP(3),
    processing_time AS PROCTIME(),
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = 'biz_statistic_wrongbook',
    'properties.bootstrap.servers' = 'kafka-cluster:9092',
    'properties.group.id' = 'user-daily-stats-wrongbook-consumer',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'json'
);

-- 源表定义：答题事件流
CREATE TEMPORARY TABLE answer_events (
    domain STRING,
    type STRING,
    payload STRING,
    event_time TIMESTAMP(3),
    processing_time AS PROCTIME(),
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = 'biz_statistic_answer',
    'properties.bootstrap.servers' = 'kafka-cluster:9092',
    'properties.group.id' = 'user-daily-stats-answer-consumer',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'json'
);

-- 源表定义：用户事件流
CREATE TEMPORARY TABLE user_events (
    domain STRING,
    type STRING,
    payload STRING,
    event_time TIMESTAMP(3),
    processing_time AS PROCTIME(),
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = 'biz_statistic_user',
    'properties.bootstrap.servers' = 'kafka-cluster:9092',
    'properties.group.id' = 'user-daily-stats-user-consumer',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'json'
);

-- 维表定义：用户画像
CREATE TEMPORARY TABLE user_profile (
    user_id STRING NOT NULL,
    user_name STRING,
    grade STRING,
    city STRING,
    register_time TIMESTAMP(3),
    user_type STRING,
    PRIMARY KEY (user_id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'url' = 'jdbc:mysql://mysql-host:3306/user_db',
    'table-name' = 'user_profile',
    'username' = 'flink_user',
    'password' = 'flink_pass',
    'lookup.cache.max-rows' = '500000',
    'lookup.cache.ttl' = '1 hour'
);

-- 结果表定义：用户日统计表
CREATE TEMPORARY TABLE dws_user_daily_stats (
    user_id STRING NOT NULL,
    stat_date DATE NOT NULL,
    -- 错题本域指标
    wrongbook_fix_count BIGINT,
    fix_success_count BIGINT,
    fix_success_rate DOUBLE,
    -- 答题域指标
    answer_submit_count BIGINT,
    avg_score DOUBLE,
    high_score_count BIGINT,
    -- 用户域指标
    login_count BIGINT,
    first_login_time TIMESTAMP(3),
    last_login_time TIMESTAMP(3),
    online_duration BIGINT,
    -- 综合指标
    total_activity_count BIGINT,
    learning_engagement_score DOUBLE,
    -- 用户画像信息
    user_name STRING,
    grade STRING,
    city STRING,
    -- 时间戳
    update_time TIMESTAMP(3),
    PRIMARY KEY (user_id, stat_date) NOT ENFORCED
) WITH (
    'connector' = 'odps',
    'project' = 'flink_ai_project',
    'tableName' = 'dws_user_daily_stats',
    'sink.operation' = 'upsert'
);

-- ============================================================================
-- 主要业务逻辑SQL：跨域用户日统计
-- ============================================================================

INSERT INTO dws_user_daily_stats
SELECT 
    -- 基础字段
    COALESCE(w.user_id, a.user_id, u.user_id) AS user_id,
    COALESCE(w.stat_date, a.stat_date, u.stat_date) AS stat_date,
    
    -- 错题本域指标
    COALESCE(w.wrongbook_fix_count, 0) AS wrongbook_fix_count,
    COALESCE(w.fix_success_count, 0) AS fix_success_count,
    CASE 
        WHEN COALESCE(w.wrongbook_fix_count, 0) > 0 
        THEN CAST(COALESCE(w.fix_success_count, 0) AS DOUBLE) / CAST(w.wrongbook_fix_count AS DOUBLE)
        ELSE 0.0 
    END AS fix_success_rate,
    
    -- 答题域指标
    COALESCE(a.answer_submit_count, 0) AS answer_submit_count,
    COALESCE(a.avg_score, 0.0) AS avg_score,
    COALESCE(a.high_score_count, 0) AS high_score_count,
    
    -- 用户域指标
    COALESCE(u.login_count, 0) AS login_count,
    u.first_login_time,
    u.last_login_time,
    COALESCE(u.online_duration, 0) AS online_duration,
    
    -- 综合指标
    (COALESCE(w.wrongbook_fix_count, 0) + COALESCE(a.answer_submit_count, 0) + COALESCE(u.login_count, 0)) AS total_activity_count,
    (
        CASE 
            WHEN COALESCE(w.wrongbook_fix_count, 0) > 0 AND COALESCE(a.answer_submit_count, 0) > 0
            THEN (CAST(COALESCE(w.fix_success_count, 0) AS DOUBLE) / CAST(w.wrongbook_fix_count AS DOUBLE) * 0.4) + 
                 (COALESCE(a.avg_score, 0.0) / 100.0 * 0.6)
            WHEN COALESCE(w.wrongbook_fix_count, 0) > 0
            THEN CAST(COALESCE(w.fix_success_count, 0) AS DOUBLE) / CAST(w.wrongbook_fix_count AS DOUBLE) * 0.4
            WHEN COALESCE(a.answer_submit_count, 0) > 0
            THEN COALESCE(a.avg_score, 0.0) / 100.0 * 0.6
            ELSE 0.0
        END
    ) AS learning_engagement_score,
    
    -- 用户画像信息
    up.user_name,
    up.grade,
    up.city,
    
    -- 时间戳
    CURRENT_TIMESTAMP AS update_time

FROM (
    -- 错题本域日统计
    SELECT 
        JSON_VALUE(payload, '$.userId') AS user_id,
        DATE(event_time) AS stat_date,
        COUNT(*) AS wrongbook_fix_count,
        COUNT(CASE WHEN JSON_VALUE(payload, '$.fixResult') = '1' THEN 1 END) AS fix_success_count
    FROM wrongbook_events
    WHERE domain = 'wrongbook' 
      AND type = 'wrongbook_fix'
      AND JSON_VALUE(payload, '$.userId') IS NOT NULL
      AND event_time >= CURRENT_DATE - INTERVAL '7' DAY
    GROUP BY 
        JSON_VALUE(payload, '$.userId'),
        DATE(event_time)
) w
FULL OUTER JOIN (
    -- 答题域日统计
    SELECT 
        JSON_VALUE(payload, '$.userId') AS user_id,
        DATE(event_time) AS stat_date,
        COUNT(*) AS answer_submit_count,
        AVG(CAST(JSON_VALUE(payload, '$.score') AS DOUBLE)) AS avg_score,
        COUNT(CASE WHEN CAST(JSON_VALUE(payload, '$.score') AS DOUBLE) >= 80 THEN 1 END) AS high_score_count
    FROM answer_events
    WHERE domain = 'answer' 
      AND type = 'answer_submit'
      AND JSON_VALUE(payload, '$.userId') IS NOT NULL
      AND JSON_VALUE(payload, '$.score') IS NOT NULL
      AND event_time >= CURRENT_DATE - INTERVAL '7' DAY
    GROUP BY 
        JSON_VALUE(payload, '$.userId'),
        DATE(event_time)
) a ON w.user_id = a.user_id AND w.stat_date = a.stat_date
FULL OUTER JOIN (
    -- 用户域日统计
    SELECT 
        JSON_VALUE(payload, '$.userId') AS user_id,
        DATE(event_time) AS stat_date,
        COUNT(*) AS login_count,
        MIN(event_time) AS first_login_time,
        MAX(event_time) AS last_login_time,
        TIMESTAMPDIFF(MINUTE, MIN(event_time), MAX(event_time)) AS online_duration
    FROM user_events
    WHERE domain = 'user' 
      AND type = 'user_login'
      AND JSON_VALUE(payload, '$.userId') IS NOT NULL
      AND event_time >= CURRENT_DATE - INTERVAL '7' DAY
    GROUP BY 
        JSON_VALUE(payload, '$.userId'),
        DATE(event_time)
) u ON COALESCE(w.user_id, a.user_id) = u.user_id 
    AND COALESCE(w.stat_date, a.stat_date) = u.stat_date
-- 关联用户画像维表
LEFT JOIN user_profile FOR SYSTEM_TIME AS OF PROCTIME() up
    ON up.user_id = COALESCE(w.user_id, a.user_id, u.user_id)
WHERE COALESCE(w.user_id, a.user_id, u.user_id) IS NOT NULL
  AND (COALESCE(w.wrongbook_fix_count, 0) + COALESCE(a.answer_submit_count, 0) + COALESCE(u.login_count, 0)) > 0;

-- ============================================================================
-- 生成统计信息
-- 源表数量: 3 (wrongbook_events, answer_events, user_events)
-- 维表数量: 1 (user_profile)
-- 结果表数量: 1 (dws_user_daily_stats)
-- 字段映射数量: 15个核心指标字段
-- JOIN关联数量: 3个FULL OUTER JOIN + 1个LEFT JOIN
-- 特殊处理: 跨域事件关联、空值处理、综合指标计算
-- 性能优化: 维表缓存1小时，事件时间过滤，FULL OUTER JOIN确保数据完整性
-- ============================================================================
