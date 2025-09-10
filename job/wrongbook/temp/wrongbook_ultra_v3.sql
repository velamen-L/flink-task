-- ============================================================================
-- 错题本修正记录实时宽表作业 v3.0 (极简版生成)
-- 业务描述: 基于ultra-simple-sql-generator.mdc生成的完整Flink SQL作业
-- 生成时间: 2024-12-27
-- 输入配置: wrongbook-request-ultra.md
-- ============================================================================

-- ============================================================================
-- 源表定义 - BusinessEvent (自动配置Kafka连接器)
-- ============================================================================
CREATE TEMPORARY TABLE BusinessEvent (
    domain STRING COMMENT '业务域',
    type STRING COMMENT '事件类型',
    payload STRING COMMENT '错题修正载荷JSON',
    event_time TIMESTAMP(3) COMMENT '事件时间',
    processing_time AS PROCTIME(),
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = 'wrongbook-events',
    'properties.bootstrap.servers' = 'kafka-cluster:9092',
    'properties.group.id' = 'wrongbook-consumer-group',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'json'
);

-- ============================================================================
-- 维表定义 - tower_pattern (自动配置MySQL+TTL)
-- ============================================================================
CREATE TEMPORARY TABLE tower_pattern (
    id STRING NOT NULL COMMENT '题型ID',
    name STRING COMMENT '题型名称',
    subject STRING COMMENT '学科',
    difficulty DECIMAL(5,3) COMMENT '难度系数',
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.ttl' = '30 min',
    'lookup.cache.max-rows' = '100000',
    'url' = 'jdbc:mysql://mysql-host:3306/tower',
    'table-name' = 'tower_pattern',
    'username' = 'flink_user',
    'password' = 'flink_password'
);

-- ============================================================================
-- 维表定义 - tower_teaching_type_pt (自动配置MySQL+TTL)
-- ============================================================================
CREATE TEMPORARY TABLE tower_teaching_type_pt (
    id BIGINT NOT NULL COMMENT '关联表ID',
    teaching_type_id BIGINT COMMENT '教学类型ID',
    pt_id STRING COMMENT '题型ID',
    is_delete TINYINT COMMENT '删除标记',
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.ttl' = '30 min',
    'lookup.cache.max-rows' = '100000',
    'url' = 'jdbc:mysql://mysql-host:3306/tower',
    'table-name' = 'tower_teaching_type_pt',
    'username' = 'flink_user',
    'password' = 'flink_password'
);

-- ============================================================================
-- 维表定义 - tower_teaching_type (自动配置MySQL+TTL)
-- ============================================================================
CREATE TEMPORARY TABLE tower_teaching_type (
    id BIGINT NOT NULL COMMENT '教学类型ID',
    teaching_type_name STRING COMMENT '教学类型名称',
    chapter_id STRING COMMENT '章节ID',
    is_delete TINYINT COMMENT '删除标记',
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'lookup.cache.ttl' = '30 min',
    'lookup.cache.max-rows' = '100000',
    'url' = 'jdbc:mysql://mysql-host:3306/tower',
    'table-name' = 'tower_teaching_type',
    'username' = 'flink_user',
    'password' = 'flink_password'
);

-- ============================================================================
-- 结果表定义 - dwd_wrong_record_wide_delta (自动配置ODPS)
-- ============================================================================
CREATE TEMPORARY TABLE dwd_wrong_record_wide_delta (
    id BIGINT NOT NULL COMMENT '修正记录ID',
    wrong_id STRING COMMENT '错题记录ID',
    user_id STRING COMMENT '用户ID',
    subject STRING COMMENT '学科代码',
    subject_name STRING COMMENT '学科名称',
    question_id STRING COMMENT '题目ID',
    pattern_id STRING COMMENT '题型ID',
    pattern_name STRING COMMENT '题型名称',
    teaching_type_id STRING COMMENT '教学类型ID',
    teaching_type_name STRING COMMENT '教学类型名称',
    collect_time TIMESTAMP(3) COMMENT '错题收集时间',
    fix_id STRING COMMENT '修正ID',
    fix_time TIMESTAMP(3) COMMENT '修正时间',
    fix_result BIGINT COMMENT '修正结果',
    fix_result_desc STRING COMMENT '修正结果描述',
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'odps',
    'project' = 'flink_project',
    'tableName' = 'dwd_wrong_record_wide_delta',
    'accessId' = '${odps.access.id}',
    'accessKey' = '${odps.access.key}',
    'endpoint' = 'http://service.cn-hangzhou.maxcompute.aliyun.com/api',
    'sink.operation' = 'upsert'
);

