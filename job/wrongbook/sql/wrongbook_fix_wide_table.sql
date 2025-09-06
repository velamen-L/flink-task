-- ================================================================
-- Flink SQL作业: 错题订正记录宽表
-- 业务描述: 实时处理错题订正记录，通过BusinessEvent标准事件流监听订正事件，
--          关联错题记录、知识点、教学类型等维表，生成宽表数据输出到ODPS
-- 生成时间: 2024-12-27
-- 版本: 1.0.0
-- ================================================================

INSERT INTO dwd_wrong_record_wide_delta
SELECT
  -- 主键和基础字段
  CAST(JSON_VALUE(be.payload, '$.fixId') AS BIGINT) AS id,
  wqr.id AS wrong_id,
  wqr.user_id,
  wqr.subject,
  
  -- 科目名称中文转换
  CASE wqr.subject
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
  
  -- 题目相关字段
  wqr.question_id,
  CAST(NULL AS STRING) AS question,
  
  -- 知识点相关字段
  wqr.pattern_id,
  pt.name AS pattern_name,
  
  -- 教学类型相关字段
  CAST(tt.id AS STRING) AS teach_type_id,
  tt.teaching_type_name,
  
  -- 时间字段
  TO_TIMESTAMP_LTZ(wqr.create_time, 0) AS collect_time,
  
  -- 订正相关字段
  JSON_VALUE(be.payload, '$.fixId') AS fix_id,
  TO_TIMESTAMP_LTZ(JSON_VALUE(be.payload, '$.submitTime'), 0) AS fix_time,
  CAST(JSON_VALUE(be.payload, '$.fixResult') AS BIGINT) AS fix_result,
  
  -- 订正结果描述转换
  CASE CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT)
    WHEN 1 THEN '订正'
    WHEN 0 THEN '未订正'
    ELSE ''
  END AS fix_result_desc

FROM BusinessEvent be
-- 关联错题记录维表（核心维表，包含用户和题目信息）
LEFT JOIN wrong_question_record FOR SYSTEM_TIME AS OF PROCTIME() wqr 
  ON wqr.id = JSON_VALUE(be.payload, '$.wrongId') 
  AND wqr.is_delete = 0

-- 关联知识点维表（获取知识点名称）
LEFT JOIN tower_pattern FOR SYSTEM_TIME AS OF PROCTIME() pt 
  ON pt.id = wqr.pattern_id

-- 关联教学类型映射表（知识点到教学类型的映射）
LEFT JOIN tower_teaching_type_pt FOR SYSTEM_TIME AS OF PROCTIME() ttp 
  ON ttp.pt_id = wqr.pattern_id 
  AND ttp.is_delete = 0

-- 关联教学类型维表（获取教学类型详细信息）
LEFT JOIN tower_teaching_type FOR SYSTEM_TIME AS OF PROCTIME() tt 
  ON tt.id = ttp.teaching_type_id 
  AND tt.is_delete = 0

WHERE 
  -- 事件类型过滤
  be.domain = 'wrongbook' 
  AND be.type = 'wrongbook_fix'
  
  -- 数据质量过滤（新payload结构中没有is_delete字段）
  
  -- 业务规则过滤：语文英语科目需要额外章节匹配
  AND (
    wqr.subject NOT IN ('CHINESE', 'ENGLISH')
    OR (
      wqr.subject IN ('CHINESE', 'ENGLISH') 
      AND tt.chapter_id = wqr.chapter_id
    )
  );

-- ================================================================
-- 性能优化建议:
-- 1. 确保所有关联字段都有索引支持
-- 2. 维表缓存配置：TTL 30分钟，最大10万行
-- 3. 建议并行度设置为4-8，根据数据量调整
-- 4. checkpoint间隔建议设置为60秒
-- 
-- 监控要点:
-- 1. 关注JSON_VALUE函数的解析性能
-- 2. 监控维表查询的缓存命中率
-- 3. 观察复杂WHERE条件的执行效率
-- 4. 跟踪端到端数据延迟，目标5秒内
-- ================================================================
