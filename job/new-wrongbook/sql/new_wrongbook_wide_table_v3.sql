-- ============================================================================
-- 新错题本增强版实时宽表作业 v3.0
-- 业务域: new-wrongbook
-- 作业类型: 增强版错题修正记录实时宽表处理
-- 创建时间: 2024-12-27
-- AI生成: intelligent-sql-job-generator.mdc v3.0
-- ============================================================================

-- 源表定义：BusinessEvent统一事件流
CREATE TEMPORARY TABLE BusinessEvent (
    domain STRING,
    type STRING,
    payload STRING,
    create_time BIGINT,
    processing_time AS PROCTIME()
) WITH (
    'connector' = 'kafka',
    'topic' = 'business-events',
    'properties.bootstrap.servers' = 'your-kafka-cluster:9092',
    'properties.group.id' = 'new-wrongbook-consumer',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'json'
);

-- 维表1：题型模式表 (增强版)
CREATE TEMPORARY TABLE tower_pattern (
    id STRING NOT NULL,
    name STRING,
    type INT,
    subject STRING,
    difficulty DECIMAL(5, 3),
    modify_time BIGINT,
    category STRING,
    skill_points STRING,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.max-rows' = '150000',
    'lookup.cache.ttl' = '45 min',
    'password' = '******',
    'table-name' = 'tower_pattern',
    'url' = 'jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower',
    'username' = 'zstt_server'
);

-- 维表2：教学类型关联表 (增强版)
CREATE TEMPORARY TABLE tower_teaching_type_pt (
    id BIGINT NOT NULL,
    teaching_type_id BIGINT,
    pt_id STRING,
    order_num INT,
    is_delete TINYINT,
    modify_time TIMESTAMP(3),
    weight DECIMAL(3,2),
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.max-rows' = '150000',
    'lookup.cache.ttl' = '45 min',
    'password' = '******',
    'table-name' = 'tower_teaching_type_pt',
    'url' = 'jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower',
    'username' = 'zstt_server'
);

-- 维表3：教学类型表 (增强版)
CREATE TEMPORARY TABLE tower_teaching_type (
    id BIGINT NOT NULL,
    chapter_id STRING,
    teaching_type_name STRING,
    is_delete TINYINT,
    modify_time TIMESTAMP(3),
    level INT,
    prerequisites STRING,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.max-rows' = '150000',
    'lookup.cache.ttl' = '45 min',
    'password' = '******',
    'table-name' = 'tower_teaching_type',
    'url' = 'jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower',
    'username' = 'zstt_server'
);

-- 结果表：增强版错题本错题记录实时宽表
CREATE TEMPORARY TABLE dwd_enhanced_wrong_record_wide_delta (
    fix_id STRING NOT NULL,
    wrong_id STRING,
    user_id STRING,
    subject STRING,
    subject_name STRING,
    question_id STRING,
    pattern_id STRING,
    pattern_name STRING,
    pattern_category STRING,
    teaching_type_id STRING,
    teaching_type_name STRING,
    teaching_level INT,
    fix_result INT,
    fix_result_desc STRING,
    confidence DOUBLE,
    attempt_count INT,
    learning_path STRING,
    recommendation STRING,
    question_difficulty DOUBLE,
    study_duration INT,
    create_time TIMESTAMP(3),
    fix_time TIMESTAMP(3),
    skill_points STRING,
    pattern_weight DECIMAL(3,2),
    is_mastered BOOLEAN,
    next_review_time TIMESTAMP(3),
    PRIMARY KEY (fix_id) NOT ENFORCED
) WITH (
    'accessId' = 'LTAI5tHvJUm7fEzCfrFT3oam',
    'accessKey' = '******',
    'connector' = 'odps',
    'enableUpsert' = 'true',
    'endpoint' = 'http://service.cn-hangzhou.maxcompute.aliyun.com/api',
    'project' = 'zstt',
    'sink.operation' = 'upsert',
    'tableName' = 'dwd_enhanced_wrong_record_wide_delta',
    'upsert.write.bucket.num' = '32'
);

-- ============================================================================
-- 主要数据处理SQL：增强版错题本实时宽表
-- ============================================================================

