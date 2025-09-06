-- ================================================================
-- Flink SQL数据验证方案: 错题订正记录宽表
-- 目标SQL: wrongbook_fix_wide_table_v2.sql
-- 验证范围: 功能正确性、数据一致性、性能稳定性
-- 创建时间: 2024-12-27
-- ================================================================

-- ================================================================
-- 1. 基础数据完整性验证
-- ================================================================

-- 验证1.1: 记录数一致性检查
-- 目的: 确保源数据和结果数据的记录数基本一致
SELECT 
  'source_data' as data_source,
  COUNT(*) as total_count,
  COUNT(DISTINCT JSON_VALUE(payload, '$.fixId')) as distinct_fix_count,
  DATE(event_time) as process_date
FROM `vvp`.`default`.`biz_statistic_wrongbook` 
WHERE domain = 'wrongbook' 
  AND type = 'wrongbook_fix'
  AND DATE(event_time) = CURRENT_DATE
GROUP BY DATE(event_time)

UNION ALL

SELECT 
  'result_data' as data_source,
  COUNT(*) as total_count,
  COUNT(DISTINCT fix_id) as distinct_fix_count,
  DATE(fix_time) as process_date
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(fix_time) = CURRENT_DATE
GROUP BY DATE(fix_time);

-- 预期结果: source_data和result_data的记录数应该基本一致（差异<5%）

-- ================================================================
-- 2. 字段映射正确性验证
-- ================================================================

-- 验证2.1: 科目名称转换正确性
-- 目的: 确保科目代码到中文名称的转换逻辑正确
SELECT 
  subject as subject_code,
  subject_name,
  COUNT(*) as record_count,
  -- 验证转换规则
  CASE 
    WHEN subject = 'ENGLISH' AND subject_name = '英语' THEN '✓'
    WHEN subject = 'BIOLOGY' AND subject_name = '生物' THEN '✓'
    WHEN subject = 'math' AND subject_name = '数学' THEN '✓'
    WHEN subject = 'MATH' AND subject_name = '数学' THEN '✓'
    WHEN subject = 'PHYSICS' AND subject_name = '物理' THEN '✓'
    WHEN subject = 'CHEMISTRY' AND subject_name = '化学' THEN '✓'
    WHEN subject = 'AOSHU' AND subject_name = '数学思维' THEN '✓'
    WHEN subject = 'SCIENCE' AND subject_name = '科学' THEN '✓'
    WHEN subject = 'CHINESE' AND subject_name = '语文' THEN '✓'
    WHEN subject_name = '' THEN '✓'  -- 其他情况应该为空
    ELSE '✗ ERROR'
  END as validation_result
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(fix_time) = CURRENT_DATE
GROUP BY subject, subject_name
ORDER BY record_count DESC;

-- 预期结果: 所有记录的validation_result都应该是'✓'

-- 验证2.2: 订正结果描述转换正确性
SELECT 
  fix_result,
  fix_result_desc,
  COUNT(*) as record_count,
  -- 验证转换规则
  CASE 
    WHEN fix_result = 1 AND fix_result_desc = '订正' THEN '✓'
    WHEN fix_result = 0 AND fix_result_desc = '未订正' THEN '✓'
    WHEN fix_result_desc = '' THEN '✓'  -- 其他情况应该为空
    ELSE '✗ ERROR'
  END as validation_result
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(fix_time) = CURRENT_DATE
GROUP BY fix_result, fix_result_desc;

-- 预期结果: 所有记录的validation_result都应该是'✓'

-- ================================================================
-- 3. JOIN关联正确性验证
-- ================================================================

-- 验证3.1: 知识点关联成功率
-- 目的: 确保知识点维表关联的成功率在合理范围内
SELECT 
  '知识点关联率' as validation_type,
  COUNT(*) as total_records,
  COUNT(pattern_name) as pattern_joined_count,
  COUNT(pattern_name) * 100.0 / COUNT(*) as join_success_rate
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(fix_time) = CURRENT_DATE;

-- 预期结果: join_success_rate 应该 > 95%

-- 验证3.2: 教学类型关联成功率  
SELECT 
  '教学类型关联率' as validation_type,
  COUNT(*) as total_records,
  COUNT(teach_type_name) as teaching_type_joined_count,
  COUNT(teach_type_name) * 100.0 / COUNT(*) as join_success_rate
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(fix_time) = CURRENT_DATE;

