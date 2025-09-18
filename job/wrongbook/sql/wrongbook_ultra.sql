-- 错题本修正记录实时宽表 Flink SQL
-- 基于 ultra-simple-sql-generator 规则生成

-- ===========================================
-- 源表定义 (Kafka)
-- ===========================================
CREATE TEMPORARY TABLE BusinessEvent (
    domain STRING,
    type STRING,
    payload STRING,
    event_time STRING,
    processing_time AS PROCTIME()
) WITH (
    'connector' = 'kafka',
    'properties.bootstrap.servers' = 'alikafka-post-cn-gh6439cb1005-1-vpc.alikafka.aliyuncs.com:9092,alikafka-post-cn-gh6439cb1005-2-vpc.alikafka.aliyuncs.com:9092,alikafka-post-cn-gh6439cb1005-3-vpc.alikafka.aliyuncs.com:9092',
    'properties.group.id' = 'flink-business-test',
    'topic' = 'biz_statistic_wrongbook-test',
    'format' = 'json',
    'scan.startup.mode' = 'latest-offset',
    'json.timestamp-format.standard' = 'ISO-8601'
);

-- ===========================================
-- 维表定义 (MySQL)
-- ===========================================

-- 题型维表
CREATE TEMPORARY TABLE tower_pattern (
    id STRING NOT NULL,
    name STRING,
    subject STRING,
    difficulty DECIMAL(5,3),
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'mysql',
    'database-name' = 'tower',
    'hostname' = 'rm-bp1543eg312q7x4n3.mysql.rds.aliyuncs.com',
    'lookup.cache.max-rows' = '100000',
    'lookup.cache.strategy' = 'LRU',
    'lookup.cache.ttl' = '30 minutes',
    'lookup.max-retries' = '3',
    'password' = 'vGcvUh7wbGREWucW6LR0',
    'port' = '3306',
    'table-name' = 'tower_pattern',
    'username' = 'app_rw'
);

-- 教学类型关联维表
CREATE TEMPORARY TABLE tower_teaching_type_pt (
    id BIGINT NOT NULL,
    teaching_type_id BIGINT,
    pt_id STRING,
    is_delete TINYINT,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'mysql',
    'database-name' = 'tower',
    'hostname' = 'rm-bp1543eg312q7x4n3.mysql.rds.aliyuncs.com',
    'lookup.cache.max-rows' = '100000',
    'lookup.cache.strategy' = 'LRU',
    'lookup.cache.ttl' = '30 minutes',
    'lookup.max-retries' = '3',
    'password' = 'vGcvUh7wbGREWucW6LR0',
    'port' = '3306',
    'table-name' = 'tower_teaching_type_pt',
    'username' = 'app_rw'
);

-- 教学类型维表
CREATE TEMPORARY TABLE tower_teaching_type (
    id BIGINT NOT NULL,
    teaching_type_name STRING,
    chapter_id STRING,
    is_delete TINYINT,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'mysql',
    'database-name' = 'tower',
    'hostname' = 'rm-bp1543eg312q7x4n3.mysql.rds.aliyuncs.com',
    'lookup.cache.max-rows' = '100000',
    'lookup.cache.strategy' = 'LRU',
    'lookup.cache.ttl' = '10 minutes',
    'lookup.max-retries' = '3',
    'password' = 'vGcvUh7wbGREWucW6LR0',
    'port' = '3306',
    'table-name' = 'tower_teaching_type',
    'username' = 'app_rw'
);

-- ===========================================
-- 结果表定义 (MySQL)
-- ===========================================
CREATE TEMPORARY TABLE dwd_wrong_record_wide_delta (
    id STRING NOT NULL,
    wrong_id STRING,
    user_id STRING,
    subject STRING,
    question_id STRING,
    pattern_id STRING,
    fix_id STRING,
    fix_result INT,
    pattern_name STRING,
    teaching_type_id BIGINT,
    teaching_type_name STRING,
    subject_name STRING,
    fix_result_desc STRING,
    collect_time BIGINT,
    fix_time BIGINT,
    chinese_fix_num BIGINT,
    difficult_fix_rate DECIMAL(5,2),
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'mysql',
    'database-name' = 'guarder',
    'hostname' = 'rm-bp1543eg312q7x4n3.mysql.rds.aliyuncs.com',
    'password' = 'vGcvUh7wbGREWucW6LR0',
    'port' = '3306',
    'table-name' = 'dwd_wrong_record_wide_delta',
    'username' = 'app_rw'
);

