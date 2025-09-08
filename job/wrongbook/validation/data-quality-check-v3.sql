-- ================================================================
-- 数据质量检查 SQL v3.0
-- 基于 AI Agent 智能生成的数据验证规则
-- ================================================================

-- 1. 数据完整性检查
-- 检查关键字段的空值情况
SELECT 
  '数据完整性检查' as check_type,
  COUNT(*) as total_records,
  COUNT(CASE WHEN id IS NULL THEN 1 END) as null_id_count,
  COUNT(CASE WHEN wrong_id IS NULL THEN 1 END) as null_wrong_id_count,
  COUNT(CASE WHEN user_id IS NULL THEN 1 END) as null_user_id_count,
  COUNT(CASE WHEN pattern_id IS NULL THEN 1 END) as null_pattern_id_count,
  ROUND(
    (COUNT(*) - COUNT(CASE WHEN id IS NULL OR wrong_id IS NULL OR user_id IS NULL THEN 1 END)) * 100.0 / COUNT(*), 
    2
  ) as completeness_rate
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(collect_time) = CURRENT_DATE;

-- 2. 业务规则验证
-- 检查学科枚举值的有效性
SELECT 
  '学科枚举检查' as check_type,
  subject,
  subject_name,
  COUNT(*) as record_count,
  CASE 
    WHEN subject_name = '' THEN 'INVALID'
    ELSE 'VALID'
  END as validation_status
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(collect_time) = CURRENT_DATE
GROUP BY subject, subject_name
ORDER BY record_count DESC;

-- 3. 修正结果状态检查
SELECT 
  '修正结果检查' as check_type,
  fix_result,
  fix_result_desc,
  COUNT(*) as record_count,
  ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) as percentage
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(collect_time) = CURRENT_DATE
GROUP BY fix_result, fix_result_desc
ORDER BY record_count DESC;

-- 4. 时间字段有效性检查
SELECT 
  '时间字段检查' as check_type,
  COUNT(*) as total_records,
  COUNT(CASE WHEN collect_time IS NULL THEN 1 END) as null_collect_time,
  COUNT(CASE WHEN fix_time IS NULL THEN 1 END) as null_fix_time,
  COUNT(CASE WHEN collect_time > fix_time THEN 1 END) as invalid_time_order,
  COUNT(CASE WHEN collect_time > CURRENT_TIMESTAMP THEN 1 END) as future_collect_time,
  COUNT(CASE WHEN fix_time > CURRENT_TIMESTAMP THEN 1 END) as future_fix_time
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(collect_time) = CURRENT_DATE;

-- 5. 维表关联质量检查
SELECT 
  '维表关联检查' as check_type,
  COUNT(*) as total_records,
  COUNT(CASE WHEN pattern_name IS NOT NULL THEN 1 END) as pattern_joined_count,
  COUNT(CASE WHEN teaching_type_name IS NOT NULL THEN 1 END) as teaching_type_joined_count,
  ROUND(COUNT(CASE WHEN pattern_name IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as pattern_join_rate,
  ROUND(COUNT(CASE WHEN teaching_type_name IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as teaching_type_join_rate
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(collect_time) = CURRENT_DATE;

-- 6. 数据分布检查
-- 按小时统计数据量分布
SELECT 
  '数据分布检查' as check_type,
  DATE_FORMAT(collect_time, 'yyyy-MM-dd HH:00:00') as hour_window,
  COUNT(*) as record_count,
  COUNT(DISTINCT user_id) as unique_users,
  COUNT(DISTINCT subject) as unique_subjects
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(collect_time) = CURRENT_DATE
GROUP BY DATE_FORMAT(collect_time, 'yyyy-MM-dd HH:00:00')
ORDER BY hour_window;

-- 7. 异常数据检测
-- 检测可能的异常模式
SELECT 
  '异常数据检测' as check_type,
  'duplicate_records' as anomaly_type,
  COUNT(*) as anomaly_count
FROM (
  SELECT id, wrong_id, user_id, COUNT(*)
  FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
  WHERE DATE(collect_time) = CURRENT_DATE
  GROUP BY id, wrong_id, user_id
  HAVING COUNT(*) > 1
) duplicates

UNION ALL

SELECT 
  '异常数据检测' as check_type,
  'extreme_time_diff' as anomaly_type,
  COUNT(*) as anomaly_count
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(collect_time) = CURRENT_DATE
  AND ABS(UNIX_TIMESTAMP(fix_time) - UNIX_TIMESTAMP(collect_time)) > 86400 -- 超过1天的时间差

UNION ALL

SELECT 
  '异常数据检测' as check_type,
  'missing_critical_joins' as anomaly_type,
  COUNT(*) as anomaly_count
FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
WHERE DATE(collect_time) = CURRENT_DATE
  AND (pattern_name IS NULL OR teaching_type_name IS NULL);

-- ================================================================
-- 数据质量阈值检查 (AI 智能建议)
-- ================================================================

-- 8. 质量阈值告警
WITH quality_metrics AS (
  SELECT 
    ROUND((COUNT(*) - COUNT(CASE WHEN id IS NULL OR wrong_id IS NULL OR user_id IS NULL THEN 1 END)) * 100.0 / COUNT(*), 2) as completeness_rate,
    ROUND(COUNT(CASE WHEN pattern_name IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as pattern_join_rate,
    ROUND(COUNT(CASE WHEN teaching_type_name IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as teaching_type_join_rate,
    COUNT(CASE WHEN subject_name = '' THEN 1 END) as invalid_subject_count
  FROM `vvp`.`default`.`dwd_wrong_record_wide_delta`
  WHERE DATE(collect_time) = CURRENT_DATE
)
SELECT 
  '质量阈值检查' as check_type,
  CASE 
    WHEN completeness_rate < 95 THEN 'ALERT: 数据完整性低于95%'
    WHEN pattern_join_rate < 90 THEN 'ALERT: 题型关联率低于90%'
    WHEN teaching_type_join_rate < 80 THEN 'ALERT: 教学类型关联率低于80%'
    WHEN invalid_subject_count > 100 THEN 'ALERT: 无效学科数据过多'
    ELSE 'PASS: 数据质量正常'
  END as quality_status,
  completeness_rate,
  pattern_join_rate,
  teaching_type_join_rate,
  invalid_subject_count
FROM quality_metrics;
