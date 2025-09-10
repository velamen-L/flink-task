-- ============================================================================
-- 错题本修正记录实时宽表作业 v3.0 (简化版生成)
-- 业务描述: 基于simple-sql-generator.mdc生成的完整Flink SQL作业
-- 生成时间: 2024-12-27
-- 输入配置: wrongbook-request-simple.md
-- ============================================================================

-- ============================================================================
-- 源表定义 - BusinessEvent统一事件流
-- ============================================================================
CREATE TEMPORARY TABLE source_events (
    domain STRING,
    type STRING,
    payload STRING,
    event_time TIMESTAMP(3),
    processing_time AS PROCTIME(),
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = 'business-events',
    'properties.bootstrap.servers' = 'kafka-cluster:9092',
    'properties.group.id' = 'wrongbook-consumer-group',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'json'
);

-- ============================================================================
-- 维表定义 - tower_pattern题型模式表
-- ============================================================================
CREATE TEMPORARY TABLE tower_pattern (
    id STRING NOT NULL,
    name STRING,
    type INT,
    subject STRING,
    difficulty DECIMAL(5,3),
    modify_time BIGINT,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.ttl' = '30 min',
    'lookup.cache.max-rows' = '100000',
    'url' = 'jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower',
    'table-name' = 'tower_pattern',
    'username' = 'zstt_server',
    'password' = '******'
);

-- ============================================================================
-- 维表定义 - tower_teaching_type_pt教学类型模式关联表
-- ============================================================================
CREATE TEMPORARY TABLE tower_teaching_type_pt (
    id BIGINT NOT NULL,
    teaching_type_id BIGINT,
    pt_id STRING,
    order_num INT,
    is_delete TINYINT,
    modify_time TIMESTAMP(3),
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.ttl' = '30 min',
    'lookup.cache.max-rows' = '100000',
    'url' = 'jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower',
    'table-name' = 'tower_teaching_type_pt',
    'username' = 'zstt_server',
    'password' = '******'
);

-- ============================================================================
-- 维表定义 - tower_teaching_type教学类型表
-- ============================================================================
CREATE TEMPORARY TABLE tower_teaching_type (
    id BIGINT NOT NULL,
    chapter_id STRING,
    teaching_type_name STRING,
    is_delete TINYINT,
    modify_time TIMESTAMP(3),
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.ttl' = '30 min',
    'lookup.cache.max-rows' = '100000',
    'url' = 'jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower',
    'table-name' = 'tower_teaching_type',
    'username' = 'zstt_server',
    'password' = '******'
);

-- ============================================================================
-- 结果表定义 - dwd_wrong_record_wide_delta错题本实时宽表
-- ============================================================================
CREATE TEMPORARY TABLE dwd_wrong_record_wide_delta (
    id BIGINT NOT NULL,
    wrong_id STRING,
    user_id STRING,
    subject STRING,
    subject_name STRING,
    question_id STRING,
    question STRING,
    pattern_id STRING,
    pattern_name STRING,
    teaching_type_id STRING,
    teaching_type_name STRING,
    collect_time TIMESTAMP(3),
    fix_id STRING,
    fix_time TIMESTAMP(3),
    fix_result BIGINT,
    fix_result_desc STRING,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'odps',
    'project' = 'zstt',
    'tableName' = 'dwd_wrong_record_wide_delta',
    'accessId' = 'LTAI5tHvJUm7fEzCfrFT3oam',
    'accessKey' = '******',
    'endpoint' = 'http://service.cn-hangzhou.maxcompute.aliyun.com/api',
    'enableUpsert' = 'true',
    'sink.operation' = 'upsert',
    'upsert.write.bucket.num' = '16'
);

-- ============================================================================
-- 主要业务逻辑SQL - 基于字段映射配置自动生成
-- ============================================================================
INSERT INTO dwd_wrong_record_wide_delta
SELECT 
    -- 基础字段 - 直接从payload映射
    CAST(JSON_VALUE(se.payload, '$.fixId') AS BIGINT) AS id,
    JSON_VALUE(se.payload, '$.wrongId') AS wrong_id,
    JSON_VALUE(se.payload, '$.userId') AS user_id,
    JSON_VALUE(se.payload, '$.subject') AS subject,
    JSON_VALUE(se.payload, '$.questionId') AS question_id,
    JSON_VALUE(se.payload, '$.patternId') AS pattern_id,
    JSON_VALUE(se.payload, '$.fixId') AS fix_id,
    CAST(JSON_VALUE(se.payload, '$.fixResult') AS BIGINT) AS fix_result,
    
    -- 维表字段映射
    pt.name AS pattern_name,
    CAST(tt.id AS STRING) AS teaching_type_id,
    tt.teaching_type_name AS teaching_type_name,
    
    -- 计算字段
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
    END AS subject_name,
    
    CAST(NULL AS STRING) AS question,
    
    CASE CAST(JSON_VALUE(se.payload, '$.fixResult') AS INT)
        WHEN 1 THEN '订正'
        WHEN 0 THEN '未订正'
        ELSE ''
    END AS fix_result_desc,
    
    -- 时间字段转换
    TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(se.payload, '$.createTime') AS BIGINT), 0) AS collect_time,
    TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(se.payload, '$.submitTime') AS BIGINT), 0) AS fix_time

FROM source_events se
-- 关联题型模式表
LEFT JOIN tower_pattern FOR SYSTEM_TIME AS OF se.processing_time pt
    ON pt.id = JSON_VALUE(se.payload, '$.patternId')
-- 关联教学类型模式关联表
LEFT JOIN tower_teaching_type_pt FOR SYSTEM_TIME AS OF se.processing_time ttp
    ON ttp.pt_id = pt.id
    AND ttp.is_delete = 0
-- 关联教学类型表
LEFT JOIN tower_teaching_type FOR SYSTEM_TIME AS OF se.processing_time tt
    ON tt.id = ttp.teaching_type_id
    AND tt.is_delete = 0
WHERE se.domain = 'wrongbook'
  AND se.type = 'wrongbook_fix';

-- ============================================================================
-- 生成统计信息
-- 源表数量: 1 (source_events - BusinessEvent统一事件流)
-- 维表数量: 3 (tower_pattern, tower_teaching_type_pt, tower_teaching_type)
-- 结果表数量: 1 (dwd_wrong_record_wide_delta)
-- 字段映射数量: 16个字段
-- JOIN关联数量: 3个LEFT JOIN (源表→维表链式关联)
-- 性能优化: 维表缓存30分钟，FOR SYSTEM_TIME AS OF优化，upsert操作
-- 配置来源: wrongbook-request-simple.md
-- 生成器: simple-sql-generator.mdc v1.0
-- ============================================================================
