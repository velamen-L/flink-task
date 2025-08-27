-- ================================================================
-- 错题本实时宽表 FlinkSQL 作业 - 阿里云Catalog版本
-- 功能：基于阿里云Catalog配置，无需手动定义表结构
-- 架构：动态路由 + 热部署 + Catalog集成
-- 输出：MySQL数据库
-- 作者：AI代码生成器
-- 创建时间：2024
-- ================================================================

-- 设置作业配置
SET 'execution.checkpointing.interval' = '10s';
SET 'execution.checkpointing.mode' = 'EXACTLY_ONCE';
SET 'table.exec.state.ttl' = '1d';

-- ================================================================
-- 使用阿里云Catalog中已配置的表，无需重复定义表结构
-- 假设Catalog中已配置：
-- - wrongbook_event_source (Kafka源表)
-- - tower_pattern (题目模式维表)
-- - tower_teaching_type_pt (教学类型关联维表)  
-- - tower_teaching_type (教学类型维表)
-- - dwd_wrong_record_wide_delta (MySQL结果表)
-- ================================================================

-- 创建处理错题添加事件的临时视图
CREATE TEMPORARY VIEW wrongbook_add_events AS
SELECT 
    eventId,
    domain,
    type,
    timestamp,
    TO_TIMESTAMP_LTZ(timestamp, 3) as event_time,
    PROCTIME() as proc_time,
    -- 从payload中提取业务字段（Topic中已包含完整数据）
    JSON_VALUE(payload, '$.userId') as user_id,
    JSON_VALUE(payload, '$.questionId') as question_id,
    JSON_VALUE(payload, '$.wrongRecordId') as wrong_record_id,
    JSON_VALUE(payload, '$.subject') as subject,
    JSON_VALUE(payload, '$.subjectName') as subject_name,
    JSON_VALUE(payload, '$.patternId') as pattern_id,
    JSON_VALUE(payload, '$.chapterId') as chapter_id,
    JSON_VALUE(payload, '$.chapterName') as chapter_name,
    JSON_VALUE(payload, '$.wrongTimes') as wrong_times,
    JSON_VALUE(payload, '$.wrongTime') as wrong_time,
    JSON_VALUE(payload, '$.sessionId') as session_id,
    JSON_VALUE(payload, '$.createTime') as create_time
FROM wrongbook_event_source 
WHERE domain = 'wrongbook' 
  AND type = 'wrongbook_add';

-- 创建处理错题订正事件的临时视图
CREATE TEMPORARY VIEW wrongbook_fix_events AS
SELECT 
    eventId,
    domain,
    type,
    timestamp,
    TO_TIMESTAMP_LTZ(timestamp, 3) as event_time,
    PROCTIME() as proc_time,
    -- 从payload中提取业务字段（Topic中已包含完整数据）
    JSON_VALUE(payload, '$.userId') as user_id,
    JSON_VALUE(payload, '$.questionId') as question_id,
    JSON_VALUE(payload, '$.wrongRecordId') as wrong_record_id,
    JSON_VALUE(payload, '$.subject') as subject,
    JSON_VALUE(payload, '$.subjectName') as subject_name,
    JSON_VALUE(payload, '$.patternId') as pattern_id,
    JSON_VALUE(payload, '$.chapterId') as chapter_id,
    JSON_VALUE(payload, '$.chapterName') as chapter_name,
    JSON_VALUE(payload, '$.fixTime') as fix_time,
    JSON_VALUE(payload, '$.fixResult') as fix_result,
    JSON_VALUE(payload, '$.attempts') as attempts,
    JSON_VALUE(payload, '$.timeCost') as time_cost,
    JSON_VALUE(payload, '$.createTime') as create_time
FROM wrongbook_event_source 
WHERE domain = 'wrongbook' 
  AND type = 'wrongbook_fix';

