-- ================================================================
-- Flink SQL作业: 错题本修正记录实时宽表 v3.0
-- 业务描述: 学生错题修正记录的实时数据宽表处理
-- 生成方式: 基于 Cursor 规则和 AI Agent 智能生成
-- 生成时间: 2024-12-27
-- ================================================================

INSERT INTO `vvp`.`default`.`dwd_wrong_record_wide_delta`
SELECT
  -- 基础字段：从 payload 映射，AI 自动转换为 JSON_VALUE
  CAST(JSON_VALUE(be.payload, '$.id') AS BIGINT) AS id,
  JSON_VALUE(be.payload, '$.wrong_id') AS wrong_id,
  JSON_VALUE(be.payload, '$.user_id') AS user_id,
  JSON_VALUE(be.payload, '$.subject') AS subject,
  
  -- 计算字段：学科名称中文转换
  CASE JSON_VALUE(be.payload, '$.subject')
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
  
  JSON_VALUE(be.payload, '$.question_id') AS question_id,
  CAST(NULL AS STRING) AS question,
  JSON_VALUE(be.payload, '$.pattern_id') AS pattern_id,
  
  -- 从维表映射的字段
  pt.name AS pattern_name,
  CAST(tt.id AS STRING) AS teaching_type_id,
  tt.teaching_type_name,
  
  -- 时间字段：AI 自动处理时区转换
  TO_TIMESTAMP_LTZ(JSON_VALUE(be.payload, '$.create_time'), 0) AS collect_time,
  
  -- 修正相关字段
  JSON_VALUE(be.payload, '$.id') AS fix_id,
  TO_TIMESTAMP_LTZ(JSON_VALUE(be.payload, '$.submit_time'), 0) AS fix_time,
  CAST(JSON_VALUE(be.payload, '$.result') AS BIGINT) AS fix_result,
  
  -- 修正结果状态码转中文描述
  CASE JSON_VALUE(be.payload, '$.result')
    WHEN '1' THEN '订正'
    WHEN '0' THEN '未订正'
    ELSE ''
  END AS fix_result_desc

FROM BusinessEvent be

-- 维表关联：AI 自动添加 FOR SYSTEM_TIME AS OF PROCTIME()
LEFT JOIN `vvp`.`default`.`tower_pattern` FOR SYSTEM_TIME AS OF PROCTIME() pt 
  ON pt.id = JSON_VALUE(be.payload, '$.pattern_id')

LEFT JOIN `vvp`.`default`.`tower_teaching_type_pt` FOR SYSTEM_TIME AS OF PROCTIME() ttp 
  ON ttp.pt_id = pt.id 
  AND ttp.is_delete = 0

LEFT JOIN `vvp`.`default`.`tower_teaching_type` FOR SYSTEM_TIME AS OF PROCTIME() tt 
  ON tt.id = ttp.teaching_type_id 
  AND tt.is_delete = 0

WHERE 
  -- BusinessEvent 标准事件过滤
  be.domain = 'wrongbook' 
  AND be.type = 'wrongbook_fix'
  
  -- 过滤已删除的记录
  AND JSON_VALUE(be.payload, '$.isDelete') = '0'
  
  -- 特殊业务规则：语文英语科目需要额外章节匹配
  AND (
    JSON_VALUE(be.payload, '$.subject') NOT IN ('CHINESE', 'ENGLISH')
    OR (
      JSON_VALUE(be.payload, '$.subject') IN ('CHINESE', 'ENGLISH') 
      AND tt.chapter_id = JSON_VALUE(be.payload, '$.chapter_id')
    )
  );

-- ================================================================
-- 性能优化建议 (AI 自动生成)
-- ================================================================
-- 1. 维表缓存：所有维表已配置 30min TTL 和 100000 行缓存
-- 2. JOIN 优化：使用 FOR SYSTEM_TIME AS OF PROCTIME() 进行时间旅行查询
-- 3. 过滤优化：过滤条件尽可能靠近数据源
-- 4. 字段裁剪：只 SELECT 需要的字段，避免不必要的数据传输
-- 5. 并行度建议：根据数据量设置适当的并行度 (推荐 4-8)
-- ================================================================

-- ================================================================
-- 监控要点 (AI 智能建议)
-- ================================================================
-- 1. 关键指标：
--    - JOIN 成功率：监控维表关联的成功率
--    - 数据延迟：BusinessEvent 到结果表的端到端延迟
--    - 吞吐量：每秒处理的记录数
--
-- 2. 数据质量检查：
--    - payload.isDelete = 0 的过滤效果
--    - 学科枚举值的覆盖率
--    - 时间字段的有效性
--
-- 3. 业务规则验证：
--    - 语文英语科目的章节匹配逻辑
--    - 修正结果状态的正确性
-- ================================================================
