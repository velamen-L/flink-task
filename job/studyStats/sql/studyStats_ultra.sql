-- 一对一学情统计Flink SQL作业
-- 基于环境配置: config/env-test.yml

-- ============================================
-- 源表定义 (Kafka)
-- ============================================

-- 答题提交事件源表
CREATE TEMPORARY TABLE answerSubmittedEvent (
    domain STRING,
    type STRING,
    user_id STRING,
    device_id STRING,
    app_id STRING,
    payload STRING,
    event_time STRING,
    processing_time AS PROCTIME()
) WITH (
    'connector' = 'kafka',
    'properties.bootstrap.servers' = 'alikafka-post-cn-gh6439cb1005-1-vpc.alikafka.aliyuncs.com:9092,alikafka-post-cn-gh6439cb1005-2-vpc.alikafka.aliyuncs.com:9092,alikafka-post-cn-gh6439cb1005-3-vpc.alikafka.aliyuncs.com:9092',
    'properties.group.id' = 'flink-business-test',
    'topic' = 'biz_statistic_answer-test',
    'format' = 'json',
    'scan.startup.mode' = 'latest-offset',
    'json.timestamp-format.standard' = 'ISO-8601'
);

-- 学习任务完成事件源表
CREATE TEMPORARY TABLE studyTaskFinishedEvent (
    domain STRING,
    type STRING,
    user_id STRING,
    device_id STRING,
    app_id STRING,
    payload STRING,
    event_time STRING,
    processing_time AS PROCTIME()
) WITH (
    'connector' = 'kafka',
    'properties.bootstrap.servers' = 'alikafka-post-cn-gh6439cb1005-1-vpc.alikafka.aliyuncs.com:9092,alikafka-post-cn-gh6439cb1005-2-vpc.alikafka.aliyuncs.com:9092,alikafka-post-cn-gh6439cb1005-3-vpc.alikafka.aliyuncs.com:9092',
    'properties.group.id' = 'flink-business-test',
    'topic' = 'biz_statistic_study-test',
    'format' = 'json',
    'scan.startup.mode' = 'latest-offset',
    'json.timestamp-format.standard' = 'ISO-8601'
);

-- PT掌握事件源表
CREATE TEMPORARY TABLE studyPtMasteredEvent (
    domain STRING,
    type STRING,
    user_id STRING,
    device_id STRING,
    app_id STRING,
    payload STRING,
    event_time STRING,
    processing_time AS PROCTIME()
) WITH (
    'connector' = 'kafka',
    'properties.bootstrap.servers' = 'alikafka-post-cn-gh6439cb1005-1-vpc.alikafka.aliyuncs.com:9092,alikafka-post-cn-gh6439cb1005-2-vpc.alikafka.aliyuncs.com:9092,alikafka-post-cn-gh6439cb1005-3-vpc.alikafka.aliyuncs.com:9092',
    'properties.group.id' = 'flink-business-test',
    'topic' = 'biz_statistic_study-test',
    'format' = 'json',
    'scan.startup.mode' = 'latest-offset',
    'json.timestamp-format.standard' = 'ISO-8601'
);

-- 应用时长统计源表 (MySQL CDC)
CREATE TEMPORARY TABLE guarder_app_time (
    id STRING,
    statistics_date STRING,
    user_id STRING,
    app_id STRING,
    time INT,
    processing_time AS PROCTIME()
) WITH (
    'connector' = 'mysql-cdc',
    'hostname' = 'rm-bp1543eg312q7x4n3.mysql.rds.aliyuncs.com',
    'port' = '3306',
    'username' = 'app_rw',
    'password' = 'vGcvUh7wbGREWucW6LR0',
    'database-name' = 'guarder',
    'table-name' = 'guarder_app_time'
);

-- ============================================
-- 维表定义 (MySQL)
-- ============================================