-- ===========================================
-- 智能指标临时视图定义
-- ===========================================

-- 语文科目订正数量统计 (当天)
CREATE TEMPORARY VIEW chinese_fix_num_stats AS
SELECT 
    JSON_VALUE(payload, '$.userId') as user_id,
    COUNT(*) as chinese_fix_num
FROM BusinessEvent
WHERE domain = 'wrongbook'
    AND JSON_VALUE(payload, '$.subject') = 'CHINESE'
    AND DATE_FORMAT(processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')
GROUP BY JSON_VALUE(payload, '$.userId');

-- 难度题目正确率统计 (当天)
CREATE TEMPORARY VIEW difficult_fix_rate_stats AS
SELECT 
    JSON_VALUE(be.payload, '$.userId') as user_id,
    CASE 
        WHEN COUNT(*) > 0 
        THEN ROUND(SUM(CASE WHEN JSON_VALUE(be.payload, '$.fixResult') = '1' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2)
        ELSE 0 
    END as difficult_fix_rate
FROM BusinessEvent be
LEFT JOIN tower_pattern tp FOR SYSTEM_TIME AS OF be.processing_time
    ON tp.id = JSON_VALUE(be.payload, '$.patternId')
WHERE be.domain = 'wrongbook'
    AND tp.difficulty > 2.0
    AND DATE_FORMAT(be.processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')
GROUP BY JSON_VALUE(be.payload, '$.userId');

-- ===========================================
-- 业务逻辑SQL
-- ===========================================
INSERT INTO dwd_wrong_record_wide_delta
SELECT 
    -- 基础字段映射
    JSON_VALUE(be.payload, '$.fixId') as id,
    JSON_VALUE(be.payload, '$.wrongId') as wrong_id,
    JSON_VALUE(be.payload, '$.userId') as user_id,
    JSON_VALUE(be.payload, '$.subject') as subject,
    JSON_VALUE(be.payload, '$.questionId') as question_id,
    JSON_VALUE(be.payload, '$.patternId') as pattern_id,
    JSON_VALUE(be.payload, '$.fixId') as fix_id,
    CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) as fix_result,
    
    -- 维表字段映射
    tp.name as pattern_name,
    tt.id as teaching_type_id,
    tt.teaching_type_name as teaching_type_name,
    
    -- 计算字段
    CASE JSON_VALUE(be.payload, '$.subject') 
        WHEN 'ENGLISH' THEN '英语' 
        WHEN 'BIOLOGY' THEN '生物' 
        WHEN 'MATH' THEN '数学' 
        WHEN 'PHYSICS' THEN '物理' 
        WHEN 'CHEMISTRY' THEN '化学' 
        WHEN 'CHINESE' THEN '语文' 
        ELSE '' 
    END as subject_name,
    
    CASE JSON_VALUE(be.payload, '$.fixResult') 
        WHEN '1' THEN '订正' 
        WHEN '0' THEN '未订正' 
        ELSE '' 
    END as fix_result_desc,
    
    -- 时间字段转换
    CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT) as collect_time,
    CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) as fix_time,
    
    -- 智能指标字段
    COALESCE(cfn.chinese_fix_num, 0) as chinese_fix_num,
    COALESCE(dfr.difficult_fix_rate, 0) as difficult_fix_rate

FROM BusinessEvent be
-- 关联题型维表
LEFT JOIN tower_pattern tp FOR SYSTEM_TIME AS OF be.processing_time
    ON tp.id = JSON_VALUE(be.payload, '$.patternId')
-- 关联教学类型关联维表
LEFT JOIN tower_teaching_type_pt ttp FOR SYSTEM_TIME AS OF be.processing_time
    ON ttp.pt_id = JSON_VALUE(be.payload, '$.patternId')
    AND ttp.is_delete = 0
-- 关联教学类型维表
LEFT JOIN tower_teaching_type tt FOR SYSTEM_TIME AS OF be.processing_time
    ON tt.id = ttp.teaching_type_id
    AND tt.is_delete = 0
-- 关联语文订正数量统计
LEFT JOIN chinese_fix_num_stats cfn
    ON cfn.user_id = JSON_VALUE(be.payload, '$.userId')
-- 关联难度题目正确率统计
LEFT JOIN difficult_fix_rate_stats dfr
    ON dfr.user_id = JSON_VALUE(be.payload, '$.userId')
WHERE be.domain = 'wrongbook';