-- ============================================================================
-- 主要业务逻辑SQL (基于关联关系和字段映射自动生成)
-- ============================================================================
INSERT INTO dwd_wrong_record_wide_delta
SELECT 
    -- 基础字段映射
    CAST(JSON_VALUE(be.payload, '$.fixId') AS BIGINT) AS id,
    JSON_VALUE(be.payload, '$.wrongId') AS wrong_id,
    JSON_VALUE(be.payload, '$.userId') AS user_id,
    JSON_VALUE(be.payload, '$.subject') AS subject,
    JSON_VALUE(be.payload, '$.questionId') AS question_id,
    JSON_VALUE(be.payload, '$.patternId') AS pattern_id,
    JSON_VALUE(be.payload, '$.fixId') AS fix_id,
    CAST(JSON_VALUE(be.payload, '$.fixResult') AS BIGINT) AS fix_result,
    
    -- 维表字段映射
    tp.name AS pattern_name,
    CAST(tt.id AS STRING) AS teaching_type_id,
    tt.teaching_type_name AS teaching_type_name,
    
    -- 计算字段
    CASE JSON_VALUE(be.payload, '$.subject')
        WHEN 'ENGLISH' THEN '英语'
        WHEN 'BIOLOGY' THEN '生物'
        WHEN 'MATH' THEN '数学'
        WHEN 'PHYSICS' THEN '物理'
        WHEN 'CHEMISTRY' THEN '化学'
        WHEN 'CHINESE' THEN '语文'
        ELSE ''
    END AS subject_name,
    
    CASE CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT)
        WHEN 1 THEN '订正'
        WHEN 0 THEN '未订正'
        ELSE ''
    END AS fix_result_desc,
    
    -- 时间字段转换
    TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT), 0) AS collect_time,
    TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT), 0) AS fix_time

FROM BusinessEvent be
-- 基于关联关系自动生成JOIN链
LEFT JOIN tower_pattern FOR SYSTEM_TIME AS OF be.processing_time tp
    ON tp.id = JSON_VALUE(be.payload, '$.patternId')
LEFT JOIN tower_teaching_type_pt FOR SYSTEM_TIME AS OF be.processing_time ttp
    ON ttp.pt_id = tp.id
    AND ttp.is_delete = 0
LEFT JOIN tower_teaching_type FOR SYSTEM_TIME AS OF be.processing_time tt
    ON tt.id = ttp.teaching_type_id
    AND tt.is_delete = 0
-- 自动生成域过滤条件
WHERE be.domain = 'wrongbook';

-- ============================================================================
-- 生成统计信息
-- 源表数量: 1 (BusinessEvent - Kafka自动配置)
-- 维表数量: 3 (tower_pattern, tower_teaching_type_pt, tower_teaching_type - MySQL+TTL自动配置)
-- 结果表数量: 1 (dwd_wrong_record_wide_delta - ODPS自动配置)
-- 字段映射数量: 16个字段
-- JOIN关联数量: 3个LEFT JOIN (基于relationships自动生成)
-- 连接器配置: 完全自动化 (源表→Kafka, 维表→MySQL+TTL, 结果表→ODPS)
-- 配置来源: wrongbook-request-ultra.md (极简版)
-- 生成器: ultra-simple-sql-generator.mdc v1.0
-- ============================================================================