INSERT INTO dwd_enhanced_wrong_record_wide_delta
SELECT 
    -- 基础标识字段
    JSON_VALUE(be.payload, '$.id') AS fix_id,
    JSON_VALUE(be.payload, '$.wrong_id') AS wrong_id,
    JSON_VALUE(be.payload, '$.user_id') AS user_id,
    JSON_VALUE(be.payload, '$.subject') AS subject,
    
    -- 学科名称转换 (支持更多学科)
    CASE JSON_VALUE(be.payload, '$.subject')
        WHEN 'ENGLISH' THEN '英语'
        WHEN 'BIOLOGY' THEN '生物'
        WHEN 'MATH' THEN '数学'
        WHEN 'PHYSICS' THEN '物理'
        WHEN 'CHEMISTRY' THEN '化学'
        WHEN 'AOSHU' THEN '数学思维'
        WHEN 'SCIENCE' THEN '科学'
        WHEN 'CHINESE' THEN '语文'
        WHEN 'HISTORY' THEN '历史'
        WHEN 'GEOGRAPHY' THEN '地理'
        WHEN 'POLITICS' THEN '政治'
        ELSE '其他'
    END AS subject_name,
    
    -- 题目和题型信息
    JSON_VALUE(be.payload, '$.question_id') AS question_id,
    JSON_VALUE(be.payload, '$.pattern_id') AS pattern_id,
    COALESCE(pt.name, '未知题型') AS pattern_name,
    COALESCE(pt.category, '其他') AS pattern_category,
    
    -- 教学类型信息
    CAST(tt.id AS STRING) AS teaching_type_id,
    COALESCE(tt.teaching_type_name, '未分类') AS teaching_type_name,
    COALESCE(tt.level, 1) AS teaching_level,
    
    -- 修正结果信息
    CAST(JSON_VALUE(be.payload, '$.result') AS INT) AS fix_result,
    CASE CAST(JSON_VALUE(be.payload, '$.result') AS INT)
        WHEN 1 THEN '已订正'
        WHEN 0 THEN '未订正'
        WHEN 2 THEN '部分订正'
        WHEN 3 THEN '需要复习'
        ELSE '未知状态'
    END AS fix_result_desc,
    
    -- 新增智能分析字段
    COALESCE(CAST(JSON_VALUE(be.payload, '$.confidence') AS DOUBLE), 0.0) AS confidence,
    COALESCE(CAST(JSON_VALUE(be.payload, '$.attempt_count') AS INT), 1) AS attempt_count,
    COALESCE(JSON_VALUE(be.payload, '$.learning_path'), 'standard') AS learning_path,
    COALESCE(JSON_VALUE(be.payload, '$.recommendation'), '') AS recommendation,
    COALESCE(CAST(JSON_VALUE(be.payload, '$.difficulty') AS DOUBLE), 0.5) AS question_difficulty,
    COALESCE(CAST(JSON_VALUE(be.payload, '$.study_duration') AS INT), 0) AS study_duration,
    
    -- 时间字段
    TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.create_time') AS BIGINT), 0) AS create_time,
    TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.submit_time') AS BIGINT), 0) AS fix_time,
    
    -- 技能点和权重信息
    COALESCE(pt.skill_points, '') AS skill_points,
    COALESCE(ttp.weight, 1.0) AS pattern_weight,
    
    -- 智能掌握度评估
    CASE 
        WHEN CAST(JSON_VALUE(be.payload, '$.result') AS INT) = 1 
             AND COALESCE(CAST(JSON_VALUE(be.payload, '$.confidence') AS DOUBLE), 0.0) >= 0.8 
             AND COALESCE(CAST(JSON_VALUE(be.payload, '$.attempt_count') AS INT), 1) <= 2 THEN true
        WHEN CAST(JSON_VALUE(be.payload, '$.result') AS INT) = 1 
             AND COALESCE(CAST(JSON_VALUE(be.payload, '$.confidence') AS DOUBLE), 0.0) >= 0.6 
             AND COALESCE(CAST(JSON_VALUE(be.payload, '$.attempt_count') AS INT), 1) <= 3 THEN true
        ELSE false
    END AS is_mastered,
    
    -- 智能下次复习时间计算
    CASE 
        WHEN CAST(JSON_VALUE(be.payload, '$.result') AS INT) = 1 
             AND COALESCE(CAST(JSON_VALUE(be.payload, '$.confidence') AS DOUBLE), 0.0) >= 0.8 THEN 
            TIMESTAMPADD(DAY, 7, TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.submit_time') AS BIGINT), 0))
        WHEN CAST(JSON_VALUE(be.payload, '$.result') AS INT) = 1 
             AND COALESCE(CAST(JSON_VALUE(be.payload, '$.confidence') AS DOUBLE), 0.0) >= 0.6 THEN 
            TIMESTAMPADD(DAY, 3, TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.submit_time') AS BIGINT), 0))
        WHEN CAST(JSON_VALUE(be.payload, '$.result') AS INT) = 0 THEN 
            TIMESTAMPADD(DAY, 1, TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.submit_time') AS BIGINT), 0))
        ELSE 
            TIMESTAMPADD(DAY, 2, TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.submit_time') AS BIGINT), 0))
    END AS next_review_time

FROM BusinessEvent be
-- 关联题型信息表
LEFT JOIN tower_pattern FOR SYSTEM_TIME AS OF be.processing_time pt 
    ON pt.id = JSON_VALUE(be.payload, '$.pattern_id')
-- 关联教学类型关系表
LEFT JOIN tower_teaching_type_pt FOR SYSTEM_TIME AS OF be.processing_time ttp 
    ON ttp.pt_id = pt.id AND ttp.is_delete = 0
-- 关联教学类型主表
LEFT JOIN tower_teaching_type FOR SYSTEM_TIME AS OF be.processing_time tt 
    ON tt.id = ttp.teaching_type_id AND tt.is_delete = 0

WHERE 
    -- 事件过滤条件
    be.domain = 'new-wrongbook' 
    AND be.type = 'enhanced_wrongbook_fix'
    -- 数据质量过滤条件
    AND CAST(JSON_VALUE(be.payload, '$.isDelete') AS INT) = 0
    AND JSON_VALUE(be.payload, '$.user_id') IS NOT NULL
    AND JSON_VALUE(be.payload, '$.pattern_id') IS NOT NULL
    -- 语文英语科目章节匹配条件
    AND (JSON_VALUE(be.payload, '$.subject') NOT IN ('CHINESE', 'ENGLISH')
         OR (JSON_VALUE(be.payload, '$.subject') IN ('CHINESE', 'ENGLISH') 
             AND tt.chapter_id = JSON_VALUE(be.payload, '$.chapter_id')))
    -- 置信度验证条件
    AND (JSON_VALUE(be.payload, '$.confidence') IS NULL 
         OR (CAST(JSON_VALUE(be.payload, '$.confidence') AS DOUBLE) >= 0.0 
             AND CAST(JSON_VALUE(be.payload, '$.confidence') AS DOUBLE) <= 1.0))
    -- 难度值验证条件
    AND (JSON_VALUE(be.payload, '$.difficulty') IS NULL 
         OR (CAST(JSON_VALUE(be.payload, '$.difficulty') AS DOUBLE) >= 0.0 
             AND CAST(JSON_VALUE(be.payload, '$.difficulty') AS DOUBLE) <= 1.0))
    -- 性能优化条件
    AND (pt.id IS NOT NULL OR ttp.pt_id IS NOT NULL);

-- ============================================================================
-- SQL生成完成统计信息
-- ============================================================================
-- 源表数量: 1 (BusinessEvent)
-- 维表数量: 3 (tower_pattern, tower_teaching_type_pt, tower_teaching_type)
-- 结果表数量: 1 (dwd_enhanced_wrong_record_wide_delta)
-- 字段映射数量: 25个字段
-- 智能分析字段: 6个 (confidence, attempt_count, learning_path, recommendation, is_mastered, next_review_time)
-- JOIN关联数量: 3层关联
-- 业务规则数量: 7个验证条件
-- 性能优化: 维表缓存45分钟，150K行缓存，32个分桶
-- ============================================================================
