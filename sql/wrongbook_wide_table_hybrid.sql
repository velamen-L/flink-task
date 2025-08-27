-- ================================================================
-- 错题本实时宽表 FlinkSQL 作业 - 混合架构版本
-- 功能：基于统一Kafka Topic接收错题本事件，关联维表构建实时宽表
-- 架构：混合架构（统一Topic + 事件类型路由）
-- 输出：MySQL数据库
-- 作者：AI代码生成器
-- 创建时间：2024
-- ================================================================

-- 源表：错题本事件统一Topic
CREATE TABLE `wrongbook_event_source` (
  `domain` STRING COMMENT '业务域',
  `type` STRING COMMENT '事件类型',
  `timestamp` BIGINT COMMENT '事件时间戳',
  `eventId` STRING COMMENT '事件ID',
  `payload` STRING COMMENT '载荷数据(JSON)',
  `version` STRING COMMENT '数据版本',
  `source` STRING COMMENT '来源系统',
  `proc_time` AS PROCTIME() COMMENT '处理时间',
  `event_time` AS TO_TIMESTAMP_LTZ(`timestamp`, 3) COMMENT '事件时间',
  WATERMARK FOR `event_time` AS `event_time` - INTERVAL '5' SECOND
)
COMMENT '错题本事件源表'
WITH (
  'connector' = 'kafka',
  'topic' = 'wrongbook-events',
  'properties.bootstrap.servers' = 'localhost:9092',
  'properties.group.id' = 'wrongbook-sql-consumer',
  'scan.startup.mode' = 'latest-offset',
  'format' = 'json',
  'json.fail-on-missing-field' = 'false',
  'json.ignore-parse-errors' = 'true'
);

-- 维表1：错题记录表（静态维表查询）
CREATE TABLE `wrong_question_record` (
  `id` STRING NOT NULL,
  `user_id` STRING,
  `question_id` STRING,
  `pattern_id` STRING,
  `subject` STRING,
  `chapter_id` STRING,
  `chapter_name` STRING,
  `study_stage` STRING,
  `course_type` STRING,
  `answer_record_id` STRING,
  `answer_image` STRING,
  `result` TINYINT,
  `correct_status` TINYINT,
  `origin` STRING,
  `tag_group` STRING,
  `draft_image` STRING,
  `q_type` INT,
  `zpd_pattern_id` STRING,
  `create_time` BIGINT,
  `submit_time` BIGINT,
  `is_delete` BOOLEAN,
  PRIMARY KEY (id) NOT ENFORCED
)
WITH (
  'connector' = 'jdbc',
  'lookup.cache.caching-missing-key' = 'false',
  'lookup.cache.max-rows' = '100000',
  'lookup.cache.ttl' = '10 min',
  'lookup.max-retries' = '3',
  'password' = '******',
  'table-name' = 'wrong_question_record',
  'url' = 'jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/shuxue',
  'username' = 'zstt_server'
);

-- 维表2：题目模式表
CREATE TABLE `tower_pattern` (
  `id` STRING NOT NULL,
  `name` STRING,
  `type` INT,
  `subject` STRING,
  `difficulty` DECIMAL(5, 3),
  `modify_time` BIGINT,
  PRIMARY KEY (id) NOT ENFORCED
)
WITH (
  'connector' = 'jdbc',
  'lookup.cache.max-rows' = '100000',
  'lookup.cache.ttl' = '30 min',
  'password' = '******',
  'table-name' = 'tower_pattern',
  'url' = 'jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower',
  'username' = 'zstt_server'
);

-- 维表3：教学类型与题目模式关联表
CREATE TABLE `tower_teaching_type_pt` (
  `id` BIGINT NOT NULL,
  `teaching_type_id` BIGINT,
  `pt_id` STRING,
  `order_num` INT,
  `is_delete` TINYINT,
  `modify_time` TIMESTAMP(3),
  PRIMARY KEY (id) NOT ENFORCED
)
WITH (
  'connector' = 'jdbc',
  'lookup.cache.max-rows' = '100000',
  'lookup.cache.ttl' = '30 min',
  'password' = '******',
  'table-name' = 'tower_teaching_type_pt',
  'url' = 'jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower',
  'username' = 'zstt_server'
);

-- 维表4：教学类型表
CREATE TABLE `tower_teaching_type` (
  `id` BIGINT NOT NULL,
  `chapter_id` STRING,
  `teaching_type_name` STRING,
  `is_delete` TINYINT,
  `modify_time` TIMESTAMP(3),
  PRIMARY KEY (id) NOT ENFORCED
)
WITH (
  'connector' = 'jdbc',
  'lookup.cache.max-rows' = '100000',
  'lookup.cache.ttl' = '30 min',
  'password' = '******',
  'table-name' = 'tower_teaching_type',
  'url' = 'jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/tower',
  'username' = 'zstt_server'
);

