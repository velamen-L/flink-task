-- 错题本修正记录实时宽表 - 自动生成的Flink SQL
-- 基于 ultra-simple-sql-generator 规则生成

-- ============================================
-- 源表定义 (Kafka)
-- ============================================
CREATE TEMPORARY TABLE BusinessEvent (
    domain STRING,
    type STRING,
    payload STRING,
    event_time TIMESTAMP(3),
    processing_time AS PROCTIME(),
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = 'wrongbook-events',
    'properties.bootstrap.servers' = 'kafka-cluster:9092',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'json'
);

-- ============================================
-- 维表定义 (MySQL + TTL)
-- ============================================
CREATE TEMPORARY TABLE tower_pattern (
    id STRING NOT NULL,
    name STRING,
    subject STRING,
    difficulty DECIMAL(5,3),
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.ttl' = '30 min',
    'lookup.cache.max-rows' = '100000',
    'url' = 'jdbc:mysql://mysql-host:3306/db',
    'table-name' = 'tower_pattern'
);

CREATE TEMPORARY TABLE tower_teaching_type_pt (
    id BIGINT NOT NULL,
    teaching_type_id BIGINT,
    pt_id STRING,
    is_delete TINYINT,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.ttl' = '30 min',
    'lookup.cache.max-rows' = '100000',
    'url' = 'jdbc:mysql://mysql-host:3306/db',
    'table-name' = 'tower_teaching_type_pt'
);

CREATE TEMPORARY TABLE tower_teaching_type (
    id BIGINT NOT NULL,
    teaching_type_name STRING,
    chapter_id STRING,
    is_delete TINYINT,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.ttl' = '30 min',
    'lookup.cache.max-rows' = '100000',
    'url' = 'jdbc:mysql://mysql-host:3306/db',
    'table-name' = 'tower_teaching_type'
);

-- ============================================
-- 结果表定义 (ODPS)
-- ============================================
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
    collect_time TIMESTAMP(3),
    fix_time TIMESTAMP(3),
    learning_progress_score DECIMAL(10,2),
    subject_weakness_analysis STRING,
    pattern_mastery_index DECIMAL(5,3),
    study_efficiency_rating STRING,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'odps',
    'project' = 'project_name',
    'tableName' = 'dwd_wrong_record_wide_delta'
);

-- ============================================
-- 业务逻辑SQL
-- ============================================
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
    
    CASE CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT)
        WHEN 1 THEN '订正' 
        WHEN 0 THEN '未订正' 
        ELSE '' 
    END as fix_result_desc,
    
    -- 时间字段转换
    TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT), 0) as collect_time,
    TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT), 0) as fix_time,
    
    -- 智能指标字段 (基于描述生成SQL)
    -- 学习进度分数：根据用户错题修正的时间间隔和修正成功率，计算学习进度分数，体现学习效果的提升趋势
    CASE 
        WHEN CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) = 1 THEN
            CASE 
                WHEN (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 300000 THEN 90.0 + ((300000 - (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT))) / 10000.0)
                WHEN (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 600000 THEN 70.0 + ((600000 - (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT))) / 20000.0)
                WHEN (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 1800000 THEN 50.0
                ELSE 30.0
            END
        ELSE 0.0
    END as learning_progress_score,
    
    -- 学科薄弱点分析：基于用户在各学科的错题分布和修正情况，识别学科薄弱点并给出改进建议等级
    CASE 
        WHEN CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) = 0 THEN
            CASE JSON_VALUE(be.payload, '$.subject')
                WHEN 'MATH' THEN '数学-需要加强基础练习'
                WHEN 'ENGLISH' THEN '英语-需要词汇和语法强化'
                WHEN 'PHYSICS' THEN '物理-需要概念理解提升'
                WHEN 'CHEMISTRY' THEN '化学-需要实验和计算能力'
                WHEN 'BIOLOGY' THEN '生物-需要记忆和理解并重'
                WHEN 'CHINESE' THEN '语文-需要阅读和写作提升'
                ELSE '其他学科-需要针对性练习'
            END
        ELSE
            CASE JSON_VALUE(be.payload, '$.subject')
                WHEN 'MATH' THEN '数学-掌握良好'
                WHEN 'ENGLISH' THEN '英语-掌握良好'
                WHEN 'PHYSICS' THEN '物理-掌握良好'
                WHEN 'CHEMISTRY' THEN '化学-掌握良好'
                WHEN 'BIOLOGY' THEN '生物-掌握良好'
                WHEN 'CHINESE' THEN '语文-掌握良好'
                ELSE '其他学科-掌握良好'
            END
    END as subject_weakness_analysis,
    
    -- 题型掌握度指数：分析用户对特定题型的掌握程度，结合题型难度和修正历史，计算掌握度指数
    CASE 
        WHEN CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) = 1 THEN
            CASE 
                WHEN tp.difficulty <= 0.3 THEN 0.9
                WHEN tp.difficulty <= 0.6 THEN 0.7
                WHEN tp.difficulty <= 0.8 THEN 0.5
                ELSE 0.3
            END
        ELSE
            CASE 
                WHEN tp.difficulty <= 0.3 THEN 0.1
                WHEN tp.difficulty <= 0.6 THEN 0.2
                WHEN tp.difficulty <= 0.8 THEN 0.3
                ELSE 0.4
            END
    END as pattern_mastery_index,
    
    -- 学习效率评级：综合考虑修正时间、题型难度、学科分布，计算用户的学习效率评级
    CASE 
        WHEN CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) = 1 THEN
            CASE 
                WHEN (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 300000 AND tp.difficulty >= 0.7 THEN 'A级-高效掌握'
                WHEN (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 600000 AND tp.difficulty >= 0.5 THEN 'B级-良好掌握'
                WHEN (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 1800000 THEN 'C级-需要加强'
                ELSE 'D级-需要重点关注'
            END
        ELSE 'E级-未掌握'
    END as study_efficiency_rating

FROM BusinessEvent be
LEFT JOIN tower_pattern FOR SYSTEM_TIME AS OF be.processing_time tp
    ON tp.id = JSON_VALUE(be.payload, '$.patternId')
LEFT JOIN tower_teaching_type_pt FOR SYSTEM_TIME AS OF be.processing_time ttp
    ON ttp.pt_id = tp.id AND ttp.is_delete = 0
LEFT JOIN tower_teaching_type FOR SYSTEM_TIME AS OF be.processing_time tt
    ON tt.id = ttp.teaching_type_id AND tt.is_delete = 0
WHERE be.domain = 'wrongbook';