-- ================================================================
-- 主要业务逻辑：处理错题添加事件
-- ================================================================
INSERT INTO dwd_wrong_record_wide_delta
SELECT 
    -- 生成唯一ID
    CAST(HASH_CODE(COALESCE(e.wrong_record_id, e.eventId)) AS BIGINT) as id,
    
    -- 错题基础信息（从Topic中直接获取，无需关联wrong_question_record）
    COALESCE(e.wrong_record_id, e.eventId) as wrong_id,
    e.user_id,
    COALESCE(e.subject, '') as subject,
    COALESCE(e.subject_name, 
        CASE 
            WHEN e.subject = 'math' THEN '数学'
            WHEN e.subject = 'chinese' THEN '语文'
            WHEN e.subject = 'english' THEN '英语'
            ELSE COALESCE(e.subject, '未知')
        END
    ) as subject_name,
    e.question_id,
    COALESCE(e.question_id, '') as question,
    COALESCE(e.pattern_id, '') as pattern_id,
    COALESCE(tp.name, '') as pattern_name,
    
    -- 教学类型信息（通过维表关联获取）
    CAST(COALESCE(ttpt.teaching_type_id, 0) AS STRING) as teach_type_id,
    COALESCE(tt.teaching_type_name, '') as teach_type_name,
    
    -- 错题收集时间
    CASE 
        WHEN e.wrong_time IS NOT NULL AND e.wrong_time != '' 
        THEN TO_TIMESTAMP_LTZ(CAST(e.wrong_time AS BIGINT), 3)
        WHEN e.create_time IS NOT NULL AND e.create_time != ''
        THEN TO_TIMESTAMP_LTZ(CAST(e.create_time AS BIGINT), 3)
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

FROM wrongbook_add_events e
-- 关联题目模式表（使用时态表查询）
LEFT JOIN tower_pattern FOR SYSTEM_TIME AS OF e.proc_time AS tp
    ON e.pattern_id = tp.id
-- 关联教学类型关系表
LEFT JOIN tower_teaching_type_pt FOR SYSTEM_TIME AS OF e.proc_time AS ttpt
    ON tp.id = ttpt.pt_id AND ttpt.is_delete = 0
-- 关联教学类型表
LEFT JOIN tower_teaching_type FOR SYSTEM_TIME AS OF e.proc_time AS tt
    ON ttpt.teaching_type_id = tt.id AND tt.is_delete = 0;

-- ================================================================
-- 主要业务逻辑：处理错题订正事件（支持Upsert）
-- ================================================================
INSERT INTO dwd_wrong_record_wide_delta
SELECT 
    -- 生成唯一ID（与添加事件保持一致）
    CAST(HASH_CODE(COALESCE(e.wrong_record_id, e.eventId)) AS BIGINT) as id,
    
    -- 错题基础信息（从Topic中直接获取）
    COALESCE(e.wrong_record_id, e.eventId) as wrong_id,
    e.user_id,
    COALESCE(e.subject, '') as subject,
    COALESCE(e.subject_name, 
        CASE 
            WHEN e.subject = 'math' THEN '数学'
            WHEN e.subject = 'chinese' THEN '语文'
            WHEN e.subject = 'english' THEN '英语'
            ELSE COALESCE(e.subject, '未知')
        END
    ) as subject_name,
    e.question_id,
    COALESCE(e.question_id, '') as question,
    COALESCE(e.pattern_id, '') as pattern_id,
    COALESCE(tp.name, '') as pattern_name,
    
    -- 教学类型信息
    CAST(COALESCE(ttpt.teaching_type_id, 0) AS STRING) as teach_type_id,
    COALESCE(tt.teaching_type_name, '') as teach_type_name,
    
    -- 错题收集时间（如果有历史数据使用历史时间，否则用当前事件时间）
    CASE 
        WHEN e.create_time IS NOT NULL AND e.create_time != ''
        THEN TO_TIMESTAMP_LTZ(CAST(e.create_time AS BIGINT), 3)
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

FROM wrongbook_fix_events e
-- 关联题目模式表
LEFT JOIN tower_pattern FOR SYSTEM_TIME AS OF e.proc_time AS tp
    ON e.pattern_id = tp.id
-- 关联教学类型关系表
LEFT JOIN tower_teaching_type_pt FOR SYSTEM_TIME AS OF e.proc_time AS ttpt
    ON tp.id = ttpt.pt_id AND ttpt.is_delete = 0
-- 关联教学类型表
LEFT JOIN tower_teaching_type FOR SYSTEM_TIME AS OF e.proc_time AS tt
    ON ttpt.teaching_type_id = tt.id AND tt.is_delete = 0;
