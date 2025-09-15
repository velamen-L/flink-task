-- 错题本修正记录实时宽表 Flink SQL
-- 基于 ultra-simple-sql-generator 规则生成

-- ==============================================
-- 源表定义 (Kafka)
-- ==============================================
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

-- ==============================================
-- 维表定义 (MySQL)
-- ==============================================

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

-- 教学类型关联表
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

-- 教学类型表
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

-- ==============================================
-- 结果表定义 (MySQL)
-- ==============================================
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
    history_fix_rate DECIMAL(5,2),
    chinese_fix_num INT,
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

-- ==============================================
-- 临时视图：实时统计语文科目订正数量
-- ==============================================
CREATE TEMPORARY VIEW chinese_fix_stats AS
SELECT 
    JSON_VALUE(payload, '$.userId') as user_id,
    COUNT(*) as chinese_fix_count
FROM BusinessEvent
WHERE domain = 'wrongbook'
    AND JSON_VALUE(payload, '$.subject') = 'CHINESE'
    AND DATE_FORMAT(processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')
GROUP BY JSON_VALUE(payload, '$.userId');

-- ==============================================
-- 临时视图：实时统计难度大于2的题目正确率
-- ==============================================
CREATE TEMPORARY VIEW difficult_fix_stats AS
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

-- ==============================================
-- 主业务逻辑SQL
-- ==============================================
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
    -- 学习进度分数：根据用户错题修正的时间间隔和修正成功率，计算学习进度分数，体现学习效果的提升趋势
    CASE 
        WHEN JSON_VALUE(be.payload, '$.fixResult') = '1' AND 
             (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 300000 THEN 
            90 + ((300000 - (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT))) / 10000)
        WHEN JSON_VALUE(be.payload, '$.fixResult') = '1' AND 
             (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 600000 THEN 
            70 + ((600000 - (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT))) / 20000)
        WHEN JSON_VALUE(be.payload, '$.fixResult') = '1' THEN 50
        ELSE 0
    END as learning_progress_score,
    
    -- 学科薄弱点分析：基于用户在各学科的错题分布和修正情况，识别学科薄弱点并给出改进建议等级
    CASE JSON_VALUE(be.payload, '$.subject')
        WHEN 'MATH' THEN 
            CASE JSON_VALUE(be.payload, '$.fixResult')
                WHEN '1' THEN '数学-已掌握'
                ELSE '数学-待提升'
            END
        WHEN 'ENGLISH' THEN 
            CASE JSON_VALUE(be.payload, '$.fixResult')
                WHEN '1' THEN '英语-已掌握'
                ELSE '英语-待提升'
            END
        WHEN 'CHINESE' THEN 
            CASE JSON_VALUE(be.payload, '$.fixResult')
                WHEN '1' THEN '语文-已掌握'
                ELSE '语文-待提升'
            END
        WHEN 'PHYSICS' THEN 
            CASE JSON_VALUE(be.payload, '$.fixResult')
                WHEN '1' THEN '物理-已掌握'
                ELSE '物理-待提升'
            END
        WHEN 'CHEMISTRY' THEN 
            CASE JSON_VALUE(be.payload, '$.fixResult')
                WHEN '1' THEN '化学-已掌握'
                ELSE '化学-待提升'
            END
        WHEN 'BIOLOGY' THEN 
            CASE JSON_VALUE(be.payload, '$.fixResult')
                WHEN '1' THEN '生物-已掌握'
                ELSE '生物-待提升'
            END
        ELSE '其他-待提升'
    END as subject_weakness_analysis,
    
    -- 题型掌握度指数：分析用户对特定题型的掌握程度，结合题型难度和修正历史，计算掌握度指数
    CASE 
        WHEN JSON_VALUE(be.payload, '$.fixResult') = '1' AND tp.difficulty IS NOT NULL THEN 
            ROUND((tp.difficulty * 20 + 80), 2)
        WHEN JSON_VALUE(be.payload, '$.fixResult') = '1' THEN 60.0
        ELSE 0.0
    END as pattern_mastery_index,
    
    -- 学习效率评级：综合考虑修正时间、题型难度、学科分布，计算用户的学习效率评级
    CASE 
        WHEN JSON_VALUE(be.payload, '$.fixResult') = '1' AND 
             (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 300000 AND 
             tp.difficulty > 2.0 THEN 'A级-高效'
        WHEN JSON_VALUE(be.payload, '$.fixResult') = '1' AND 
             (CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT) - CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT)) < 600000 THEN 'B级-良好'
        WHEN JSON_VALUE(be.payload, '$.fixResult') = '1' THEN 'C级-一般'
        ELSE 'D级-需改进'
    END as study_efficiency_rating,
    
    -- 历史修正正确率：获取七天内用户订正的正确率
    (SELECT COALESCE(
        ROUND(
            SUM(CASE WHEN JSON_VALUE(h.payload, '$.fixResult') = '1' THEN 1 ELSE 0 END) * 100.0 
            / NULLIF(COUNT(*), 0), 2
        ), 0
    )
    FROM BusinessEvent h 
    WHERE h.domain = 'wrongbook' 
        AND JSON_VALUE(h.payload, '$.userId') = JSON_VALUE(be.payload, '$.userId')
        AND h.processing_time >= be.processing_time - INTERVAL '7' DAY
        AND h.processing_time <= be.processing_time
    ) as history_fix_rate,
    
    -- 语文科目订正数量：根据payload.subject,实时统计当天语文科目的订正数量
    COALESCE(cfs.chinese_fix_count, 0) as chinese_fix_num,
    
    -- 难度题目正确率：根据payload.result和tower_pattern.difficulty，实时统计用户当天在难度大于2的题目上的答题正确率
    COALESCE(dfs.difficult_fix_rate, 0.0) as difficult_fix_rate

FROM BusinessEvent be
LEFT JOIN tower_pattern tp FOR SYSTEM_TIME AS OF be.processing_time
    ON tp.id = JSON_VALUE(be.payload, '$.patternId')
LEFT JOIN tower_teaching_type_pt ttp FOR SYSTEM_TIME AS OF be.processing_time
    ON ttp.pt_id = tp.id AND ttp.is_delete = 0
LEFT JOIN tower_teaching_type tt FOR SYSTEM_TIME AS OF be.processing_time
    ON tt.id = ttp.teaching_type_id AND tt.is_delete = 0
LEFT JOIN chinese_fix_stats cfs
    ON cfs.user_id = JSON_VALUE(be.payload, '$.userId')
LEFT JOIN difficult_fix_stats dfs
    ON dfs.user_id = JSON_VALUE(be.payload, '$.userId')
WHERE be.domain = 'wrongbook';