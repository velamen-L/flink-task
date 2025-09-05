-- ================================================================
-- Flink SQL作业: 错题订正记录宽表 (BusinessEvent版本)
-- 业务描述: 实时处理错题订正记录，通过BusinessEvent标准事件流监听订正事件，
--          关联错题记录、知识点、教学类型等维表，生成宽表数据输出到ODPS
-- 输入源: biz_statistic_wrongbook (业务域事件流，BusinessEvent结构)
-- 事件类型: domain='wrongbook', type='wrongbook_fix'
-- 生成时间: 2024-12-27
-- 版本: v2.0 (基于BusinessEvent标准)
-- ================================================================

INSERT INTO `vvp`.`default`.`dwd_wrong_record_wide_delta`
SELECT
  -- 基础字段映射
  CAST(JSON_VALUE(be.payload, '$.fixId') AS BIGINT) AS id,
  JSON_VALUE(be.payload, '$.wrongId') AS wrong_id,
  JSON_VALUE(be.payload, '$.userId') AS user_id,
  JSON_VALUE(be.payload, '$.subject') AS subject,
  
  -- 科目名称中文转换
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
  
  -- 题目和知识点信息
  JSON_VALUE(be.payload, '$.questionId') AS question_id,
  CAST(NULL AS STRING) AS question,  -- 题目内容暂时设为空
  JSON_VALUE(be.payload, '$.patternId') AS pattern_id,
  pt.name AS pattern_name,
  
  -- 教学类型信息
  CAST(tt.id AS STRING) AS teach_type_id,
  tt.teaching_type_name AS teach_type_name,
  
  -- 时间字段转换
  TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.createTime') AS BIGINT), 3) AS collect_time,
  
  -- 订正相关信息
  JSON_VALUE(be.payload, '$.fixId') AS fix_id,
  TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(be.payload, '$.submitTime') AS BIGINT), 3) AS fix_time,
  CAST(JSON_VALUE(be.payload, '$.fixResult') AS BIGINT) AS fix_result,
  
  -- 订正结果描述转换
  CASE CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT)
    WHEN 1 THEN '订正'
    WHEN 0 THEN '未订正'
    ELSE ''
  END AS fix_result_desc

-- 主事件流表 (biz_statistic_{domain}标准结构，数据结构为BusinessEvent格式)
FROM `vvp`.`default`.`biz_statistic_wrongbook` be

-- 关联维表1: 知识点模式表
LEFT JOIN `vvp`.`default`.`tower_pattern` FOR SYSTEM_TIME AS OF PROCTIME() pt 
  ON pt.id = JSON_VALUE(be.payload, '$.patternId')

-- 关联维表2: 教学类型-知识点映射表
LEFT JOIN `vvp`.`default`.`tower_teaching_type_pt` FOR SYSTEM_TIME AS OF PROCTIME() ttp 
  ON ttp.pt_id = JSON_VALUE(be.payload, '$.patternId')
  AND ttp.is_delete = 0

-- 关联维表3: 教学类型表 (包含特殊的章节匹配逻辑)
LEFT JOIN `vvp`.`default`.`tower_teaching_type` FOR SYSTEM_TIME AS OF PROCTIME() tt 
  ON ttp.teaching_type_id = tt.id
  AND tt.is_delete = 0

-- 数据过滤条件
WHERE 
  -- BusinessEvent标准事件过滤 (从biz_statistic_wrongbook表)
  be.domain = 'wrongbook' 
  AND be.type = 'wrongbook_fix'
  
  -- 数据质量保证
  AND JSON_VALUE(be.payload, '$.fixId') IS NOT NULL
  AND JSON_VALUE(be.payload, '$.wrongId') IS NOT NULL
  AND JSON_VALUE(be.payload, '$.userId') IS NOT NULL
  
  -- 科目特殊处理规则：语文英语需要额外章节匹配
  AND (
    JSON_VALUE(be.payload, '$.subject') NOT IN ('CHINESE', 'ENGLISH')
    OR (
      JSON_VALUE(be.payload, '$.subject') IN ('CHINESE', 'ENGLISH') 
      AND tt.chapter_id = JSON_VALUE(be.payload, '$.chapterId')
    )
  )
  
  -- 订正结果值范围检查
  AND CAST(JSON_VALUE(be.payload, '$.fixResult') AS INT) IN (0, 1);

-- ================================================================
-- 性能优化说明:
-- 1. 使用 FOR SYSTEM_TIME AS OF PROCTIME() 确保维表查询实时性
-- 2. 维表缓存配置: 30分钟TTL, 100000条最大缓存
-- 3. 过滤条件前置，减少不必要的JOIN计算
-- 4. JSON_VALUE函数提取payload字段，保证类型安全
-- 5. 建议并行度: 根据数据量调整，推荐8-16个并行度
-- ================================================================

-- ================================================================
-- 监控要点:
-- 1. 关键指标: 处理延迟(P95<5s), 吞吐量(50-500 RPS)
-- 2. 数据质量: fixId/wrongId/userId非空率 > 99%
-- 3. JOIN命中率: 知识点表关联成功率 > 95%
-- 4. 异常监控: JSON解析失败、类型转换错误
-- 5. 资源监控: CPU < 70%, 内存 < 80%, Checkpoint < 30s
-- ================================================================

-- ================================================================
-- 运维注意事项:
-- 1. 维表数据更新可能有延迟，缓存TTL需要根据业务需求调整
-- 2. BusinessEvent payload为JSON格式，解析有一定性能开销
-- 3. 复杂的科目筛选逻辑可能影响查询性能，需要持续监控
-- 4. 时间字段统一使用TIMESTAMP_LTZ，确保时区一致性
-- 5. 支持upsert操作，主键冲突时会覆盖旧数据
-- ================================================================
