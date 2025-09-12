-- 错题本修正记录实时宽表 Flink SQL
-- 基于 ultra-simple-sql-generator 规则生成

-- 源表定义 (Kafka)
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

-- 维表定义 (MySQL) - tower_pattern
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

-- 维表定义 (MySQL) - tower_teaching_type_pt
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

-- 维表定义 (MySQL) - tower_teaching_type
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

-- 结果表定义 (MySQL)
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
    learning_progress_score DECIMAL(10,2),
    subject_weakness_analysis STRING,
    pattern_mastery_index DECIMAL(10,2),
    study_efficiency_rating STRING,
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

-- 业务逻辑SQL
INSERT INTO dwd_wrong_record_wide_delta
SELECT 
    JSON_VALUE(be.payload, '$.fixId') as id,
    JSON_VALUE(be.payload, '$.wrongId') as wrong_id,
    JSON_VALUE(be.payload, '$.userId') as user_id,
    JSON_VALUE(be.payload, '$.subject') as subject,
    JSON_VALUE(be.payload, '$.questionId') as question_id,
    JSON_VALUE(be.payload, '$.patternId') as pattern_id,
    JSON_VALUE(be.payload, '$.fixId') as fix_id,
    CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) as fix_result,
    tp.name as pattern_name,
    tt.id as teaching_type_id,
    tt.teaching_type_name as teaching_type_name,
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
    CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT) as collect_time,
    CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) as fix_time,
    -- 学习进度分数：根据修正时间间隔和成功率计算
    CASE 
        WHEN CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) = 1 THEN
            CASE 
                WHEN (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 300000 THEN 90.0 + (300000 - (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT))) / 10000.0
                WHEN (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 600000 THEN 70.0 + (600000 - (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT))) / 20000.0
                ELSE 50.0
            END
        ELSE 0.0
    END as learning_progress_score,
    -- 学科薄弱点分析：基于学科和修正结果给出改进建议等级
    CASE 
        WHEN CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) = 1 THEN
            CASE JSON_VALUE(be.payload, '$.subject')
                WHEN 'MATH' THEN '数学-已掌握'
                WHEN 'ENGLISH' THEN '英语-已掌握'
                WHEN 'PHYSICS' THEN '物理-已掌握'
                WHEN 'CHEMISTRY' THEN '化学-已掌握'
                WHEN 'BIOLOGY' THEN '生物-已掌握'
                WHEN 'CHINESE' THEN '语文-已掌握'
                ELSE '其他-已掌握'
            END
        ELSE 
            CASE JSON_VALUE(be.payload, '$.subject')
                WHEN 'MATH' THEN '数学-待提升'
                WHEN 'ENGLISH' THEN '英语-待提升'
                WHEN 'PHYSICS' THEN '物理-待提升'
                WHEN 'CHEMISTRY' THEN '化学-待提升'
                WHEN 'BIOLOGY' THEN '生物-待提升'
                WHEN 'CHINESE' THEN '语文-待提升'
                ELSE '其他-待提升'
            END
    END as subject_weakness_analysis,
    -- 题型掌握度指数：结合题型难度和修正历史计算
    CASE 
        WHEN CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) = 1 THEN
            CASE 
                WHEN tp.difficulty >= 0.8 THEN 85.0
                WHEN tp.difficulty >= 0.6 THEN 75.0
                WHEN tp.difficulty >= 0.4 THEN 65.0
                ELSE 55.0
            END
        ELSE
            CASE 
                WHEN tp.difficulty >= 0.8 THEN 15.0
                WHEN tp.difficulty >= 0.6 THEN 25.0
                WHEN tp.difficulty >= 0.4 THEN 35.0
                ELSE 45.0
            END
    END as pattern_mastery_index,
    -- 学习效率评级：综合考虑修正时间、题型难度、学科分布
    CASE 
        WHEN CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) = 1 AND (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 300000 AND tp.difficulty >= 0.6 THEN '高效学习'
        WHEN CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) = 1 AND (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 600000 THEN '良好学习'
        WHEN CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) = 1 THEN '一般学习'
        ELSE '需要改进'
    END as study_efficiency_rating
FROM BusinessEvent be
LEFT JOIN tower_pattern FOR SYSTEM_TIME AS OF be.processing_time tp
    ON tp.id = JSON_VALUE(be.payload, '$.patternId')
LEFT JOIN tower_teaching_type_pt FOR SYSTEM_TIME AS OF be.processing_time ttp
    ON ttp.pt_id = tp.id AND ttp.is_delete = 0
LEFT JOIN tower_teaching_type FOR SYSTEM_TIME AS OF be.processing_time tt
    ON tt.id = ttp.teaching_type_id AND tt.is_delete = 0
WHERE be.domain = 'wrongbook';