-- 应用ID到学科映射维表
CREATE TEMPORARY TABLE app_subject_mapping (
    app_id STRING NOT NULL,
    subject STRING,
    PRIMARY KEY (app_id) NOT ENFORCED
) WITH (
    'connector' = 'mysql',
    'database-name' = 'guarder',
    'hostname' = 'rm-bp1543eg312q7x4n3.mysql.rds.aliyuncs.com',
    'lookup.cache.max-rows' = '100000',
    'lookup.cache.strategy' = 'LRU',
    'lookup.cache.ttl' = '10 minutes',
    'lookup.max-retries' = '3',
    'password' = 'vGcvUh7wbGREWucW6LR0',
    'port' = '3306',
    'table-name' = 'app_subject_mapping',
    'username' = 'app_rw'
);

-- 题型维表
CREATE TEMPORARY TABLE tower_pattern (
    id STRING NOT NULL,
    name STRING,
    subject STRING,
    difficulty DECIMAL(5,3),
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'mysql',
    'database-name' = 'tower',
    'hostname' = 'rm-bp1543eg312q7x4n3.mysql.rds.aliyuncs.com',
    'lookup.cache.max-rows' = '100000',
    'lookup.cache.strategy' = 'LRU',
    'lookup.cache.ttl' = '30min',
    'lookup.max-retries' = '3',
    'password' = 'vGcvUh7wbGREWucW6LR0',
    'port' = '3306',
    'table-name' = 'tower_pattern',
    'username' = 'app_rw'
);

-- 教学类型PT关联维表
CREATE TEMPORARY TABLE tower_teaching_type_pt (
    id BIGINT NOT NULL,
    teaching_type_id BIGINT,
    pt_id STRING,
    is_delete TINYINT,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'mysql',
    'database-name' = 'tower',
    'hostname' = 'rm-bp1543eg312q7x4n3.mysql.rds.aliyuncs.com',
    'lookup.cache.max-rows' = '100000',
    'lookup.cache.strategy' = 'LRU',
    'lookup.cache.ttl' = '10 minutes',
    'lookup.max-retries' = '3',
    'password' = 'vGcvUh7wbGREWucW6LR0',
    'port' = '3306',
    'table-name' = 'tower_teaching_type_pt',
    'username' = 'app_rw'
);

-- 教学类型维表
CREATE TEMPORARY TABLE tower_teaching_type (
    id BIGINT NOT NULL,
    teaching_type_name STRING,
    chapter_id STRING,
    is_delete TINYINT,
    PRIMARY KEY (id) NOT ENFORCED
) WITH (
    'connector' = 'mysql',
    'database-name' = 'tower',
    'hostname' = 'rm-bp1543eg312q7x4n3.mysql.rds.aliyuncs.com',
    'lookup.cache.max-rows' = '100000',
    'lookup.cache.strategy' = 'LRU',
    'lookup.cache.ttl' = '10 minutes',
    'lookup.max-retries' = '3',
    'password' = 'vGcvUh7wbGREWucW6LR0',
    'port' = '3306',
    'table-name' = 'tower_teaching_type',
    'username' = 'app_rw'
);

-- ============================================
-- 结果表定义 (MySQL)
-- ============================================

CREATE TEMPORARY TABLE dws_study_stats_delta (
    current_day STRING NOT NULL,
    user_id STRING NOT NULL,
    subject STRING NOT NULL,
    study_time INT,
    answer_cnt INT,
    answer_right_cnt INT,
    master_pt_cnt INT,
    study_task_count INT,
    study_task_knowledge_explain_count INT,
    study_task_example_explain_count INT,
    PRIMARY KEY (current_day, user_id, subject) NOT ENFORCED
) WITH (
    'connector' = 'mysql',
    'database-name' = 'guarder',
    'hostname' = 'rm-bp1543eg312q7x4n3.mysql.rds.aliyuncs.com',
    'password' = 'vGcvUh7wbGREWucW6LR0',
    'port' = '3306',
    'table-name' = 'dws_study_stats_delta',
    'username' = 'app_rw'
);