-- 预期结果: join_success_rate 应该 > 80% (教学类型是多级关联，成功率可能略低)

-- ================================================================
-- 4. 时间处理正确性验证
-- ================================================================

-- 验证4.1: 时间字段转换正确性
-- 目的: 确保timestamp转换逻辑正确，时区处理正确
WITH source_time_stats AS (
  SELECT 
    'source' as data_type,
    MIN(CAST(JSON_VALUE(payload, '$.submitTime') AS BIGINT)) as min_submit_time,
    MAX(CAST(JSON_VALUE(payload, '$.submitTime') AS BIGINT)) as max_submit_time,
    AVG(CAST(JSON_VALUE(payload, '$.submitTime') AS BIGINT)) as avg_submit_time,
    COUNT(*) as record_count
  FROM `vvp`.`default`.`biz_statistic_wrongbook`
  WHERE domain = 'wrongbook' 
    AND type = 'wrongbook_fix'
    AND DATE(event_time) = CURRENT_DATE
),
result_time_stats AS (
  SELECT 
    'result' as data_type,
    MIN(UNIX_TIMESTAMP(fix_time) * 1000) as min_submit_time,
    MAX(UNIX_TIMESTAMP(fix_time) * 1000) as max_submit_time,
    AVG(UNIX_TIMESTAMP(fix_time) * 1000) as avg_submit_time,
    COUNT(*) as record_count
  FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
  WHERE DATE(fix_time) = CURRENT_DATE
)
SELECT * FROM source_time_stats
UNION ALL  
SELECT * FROM result_time_stats;

-- 预期结果: 源数据和结果数据的时间范围应该基本一致

-- ================================================================
-- 5. 业务逻辑验证
-- ================================================================

-- 验证5.1: 语文英语科目章节匹配逻辑验证
-- 目的: 确保语文英语科目的特殊处理逻辑正确
SELECT 
  subject,
  COUNT(*) as total_count,
  COUNT(teach_type_name) as has_teaching_type_count,
  COUNT(teach_type_name) * 100.0 / COUNT(*) as teaching_type_rate
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(fix_time) = CURRENT_DATE
  AND subject IN ('CHINESE', 'ENGLISH')
GROUP BY subject;

-- 预期结果: 语文英语科目的教学类型关联率应该合理（取决于测试数据的章节匹配情况）

-- 验证5.2: 必填字段完整性检查
SELECT 
  'fix_id' as field_name,
  COUNT(*) as total_count,
  COUNT(fix_id) as non_null_count,
  (COUNT(*) - COUNT(fix_id)) as null_count
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(fix_time) = CURRENT_DATE

UNION ALL

SELECT 
  'user_id' as field_name,
  COUNT(*) as total_count,
  COUNT(user_id) as non_null_count,
  (COUNT(*) - COUNT(user_id)) as null_count
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(fix_time) = CURRENT_DATE

UNION ALL

SELECT 
  'wrong_id' as field_name,
  COUNT(*) as total_count,
  COUNT(wrong_id) as non_null_count,
  (COUNT(*) - COUNT(wrong_id)) as null_count
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(fix_time) = CURRENT_DATE;

-- 预期结果: 关键字段的null_count应该为0

-- ================================================================
-- 6. 端到端验证测试用例
-- ================================================================

-- 测试用例6.1: 插入特定测试数据进行端到端验证
-- 注意: 以下SQL仅用于测试环境，生产环境请谨慎使用

/*
-- 插入测试数据到源表
INSERT INTO `vvp`.`default`.`biz_statistic_wrongbook` VALUES (
  'test_event_20241227_001',
  'wrongbook',
  'wrongbook_fix',
  '{"fixId":"test_fix_20241227_001","wrongId":"test_wrong_001","userId":"test_user_001","subject":"MATH","questionId":"test_question_001","patternId":"existing_pattern_id","createTime":1703123456000,"submitTime":1703123456789,"fixResult":1,"chapterId":"test_chapter_001"}',
  CURRENT_TIMESTAMP,
  'test_validation'
);

-- 等待5-10秒后查询结果
SELECT 
  id,
  wrong_id,
  user_id,
  subject,
  subject_name,
  pattern_name,
  fix_result,
  fix_result_desc,
  fix_time
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE fix_id = 'test_fix_20241227_001';

-- 预期结果验证:
-- 1. 应该存在1条记录
-- 2. subject = 'MATH', subject_name = '数学'  
-- 3. fix_result = 1, fix_result_desc = '订正'
-- 4. fix_time 应该对应 submitTime 的转换结果
-- 5. 如果pattern_id存在于维表中，pattern_name应该有值
*/

