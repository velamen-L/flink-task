-- ============================================================================
-- 用户日统计作业测试数据生成
-- 业务域: user-daily-stats (跨域统计)
-- 生成时间: 2024-12-27
-- AI生成: intelligent-sql-job-generator.mdc v3.0
-- 用途: 数据验证、边界测试、性能测试
-- ============================================================================

-- ============================================================================
-- 1. 错题本事件测试数据
-- ============================================================================

-- 正常场景：用户订正成功
INSERT INTO wrongbook_events VALUES (
    'wrongbook', 
    'wrongbook_fix', 
    '{"userId":"user_001","fixResult":"1","questionId":"q_001","patternId":"p_001"}',
    TIMESTAMP '2024-12-27 09:00:00.000'
);

INSERT INTO wrongbook_events VALUES (
    'wrongbook', 
    'wrongbook_fix', 
    '{"userId":"user_001","fixResult":"1","questionId":"q_002","patternId":"p_002"}',
    TIMESTAMP '2024-12-27 10:30:00.000'
);

-- 订正失败场景
INSERT INTO wrongbook_events VALUES (
    'wrongbook', 
    'wrongbook_fix', 
    '{"userId":"user_001","fixResult":"0","questionId":"q_003","patternId":"p_003"}',
    TIMESTAMP '2024-12-27 14:15:00.000'
);

-- 不同用户的订正记录
INSERT INTO wrongbook_events VALUES (
    'wrongbook', 
    'wrongbook_fix', 
    '{"userId":"user_002","fixResult":"1","questionId":"q_004","patternId":"p_004"}',
    TIMESTAMP '2024-12-27 11:20:00.000'
);

INSERT INTO wrongbook_events VALUES (
    'wrongbook', 
    'wrongbook_fix', 
    '{"userId":"user_003","fixResult":"0","questionId":"q_005","patternId":"p_005"}',
    TIMESTAMP '2024-12-27 16:45:00.000'
);

-- 边界测试：跨日数据
INSERT INTO wrongbook_events VALUES (
    'wrongbook', 
    'wrongbook_fix', 
    '{"userId":"user_001","fixResult":"1","questionId":"q_006","patternId":"p_006"}',
    TIMESTAMP '2024-12-26 23:59:30.000'
);

-- 异常数据测试：缺失userId (应被过滤)
INSERT INTO wrongbook_events VALUES (
    'wrongbook', 
    'wrongbook_fix', 
    '{"fixResult":"1","questionId":"q_007","patternId":"p_007"}',
    TIMESTAMP '2024-12-27 12:00:00.000'
);

-- ============================================================================
-- 2. 答题事件测试数据  
-- ============================================================================

-- 正常答题场景：高分
INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit',
    '{"userId":"user_001","score":"95","questionId":"q_101","duration":"120"}',
    TIMESTAMP '2024-12-27 09:30:00.000'
);

INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit', 
    '{"userId":"user_001","score":"88","questionId":"q_102","duration":"180"}',
    TIMESTAMP '2024-12-27 15:20:00.000'
);

-- 低分场景
INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit',
    '{"userId":"user_001","score":"65","questionId":"q_103","duration":"90"}',
    TIMESTAMP '2024-12-27 20:10:00.000'
);

-- 不同用户答题
INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit',
    '{"userId":"user_002","score":"92","questionId":"q_104","duration":"150"}',
    TIMESTAMP '2024-12-27 10:45:00.000'
);

INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit',
    '{"userId":"user_002","score":"78","questionId":"q_105","duration":"200"}',
    TIMESTAMP '2024-12-27 14:30:00.000'
);

-- 仅有答题活动的用户
INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit',
    '{"userId":"user_004","score":"85","questionId":"q_106","duration":"160"}',
    TIMESTAMP '2024-12-27 11:15:00.000'
);

-- 边界测试：满分和零分
INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit',
    '{"userId":"user_003","score":"100","questionId":"q_107","duration":"90"}',
    TIMESTAMP '2024-12-27 13:45:00.000'
);

INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit',
    '{"userId":"user_003","score":"0","questionId":"q_108","duration":"30"}',
    TIMESTAMP '2024-12-27 17:20:00.000'
);