-- 结果表：错题记录实时宽表（MySQL）
CREATE TABLE `dwd_wrong_record_wide_delta` (
  `id` BIGINT NOT NULL,
  `wrong_id` STRING,
  `user_id` STRING,
  `subject` STRING,
  `subject_name` STRING,
  `question_id` STRING,
  `question` STRING,
  `pattern_id` STRING,
  `pattern_name` STRING,
  `teach_type_id` STRING,
  `teach_type_name` STRING,
  `collect_time` TIMESTAMP(3),
  `fix_id` STRING,
  `fix_time` TIMESTAMP(3),
  `fix_result` BIGINT,
  `fix_result_desc` STRING,
  `event_id` STRING,
  `event_type` STRING,
  `process_time` TIMESTAMP(3),
  PRIMARY KEY (id) NOT ENFORCED
)
COMMENT '错题本错题记录实时宽表'
WITH (
  'connector' = 'jdbc',
  'url' = 'jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/shuxue',
  'table-name' = 'dwd_wrong_record_wide_delta',
  'username' = 'zstt_server',
  'password' = '******',
  'sink.buffer-flush.max-rows' = '1000',
  'sink.buffer-flush.interval' = '2s',
  'sink.max-retries' = '3'
);

-- ================================================================
-- 主要业务逻辑查询 - 支持错题添加和订正事件
-- ================================================================

-- 创建处理错题添加事件的视图
CREATE TEMPORARY VIEW `wrongbook_add_events` AS
SELECT 
    s.eventId,
    s.domain,
    s.type,
    s.timestamp,
    s.event_time,
    s.proc_time,
    JSON_VALUE(s.payload, '$.userId') as user_id,
    JSON_VALUE(s.payload, '$.questionId') as question_id,
    JSON_VALUE(s.payload, '$.wrongRecordId') as wrong_record_id,
    JSON_VALUE(s.payload, '$.wrongTimes') as wrong_times,
    JSON_VALUE(s.payload, '$.wrongTime') as wrong_time,
    JSON_VALUE(s.payload, '$.sessionId') as session_id
FROM `wrongbook_event_source` s
WHERE s.domain = 'wrongbook' AND s.type = 'wrongbook_add';

-- 创建处理错题订正事件的视图
CREATE TEMPORARY VIEW `wrongbook_fix_events` AS
SELECT 
    s.eventId,
    s.domain,
    s.type,
    s.timestamp,
    s.event_time,
    s.proc_time,
    JSON_VALUE(s.payload, '$.userId') as user_id,
    JSON_VALUE(s.payload, '$.questionId') as question_id,
    JSON_VALUE(s.payload, '$.wrongRecordId') as wrong_record_id,
    JSON_VALUE(s.payload, '$.fixTime') as fix_time,
    JSON_VALUE(s.payload, '$.fixResult') as fix_result,
    JSON_VALUE(s.payload, '$.attempts') as attempts,
    JSON_VALUE(s.payload, '$.timeCost') as time_cost
FROM `wrongbook_event_source` s
WHERE s.domain = 'wrongbook' AND s.type = 'wrongbook_fix';

-- 处理错题添加事件：插入新记录
INSERT INTO `dwd_wrong_record_wide_delta`
SELECT 
    -- 生成唯一ID：使用事件ID的hash值
    CAST(HASH_CODE(e.eventId) AS BIGINT) as id,
    
    -- 错题基础信息
    COALESCE(e.wrong_record_id, e.eventId) as wrong_id,
    e.user_id,
    COALESCE(wr.subject, '') as subject,
    CASE 
        WHEN wr.subject = 'math' THEN '数学'
        WHEN wr.subject = 'chinese' THEN '语文'
        WHEN wr.subject = 'english' THEN '英语'
        ELSE COALESCE(wr.subject, '未知')
    END as subject_name,
    e.question_id,
    COALESCE(e.question_id, '') as question,
    COALESCE(wr.pattern_id, '') as pattern_id,
    COALESCE(tp.name, '') as pattern_name,
    
    -- 教学类型信息
    CAST(COALESCE(ttpt.teaching_type_id, 0) AS STRING) as teach_type_id,
    COALESCE(tt.teaching_type_name, '') as teach_type_name,
    
    -- 错题收集时间
    CASE 
        WHEN e.wrong_time IS NOT NULL AND e.wrong_time != '' 
        THEN TO_TIMESTAMP_LTZ(CAST(e.wrong_time AS BIGINT), 3)
        ELSE e.event_time 
    END as collect_time,
    
    -- 订正信息（错题添加时为空）
    '' as fix_id,
    CAST(NULL AS TIMESTAMP(3)) as fix_time,
    0 as fix_result,
    '未订正' as fix_result_desc,
    
    -- 事件元信息
    e.eventId as event_id,
    e.type as event_type,
    e.proc_time as process_time

