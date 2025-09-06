-- =====================================================
-- 错题本系统实时宽表作业 - Flink SQL版本
-- 基于智能作业生成器生成
-- 作者: AI代码生成器
-- 日期: 2025-08-29
-- =====================================================

-- 设置作业配置
SET 'table.dynamic-table-options.enabled' = 'true';
SET 'pipeline.name' = 'WrongbookWideTableSQLJob';
SET 'parallelism.default' = '4';
SET 'checkpoint.interval' = '60000';

-- =====================================================
-- 1. 源表：错题本订正事件表
-- =====================================================
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

-- =====================================================
-- 2. 维表1：错题记录表（静态维表查询）
-- =====================================================
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

-- =====================================================
-- 3. 维表2：题目模式表
-- =====================================================
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

-- =====================================================
-- 4. 维表3：教学类型与题目模式关联表
-- =====================================================
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

-- =====================================================
-- 5. 维表4：教学类型表
-- =====================================================
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

-- =====================================================
-- 6. 结果表：错题记录实时宽表（MySQL）
-- =====================================================
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

-- =====================================================
-- 7. 数据处理SQL - 错题添加事件处理
-- =====================================================
INSERT INTO `dwd_wrong_record_wide_delta`
SELECT 
    -- 主键ID（使用wrong_id的hash值）
    CAST(ABS(HASH(wrong_id)) AS BIGINT) AS id,
    
    -- 基础字段
    wrong_id,
    user_id,
    subject,
    -- 科目中文名称映射
    CASE 
        WHEN subject = 'math' OR subject = 'shuxue' THEN '数学'
        WHEN subject = 'chinese' OR subject = 'yuwen' THEN '语文'
        WHEN subject = 'english' OR subject = 'yingyu' THEN '英语'
        WHEN subject = 'physics' OR subject = 'wuli' THEN '物理'
        WHEN subject = 'chemistry' OR subject = 'huaxue' THEN '化学'
        ELSE subject
    END AS subject_name,
    question_id,
    question,
    pattern_id,
    pattern_name,
    teach_type_id,
    teach_type_name,
    
    -- 错题添加事件特有字段
    TO_TIMESTAMP_LTZ(create_time, 3) AS collect_time,
    NULL AS fix_id,
    NULL AS fix_time,
    NULL AS fix_result,
    NULL AS fix_result_desc,
    
    -- 事件信息
    eventId AS event_id,
    type AS event_type,
    PROCTIME() AS process_time
    
FROM (
    -- 解析错题添加事件的payload
    SELECT 
        e.eventId,
        e.type,
        e.timestamp,
        -- 解析JSON payload
        JSON_VALUE(e.payload, '$.wrong_id') AS wrong_id,
        JSON_VALUE(e.payload, '$.user_id') AS user_id,
        JSON_VALUE(e.payload, '$.question_id') AS question_id,
        JSON_VALUE(e.payload, '$.pattern_id') AS pattern_id,
        JSON_VALUE(e.payload, '$.subject') AS subject,
        JSON_VALUE(e.payload, '$.question') AS question,
        JSON_VALUE(e.payload, '$.create_time') AS create_time
    FROM `wrongbook_event_source` e
    WHERE e.type = 'wrongbook_add'
) add_events
-- 关联错题记录维表
LEFT JOIN `wrong_question_record` wqr ON add_events.wrong_id = wqr.id
-- 关联题目模式维表
LEFT JOIN `tower_pattern` tp ON add_events.pattern_id = tp.id
-- 关联教学类型维表（通过pattern_id关联）
LEFT JOIN `tower_teaching_type_pt` ttp ON add_events.pattern_id = ttp.pt_id AND ttp.is_delete = 0
LEFT JOIN `tower_teaching_type` ttt ON ttp.teaching_type_id = ttt.id AND ttt.is_delete = 0;

-- =====================================================
-- 8. 数据处理SQL - 错题订正事件处理
-- =====================================================
INSERT INTO `dwd_wrong_record_wide_delta`
SELECT 
    -- 主键ID（使用wrong_id的hash值）
    CAST(ABS(HASH(wrong_id)) AS BIGINT) AS id,
    
    -- 基础字段（从维表获取）
    wrong_id,
    user_id,
    wqr.subject,
    -- 科目中文名称映射
    CASE 
        WHEN wqr.subject = 'math' OR wqr.subject = 'shuxue' THEN '数学'
        WHEN wqr.subject = 'chinese' OR wqr.subject = 'yuwen' THEN '语文'
        WHEN wqr.subject = 'english' OR wqr.subject = 'yingyu' THEN '英语'
        WHEN wqr.subject = 'physics' OR wqr.subject = 'wuli' THEN '物理'
        WHEN wqr.subject = 'chemistry' OR wqr.subject = 'huaxue' THEN '化学'
        ELSE wqr.subject
    END AS subject_name,
    question_id,
    wqr.question,
    pattern_id,
    tp.name AS pattern_name,
    CAST(ttt.id AS STRING) AS teach_type_id,
    ttt.teaching_type_name,
    
    -- 错题订正事件特有字段
    TO_TIMESTAMP_LTZ(wqr.create_time, 3) AS collect_time,
    fix_id,
    TO_TIMESTAMP_LTZ(fix_time, 3) AS fix_time,
    CAST(fix_result AS BIGINT) AS fix_result,
    fix_result_desc,
    
    -- 事件信息
    eventId AS event_id,
    type AS event_type,
    PROCTIME() AS process_time
    