-- ================================================================
-- 7. 性能基准验证
-- ================================================================

-- 验证7.1: 处理延迟统计
-- 目的: 评估数据处理的延迟情况
SELECT 
  DATE(fix_time) as process_date,
  COUNT(*) as record_count,
  PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY UNIX_TIMESTAMP(CURRENT_TIMESTAMP) - UNIX_TIMESTAMP(fix_time)) as p50_delay_seconds,
  PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY UNIX_TIMESTAMP(CURRENT_TIMESTAMP) - UNIX_TIMESTAMP(fix_time)) as p95_delay_seconds,
  PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY UNIX_TIMESTAMP(CURRENT_TIMESTAMP) - UNIX_TIMESTAMP(fix_time)) as p99_delay_seconds
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(fix_time) = CURRENT_DATE
GROUP BY DATE(fix_time);

-- 预期结果: p95_delay_seconds < 300 (5分钟), p99_delay_seconds < 600 (10分钟)

-- ================================================================
-- 8. 数据质量监控视图
-- ================================================================

-- 创建数据质量监控视图 (用于持续监控)
CREATE VIEW IF NOT EXISTS `vvp`.`default`.`wrongbook_fix_data_quality_monitor` AS
SELECT 
  DATE(fix_time) as monitor_date,
  COUNT(*) as total_records,
  
  -- 数据完整性指标
  COUNT(fix_id) * 100.0 / COUNT(*) as fix_id_completion_rate,
  COUNT(user_id) * 100.0 / COUNT(*) as user_id_completion_rate,
  COUNT(wrong_id) * 100.0 / COUNT(*) as wrong_id_completion_rate,
  
  -- 维表关联成功率
  COUNT(pattern_name) * 100.0 / COUNT(*) as pattern_join_success_rate,
  COUNT(teach_type_name) * 100.0 / COUNT(*) as teaching_type_join_success_rate,
  
  -- 业务指标分布
  COUNT(CASE WHEN fix_result = 1 THEN 1 END) * 100.0 / COUNT(*) as fix_success_rate,
  COUNT(CASE WHEN subject_name != '' THEN 1 END) * 100.0 / COUNT(*) as subject_mapping_success_rate,
  
  -- 数据时效性 (处理延迟)
  AVG(UNIX_TIMESTAMP(CURRENT_TIMESTAMP) - UNIX_TIMESTAMP(fix_time)) as avg_processing_delay_seconds,
  
  CURRENT_TIMESTAMP as last_updated
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE fix_time >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY)  -- 最近7天数据
GROUP BY DATE(fix_time);

-- ================================================================
-- 9. 验证结果标准和告警阈值
-- ================================================================

/*
数据质量标准:
✅ 基础完整性:
   - 记录数一致性: |source_count - result_count| / source_count < 5%
   - 关键字段完整性: fix_id, user_id, wrong_id 非空率 = 100%

✅ 业务逻辑正确性:
   - 科目名称转换: 转换错误率 = 0%
   - 订正结果转换: 转换错误率 = 0%
   - 时间处理: 时间范围差异 < 1小时

✅ 维表关联质量:
   - 知识点关联成功率: > 95%
   - 教学类型关联成功率: > 80%

✅ 性能要求:
   - 处理延迟P95: < 5分钟
   - 处理延迟P99: < 10分钟

🚨 告警触发条件:
   - 任何关键字段出现NULL值
   - 科目或订正结果转换出现错误
   - 知识点关联成功率 < 90%
   - 处理延迟P95 > 10分钟
   - 单日数据量异常波动 > 50%
*/

-- ================================================================
-- 使用说明:
-- 1. 测试环境: 运行全部验证SQL，确保所有指标符合预期
-- 2. 预发环境: 重点运行完整性和正确性验证
-- 3. 生产环境: 定期运行监控视图，设置自动告警
-- 4. 问题排查: 当指标异常时，运行对应的详细验证SQL定位问题
-- ================================================================