FROM `wrongbook_add_events` e
-- 关联错题记录表
LEFT JOIN `wrong_question_record` FOR SYSTEM_TIME AS OF e.proc_time AS wr
    ON e.wrong_record_id = wr.id
-- 关联题目模式表
LEFT JOIN `tower_pattern` FOR SYSTEM_TIME AS OF e.proc_time AS tp
    ON wr.pattern_id = tp.id
-- 关联教学类型关系表
LEFT JOIN `tower_teaching_type_pt` FOR SYSTEM_TIME AS OF e.proc_time AS ttpt
    ON tp.id = ttpt.pt_id AND ttpt.is_delete = 0
-- 关联教学类型表
LEFT JOIN `tower_teaching_type` FOR SYSTEM_TIME AS OF e.proc_time AS tt
    ON ttpt.teaching_type_id = tt.id AND tt.is_delete = 0;

-- 处理错题订正事件：更新已有记录或插入新记录（如果原记录不存在）
INSERT INTO `dwd_wrong_record_wide_delta`
SELECT 
    -- 生成唯一ID：使用错题记录ID或事件ID的hash值
    CAST(HASH_CODE(COALESCE(e.wrong_record_id, e.eventId)) AS BIGINT) as id,
    
    -- 错题基础信息
    COALESCE(e.wrong_record_id, e.eventId) as wrong_id,
    e.user_id,
    COALESCE(wr.subject, '') as subject,
    CASE 
        WHEN wr.subject = 'math' THEN '数学'
        WHEN wr.subject = 'chinese' THEN '语文'
        WHEN wr.subject = 'english' THEN '英语'
        ELSE COALESCE(wr.subject, '未知')
    END as subject_name,
    e.question_id,
    COALESCE(e.question_id, '') as question,
    COALESCE(wr.pattern_id, '') as pattern_id,
    COALESCE(tp.name, '') as pattern_name,
    
    -- 教学类型信息
    CAST(COALESCE(ttpt.teaching_type_id, 0) AS STRING) as teach_type_id,
    COALESCE(tt.teaching_type_name, '') as teach_type_name,
    
    -- 错题收集时间（如果有原记录用原记录时间，否则用当前事件时间）
    CASE 
        WHEN wr.create_time IS NOT NULL 
        THEN TO_TIMESTAMP_LTZ(wr.create_time, 3)
        ELSE e.event_time 
    END as collect_time,
    
    -- 订正信息
    e.eventId as fix_id,
    CASE 
        WHEN e.fix_time IS NOT NULL AND e.fix_time != '' 
        THEN TO_TIMESTAMP_LTZ(CAST(e.fix_time AS BIGINT), 3)
        ELSE e.event_time 
    END as fix_time,
    CASE 
        WHEN e.fix_result = 'correct' THEN 1
        WHEN e.fix_result = 'incorrect' THEN 0
        ELSE 0
    END as fix_result,
    CASE 
        WHEN e.fix_result = 'correct' THEN '订正正确'
        WHEN e.fix_result = 'incorrect' THEN '订正错误'
        ELSE '订正未知'
    END as fix_result_desc,
    
    -- 事件元信息
    e.eventId as event_id,
    e.type as event_type,
    e.proc_time as process_time

FROM `wrongbook_fix_events` e
-- 关联错题记录表
LEFT JOIN `wrong_question_record` FOR SYSTEM_TIME AS OF e.proc_time AS wr
    ON e.wrong_record_id = wr.id
-- 关联题目模式表
LEFT JOIN `tower_pattern` FOR SYSTEM_TIME AS OF e.proc_time AS tp
    ON wr.pattern_id = tp.id
-- 关联教学类型关系表
LEFT JOIN `tower_teaching_type_pt` FOR SYSTEM_TIME AS OF e.proc_time AS ttpt
    ON tp.id = ttpt.pt_id AND ttpt.is_delete = 0
-- 关联教学类型表
LEFT JOIN `tower_teaching_type` FOR SYSTEM_TIME AS OF e.proc_time AS tt
    ON ttpt.teaching_type_id = tt.id AND tt.is_delete = 0;