-- ============================================
-- 临时视图定义
-- ============================================

-- 应用时长统计视图 (先转换为学科)
CREATE TEMPORARY VIEW study_time_stats AS
SELECT 
    gat.statistics_date,
    gat.user_id,
    asm.subject,
    SUM(gat.time) as study_time
FROM guarder_app_time gat
LEFT JOIN app_subject_mapping FOR SYSTEM_TIME AS OF gat.processing_time asm
    ON asm.app_id = gat.app_id
WHERE asm.subject IS NOT NULL
GROUP BY gat.statistics_date, gat.user_id, asm.subject;

-- 当天答题数量统计视图
CREATE TEMPORARY VIEW answer_cnt_stats AS
SELECT 
    JSON_VALUE(payload, '$.id') as answer_id,
    user_id,
    DATE_FORMAT(processing_time, 'yyyy-MM-dd') as statistics_date,
    JSON_VALUE(payload, '$.subject') as subject,
    COUNT(*) as answer_cnt
FROM answerSubmittedEvent
WHERE domain = 'answer'
    AND DATE_FORMAT(processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')
GROUP BY JSON_VALUE(payload, '$.id'), user_id, DATE_FORMAT(processing_time, 'yyyy-MM-dd'), JSON_VALUE(payload, '$.subject');

-- 当天答对数量统计视图
CREATE TEMPORARY VIEW answer_right_cnt_stats AS
SELECT 
    JSON_VALUE(payload, '$.id') as answer_id,
    user_id,
    DATE_FORMAT(processing_time, 'yyyy-MM-dd') as statistics_date,
    JSON_VALUE(payload, '$.subject') as subject,
    COUNT(*) as answer_right_cnt
FROM answerSubmittedEvent
WHERE domain = 'answer'
    AND JSON_VALUE(payload, '$.result') = '1'
    AND DATE_FORMAT(processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')
GROUP BY JSON_VALUE(payload, '$.id'), user_id, DATE_FORMAT(processing_time, 'yyyy-MM-dd'), JSON_VALUE(payload, '$.subject');

-- 当天掌握PT数量统计视图
CREATE TEMPORARY VIEW master_pt_cnt_stats AS
SELECT 
    JSON_VALUE(payload, '$.ptId') as pt_id,
    user_id,
    DATE_FORMAT(processing_time, 'yyyy-MM-dd') as statistics_date,
    JSON_VALUE(payload, '$.subject') as subject,
    COUNT(*) as master_pt_cnt
FROM studyPtMasteredEvent
WHERE domain = 'study'
    AND JSON_VALUE(payload, '$.masterStatus') = 'MASTERED'
    AND DATE_FORMAT(processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')
GROUP BY JSON_VALUE(payload, '$.ptId'), user_id, DATE_FORMAT(processing_time, 'yyyy-MM-dd'), JSON_VALUE(payload, '$.subject');

-- 当天互动任务数量统计视图
CREATE TEMPORARY VIEW study_task_count_stats AS
SELECT 
    JSON_VALUE(payload, '$.task_pt_id') as task_pt_id,
    user_id,
    DATE_FORMAT(processing_time, 'yyyy-MM-dd') as statistics_date,
    JSON_VALUE(payload, '$.subject') as subject,
    COUNT(*) as study_task_count
FROM studyTaskFinishedEvent
WHERE domain = 'study'
    AND JSON_VALUE(payload, '$.subject') IN ('chinese', 'english')
    AND DATE_FORMAT(processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')
GROUP BY JSON_VALUE(payload, '$.task_pt_id'), user_id, DATE_FORMAT(processing_time, 'yyyy-MM-dd'), JSON_VALUE(payload, '$.subject');

-- 当天知识点讲解数量统计视图
CREATE TEMPORARY VIEW study_task_knowledge_explain_count_stats AS
SELECT 
    JSON_VALUE(payload, '$.task_pt_id') as task_pt_id,
    user_id,
    DATE_FORMAT(processing_time, 'yyyy-MM-dd') as statistics_date,
    JSON_VALUE(payload, '$.subject') as subject,
    COUNT(*) as study_task_knowledge_explain_count
FROM studyTaskFinishedEvent
WHERE domain = 'study'
    AND JSON_VALUE(payload, '$.subject') NOT IN ('chinese', 'english')
    AND JSON_VALUE(payload, '$.teaching_type_name') = '知识点讲解'
    AND DATE_FORMAT(processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')
GROUP BY JSON_VALUE(payload, '$.task_pt_id'), user_id, DATE_FORMAT(processing_time, 'yyyy-MM-dd'), JSON_VALUE(payload, '$.subject');

-- 当天例题讲解数量统计视图
CREATE TEMPORARY VIEW study_task_example_explain_count_stats AS
SELECT 
    JSON_VALUE(payload, '$.task_pt_id') as task_pt_id,
    user_id,
    DATE_FORMAT(processing_time, 'yyyy-MM-dd') as statistics_date,
    JSON_VALUE(payload, '$.subject') as subject,
    COUNT(*) as study_task_example_explain_count
FROM studyTaskFinishedEvent
WHERE domain = 'study'
    AND JSON_VALUE(payload, '$.subject') NOT IN ('chinese', 'english')
    AND JSON_VALUE(payload, '$.teaching_type_name') = '例题讲解'
    AND DATE_FORMAT(processing_time, 'yyyy-MM-dd') = DATE_FORMAT(CURRENT_TIMESTAMP, 'yyyy-MM-dd')
GROUP BY JSON_VALUE(payload, '$.task_pt_id'), user_id, DATE_FORMAT(processing_time, 'yyyy-MM-dd'), JSON_VALUE(payload, '$.subject');

-- ============================================
-- 业务逻辑SQL
-- ============================================

INSERT INTO dws_study_stats_delta
SELECT 
    sts.statistics_date as current_day,
    sts.user_id,
    sts.subject,
    sts.study_time,
    COALESCE(SUM(acs.answer_cnt), 0) as answer_cnt,
    COALESCE(SUM(arcs.answer_right_cnt), 0) as answer_right_cnt,
    COALESCE(SUM(mpcs.master_pt_cnt), 0) as master_pt_cnt,
    COALESCE(SUM(stcs.study_task_count), 0) as study_task_count,
    COALESCE(SUM(stkecs.study_task_knowledge_explain_count), 0) as study_task_knowledge_explain_count,
    COALESCE(SUM(stecs.study_task_example_explain_count), 0) as study_task_example_explain_count
FROM study_time_stats sts
LEFT JOIN answer_cnt_stats acs 
    ON acs.user_id = sts.user_id 
    AND acs.statistics_date = sts.statistics_date
    AND acs.subject = sts.subject
LEFT JOIN answer_right_cnt_stats arcs 
    ON arcs.user_id = sts.user_id 
    AND arcs.statistics_date = sts.statistics_date
    AND arcs.subject = sts.subject
LEFT JOIN master_pt_cnt_stats mpcs 
    ON mpcs.user_id = sts.user_id 
    AND mpcs.statistics_date = sts.statistics_date
    AND mpcs.subject = sts.subject
LEFT JOIN study_task_count_stats stcs 
    ON stcs.user_id = sts.user_id 
    AND stcs.statistics_date = sts.statistics_date
    AND stcs.subject = sts.subject
LEFT JOIN study_task_knowledge_explain_count_stats stkecs 
    ON stkecs.user_id = sts.user_id 
    AND stkecs.statistics_date = sts.statistics_date
    AND stkecs.subject = sts.subject
LEFT JOIN study_task_example_explain_count_stats stecs 
    ON stecs.user_id = sts.user_id 
    AND stecs.statistics_date = sts.statistics_date
    AND stecs.subject = sts.subject
GROUP BY 
    sts.statistics_date,
    sts.user_id,
    sts.subject,
    sts.study_time;