-- 异常数据：缺失score (应被过滤)
INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit',
    '{"userId":"user_001","questionId":"q_109","duration":"120"}',
    TIMESTAMP '2024-12-27 18:00:00.000'
);

-- ============================================================================
-- 3. 用户登录事件测试数据
-- ============================================================================

-- 正常登录场景：多次登录
INSERT INTO user_events VALUES (
    'user',
    'user_login',
    '{"userId":"user_001","loginType":"mobile","deviceId":"device_001"}',
    TIMESTAMP '2024-12-27 08:30:00.000'
);

INSERT INTO user_events VALUES (
    'user',
    'user_login',
    '{"userId":"user_001","loginType":"web","deviceId":"device_002"}',
    TIMESTAMP '2024-12-27 12:45:00.000'
);

INSERT INTO user_events VALUES (
    'user',
    'user_login',
    '{"userId":"user_001","loginType":"mobile","deviceId":"device_001"}',
    TIMESTAMP '2024-12-27 21:15:00.000'
);

-- 不同用户登录
INSERT INTO user_events VALUES (
    'user',
    'user_login',
    '{"userId":"user_002","loginType":"web","deviceId":"device_003"}',
    TIMESTAMP '2024-12-27 09:15:00.000'
);

INSERT INTO user_events VALUES (
    'user',
    'user_login',
    '{"userId":"user_002","loginType":"mobile","deviceId":"device_004"}',
    TIMESTAMP '2024-12-27 19:30:00.000'
);

-- 仅有登录活动的用户
INSERT INTO user_events VALUES (
    'user',
    'user_login',
    '{"userId":"user_005","loginType":"web","deviceId":"device_005"}',
    TIMESTAMP '2024-12-27 10:20:00.000'
);

INSERT INTO user_events VALUES (
    'user',
    'user_login',
    '{"userId":"user_005","loginType":"mobile","deviceId":"device_006"}',
    TIMESTAMP '2024-12-27 15:40:00.000'
);

-- 边界测试：单次登录
INSERT INTO user_events VALUES (
    'user',
    'user_login',
    '{"userId":"user_006","loginType":"web","deviceId":"device_007"}',
    TIMESTAMP '2024-12-27 14:00:00.000'
);

-- ============================================================================
-- 4. 用户画像维表测试数据
-- ============================================================================

INSERT INTO user_profile VALUES (
    'user_001', 
    '张小明', 
    '五年级', 
    '北京', 
    TIMESTAMP '2024-01-15 10:00:00.000',
    'premium'
);

INSERT INTO user_profile VALUES (
    'user_002', 
    '李小红', 
    '四年级', 
    '上海', 
    TIMESTAMP '2024-02-20 14:30:00.000',
    'standard'
);

INSERT INTO user_profile VALUES (
    'user_003', 
    '王小华', 
    '六年级', 
    '广州', 
    TIMESTAMP '2024-03-10 09:15:00.000',
    'premium'
);

INSERT INTO user_profile VALUES (
    'user_004', 
    '赵小丽', 
    '三年级', 
    '深圳', 
    TIMESTAMP '2024-04-05 16:45:00.000',
    'standard'
);

INSERT INTO user_profile VALUES (
    'user_005', 
    '陈小强', 
    '五年级', 
    '杭州', 
    TIMESTAMP '2024-05-12 11:20:00.000',
    'basic'
);

-- 边界测试：无画像用户
-- user_006 故意不插入画像数据，测试LEFT JOIN行为

-- ============================================================================
-- 5. 预期结果验证
-- ============================================================================

-- 验证查询：检查user_001的统计结果
-- 预期结果：
-- - wrongbook_fix_count: 3 (2成功 + 1失败)
-- - fix_success_count: 2
-- - fix_success_rate: 2/3 = 0.667
-- - answer_submit_count: 3
-- - avg_score: (95+88+65)/3 = 82.67
-- - high_score_count: 2 (95, 88)
-- - login_count: 3
-- - total_activity_count: 3+3+3 = 9
-- - learning_engagement_score: (0.667*0.4) + (82.67/100*0.6) = 0.267 + 0.496 = 0.763