FROM (
    -- 解析错题订正事件的payload
    SELECT 
        e.eventId,
        e.type,
        e.timestamp,
        -- 解析JSON payload
        JSON_VALUE(e.payload, '$.wrong_id') AS wrong_id,
        JSON_VALUE(e.payload, '$.user_id') AS user_id,
        JSON_VALUE(e.payload, '$.question_id') AS question_id,
        JSON_VALUE(e.payload, '$.pattern_id') AS pattern_id,
        JSON_VALUE(e.payload, '$.fix_id') AS fix_id,
        JSON_VALUE(e.payload, '$.fix_time') AS fix_time,
        JSON_VALUE(e.payload, '$.fix_result') AS fix_result,
        JSON_VALUE(e.payload, '$.fix_result_desc') AS fix_result_desc
    FROM `wrongbook_event_source` e
    WHERE e.type = 'wrongbook_fix'
) fix_events
-- 关联错题记录维表（获取基础信息）
LEFT JOIN `wrong_question_record` wqr ON fix_events.wrong_id = wqr.id
-- 关联题目模式维表
LEFT JOIN `tower_pattern` tp ON fix_events.pattern_id = tp.id
-- 关联教学类型维表（通过pattern_id关联）
LEFT JOIN `tower_teaching_type_pt` ttp ON fix_events.pattern_id = ttp.pt_id AND ttp.is_delete = 0
LEFT JOIN `tower_teaching_type` ttt ON ttp.teaching_type_id = ttt.id AND ttt.is_delete = 0;

-- =====================================================
-- 9. 监控和告警视图
-- =====================================================

-- 实时处理统计视图
CREATE VIEW `wrongbook_processing_stats` AS
SELECT 
    event_type,
    COUNT(*) AS event_count,
    COUNT(DISTINCT user_id) AS user_count,
    COUNT(DISTINCT wrong_id) AS wrong_count,
    TUMBLE_START(event_time, INTERVAL '5' MINUTE) AS window_start,
    TUMBLE_END(event_time, INTERVAL '5' MINUTE) AS window_end
FROM `wrongbook_event_source`
GROUP BY event_type, TUMBLE(event_time, INTERVAL '5' MINUTE);

-- 错题订正成功率统计
CREATE VIEW `fix_success_rate` AS
SELECT 
    COUNT(CASE WHEN fix_result = 1 THEN 1 END) * 100.0 / COUNT(*) AS success_rate,
    COUNT(*) AS total_fixes,
    COUNT(CASE WHEN fix_result = 1 THEN 1 END) AS successful_fixes,
    TUMBLE_START(event_time, INTERVAL '10' MINUTE) AS window_start,
    TUMBLE_END(event_time, INTERVAL '10' MINUTE) AS window_end
FROM (
    SELECT 
        e.event_time,
        CAST(JSON_VALUE(e.payload, '$.fix_result') AS INT) AS fix_result
    FROM `wrongbook_event_source` e
    WHERE e.type = 'wrongbook_fix'
)
GROUP BY TUMBLE(event_time, INTERVAL '10' MINUTE);

-- =====================================================
-- 10. 作业配置说明
-- =====================================================
/*
作业配置参数：
- parallelism.default: 4 (并行度)
- checkpoint.interval: 60000 (检查点间隔60秒)
- table.dynamic-table-options.enabled: true (启用动态表选项)

性能优化建议：
1. 维表缓存配置：
   - wrong_question_record: 10分钟TTL，10万行缓存
   - tower_pattern: 30分钟TTL，10万行缓存
   - 教学类型相关表: 30分钟TTL，10万行缓存

2. 输出配置：
   - 批量写入：1000行/批次
   - 刷新间隔：2秒
   - 重试次数：3次

3. 监控指标：
   - 事件处理统计：5分钟窗口
   - 订正成功率：10分钟窗口
   - 实时告警：处理延迟、错误率

4. 扩展性考虑：
   - 支持动态路由配置
   - 支持热更新处理器
   - 支持故障隔离机制
*/