SELECT 
    user_id,
    stat_date,
    wrongbook_fix_count,
    fix_success_count,
    fix_success_rate,
    answer_submit_count,
    avg_score,
    high_score_count,
    login_count,
    total_activity_count,
    learning_engagement_score,
    user_name,
    grade,
    city
FROM dws_user_daily_stats
WHERE user_id = 'user_001' AND stat_date = DATE '2024-12-27';

-- 验证查询：检查各用户的活动分布
SELECT 
    user_id,
    stat_date,
    CASE 
        WHEN wrongbook_fix_count > 0 THEN 'wrongbook'
        ELSE '' 
    END ||
    CASE 
        WHEN answer_submit_count > 0 THEN '+answer'
        ELSE '' 
    END ||
    CASE 
        WHEN login_count > 0 THEN '+login'
        ELSE '' 
    END AS activity_domains,
    total_activity_count
FROM dws_user_daily_stats
WHERE stat_date = DATE '2024-12-27'
ORDER BY user_id;

-- 验证查询：检查空值处理
SELECT 
    user_id,
    stat_date,
    wrongbook_fix_count,
    answer_submit_count, 
    login_count,
    total_activity_count,
    user_name
FROM dws_user_daily_stats
WHERE stat_date = DATE '2024-12-27'
  AND (wrongbook_fix_count = 0 OR answer_submit_count = 0 OR login_count = 0)
ORDER BY user_id;

-- ============================================================================
-- 6. 边界和异常测试用例
-- ============================================================================

-- 测试用例1：历史数据（应被过滤）
INSERT INTO wrongbook_events VALUES (
    'wrongbook', 
    'wrongbook_fix', 
    '{"userId":"user_historical","fixResult":"1"}',
    TIMESTAMP '2024-12-19 10:00:00.000'  -- 超过7天
);

-- 测试用例2：未来数据
INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit',
    '{"userId":"user_future","score":"90"}',
    TIMESTAMP '2024-12-29 10:00:00.000'  -- 未来时间
);

-- 测试用例3：异常分数
INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit',
    '{"userId":"user_anomaly","score":"-10"}',  -- 负分
    TIMESTAMP '2024-12-27 10:00:00.000'
);

INSERT INTO answer_events VALUES (
    'answer',
    'answer_submit',
    '{"userId":"user_anomaly","score":"150"}',  -- 超过100分
    TIMESTAMP '2024-12-27 11:00:00.000'
);

-- 测试用例4：极大数据量（性能测试）
-- 模拟单用户大量活动
WITH RECURSIVE time_series AS (
    SELECT TIMESTAMP '2024-12-27 00:00:00.000' as ts, 1 as cnt
    UNION ALL
    SELECT ts + INTERVAL '5' MINUTE, cnt + 1
    FROM time_series 
    WHERE cnt < 100
)
INSERT INTO wrongbook_events
SELECT 
    'wrongbook',
    'wrongbook_fix',
    '{"userId":"user_heavy","fixResult":"1","questionId":"q_' || cnt || '"}',
    ts
FROM time_series;

-- 验证查询：检查数据过滤效果
SELECT 
    '历史数据过滤' as test_case,
    COUNT(*) as record_count
FROM dws_user_daily_stats
WHERE user_id = 'user_historical' AND stat_date < CURRENT_DATE - INTERVAL '7' DAY

UNION ALL

SELECT 
    '异常分数处理' as test_case,
    COUNT(*) as record_count  
FROM dws_user_daily_stats
WHERE user_id = 'user_anomaly' AND stat_date = DATE '2024-12-27'

UNION ALL

SELECT 
    '大数据量处理' as test_case,
    wrongbook_fix_count as record_count
FROM dws_user_daily_stats
WHERE user_id = 'user_heavy' AND stat_date = DATE '2024-12-27';

-- ============================================================================
-- 测试数据统计
-- ============================================================================
-- 错题本事件: 8条 (7条有效 + 1条无userId)
-- 答题事件: 10条 (9条有效 + 1条无score)  
-- 用户登录事件: 8条 (全部有效)
-- 用户画像: 5条 (user_006无画像)
-- 
-- 预期生成用户统计记录: 6个用户 (user_001 ~ user_006)
-- 跨域活动用户: user_001, user_002 (有2-3个域的活动)
-- 单域活动用户: user_003, user_004, user_005, user_006
-- ============================================================================
