-- =============================================
-- Flink动态路由混合架构数据库初始化脚本
-- =============================================

-- 1. 创建路由配置表
CREATE TABLE IF NOT EXISTS flink_routing_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain VARCHAR(50) NOT NULL COMMENT '业务域',
    event_type VARCHAR(100) NOT NULL COMMENT '事件类型',
    processor_class VARCHAR(200) NOT NULL COMMENT '处理器类名',
    output_config JSON COMMENT '输出配置',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    priority INT DEFAULT 100 COMMENT '优先级',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_domain_type (domain, event_type),
    INDEX idx_domain (domain),
    INDEX idx_update_time (update_time),
    INDEX idx_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Flink动态路由配置表';

-- 2. 创建错题本宽表
CREATE TABLE IF NOT EXISTS wrongbook_wide_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL COMMENT '事件ID',
    domain VARCHAR(50) NOT NULL COMMENT '业务域',
    type VARCHAR(100) NOT NULL COMMENT '事件类型',
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    user_name VARCHAR(100) COMMENT '用户名称',
    grade VARCHAR(20) COMMENT '年级',
    school VARCHAR(100) COMMENT '学校',
    question_id VARCHAR(50) NOT NULL COMMENT '题目ID',
    subject_id VARCHAR(20) COMMENT '学科ID',
    subject_name VARCHAR(50) COMMENT '学科名称',
    chapter_id VARCHAR(20) COMMENT '章节ID',
    chapter_name VARCHAR(100) COMMENT '章节名称',
    knowledge_point VARCHAR(200) COMMENT '知识点',
    difficulty_level INT COMMENT '难度等级',
    question_type VARCHAR(20) COMMENT '题目类型',
    wrong_times INT COMMENT '错误次数',
    wrong_time TIMESTAMP COMMENT '错题时间',
    fix_time TIMESTAMP COMMENT '订正时间',
    fix_result VARCHAR(20) COMMENT '订正结果',
    business_data JSON COMMENT '业务数据',
    process_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
    data_source VARCHAR(20) DEFAULT 'realtime' COMMENT '数据来源',
    processor_class VARCHAR(200) COMMENT '处理器类名',
    UNIQUE KEY uk_event_id (event_id),
    INDEX idx_user_id (user_id),
    INDEX idx_question_id (question_id),
    INDEX idx_subject_id (subject_id),
    INDEX idx_wrong_time (wrong_time),
    INDEX idx_process_time (process_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本宽表';

-- 3. 创建用户维表
CREATE TABLE IF NOT EXISTS user_dim (
    user_id VARCHAR(50) PRIMARY KEY COMMENT '用户ID',
    user_name VARCHAR(100) COMMENT '用户名称',
    grade VARCHAR(20) COMMENT '年级',
    school VARCHAR(100) COMMENT '学校',
    register_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否活跃',
    INDEX idx_grade (grade),
    INDEX idx_school (school)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户维表';

-- 4. 创建题目维表
CREATE TABLE IF NOT EXISTS question_dim (
    question_id VARCHAR(50) PRIMARY KEY COMMENT '题目ID',
    subject_id VARCHAR(20) NOT NULL COMMENT '学科ID',
    subject_name VARCHAR(50) NOT NULL COMMENT '学科名称',
    chapter_id VARCHAR(20) COMMENT '章节ID',
    chapter_name VARCHAR(100) COMMENT '章节名称',
    knowledge_point VARCHAR(200) COMMENT '知识点',
    difficulty_level INT DEFAULT 1 COMMENT '难度等级(1-5)',
    question_type VARCHAR(20) DEFAULT 'choice' COMMENT '题目类型',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否有效',
    INDEX idx_subject (subject_id),
    INDEX idx_chapter (chapter_id),
    INDEX idx_difficulty (difficulty_level),
    INDEX idx_type (question_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目维表';

-- 5. 插入示例路由配置
INSERT INTO flink_routing_config (domain, event_type, processor_class, output_config, priority) VALUES
('wrongbook', 'wrongbook_add', 'com.flink.realtime.processor.impl.WrongbookAddProcessor', 
 '{"sinks": ["wrongbook_wide_table", "alert_topic"]}', 100),
('wrongbook', 'wrongbook_fix', 'com.flink.realtime.processor.impl.WrongbookFixProcessor', 
 '{"sinks": ["wrongbook_wide_table", "metrics_topic"]}', 100),
('wrongbook', 'wrongbook_delete', 'com.flink.realtime.processor.impl.WrongbookDeleteProcessor', 
 '{"sinks": ["wrongbook_wide_table", "audit_topic"]}', 100),
('user', 'user_login', 'com.flink.realtime.processor.impl.UserLoginProcessor', 
 '{"sinks": ["user_wide_table", "behavior_topic"]}', 100),
('user', 'user_register', 'com.flink.realtime.processor.impl.UserRegisterProcessor', 
 '{"sinks": ["user_wide_table", "alert_topic"]}', 100)
ON DUPLICATE KEY UPDATE 
    processor_class = VALUES(processor_class),
    output_config = VALUES(output_config),
    priority = VALUES(priority),
    update_time = CURRENT_TIMESTAMP;

-- 6. 插入示例用户维表数据
INSERT INTO user_dim (user_id, user_name, grade, school) VALUES
('user001', '张三', '高一', '北京一中'),
('user002', '李四', '高二', '上海中学'),
('user003', '王五', '高三', '深圳实验'),
('user004', '赵六', '初三', '广州附中'),
('user005', '钱七', '初二', '成都七中')
ON DUPLICATE KEY UPDATE 
    user_name = VALUES(user_name),
    grade = VALUES(grade),
    school = VALUES(school),
    update_time = CURRENT_TIMESTAMP;

-- 7. 插入示例题目维表数据
INSERT INTO question_dim (question_id, subject_id, subject_name, chapter_id, chapter_name, knowledge_point, difficulty_level, question_type) VALUES
('q001', 'math', '数学', 'algebra', '代数', '一元二次方程', 3, 'choice'),
('q002', 'math', '数学', 'geometry', '几何', '三角形性质', 2, 'choice'),
('q003', 'physics', '物理', 'mechanics', '力学', '牛顿定律', 4, 'calculation'),
('q004', 'chemistry', '化学', 'organic', '有机化学', '烷烃性质', 3, 'choice'),
('q005', 'english', '英语', 'grammar', '语法', '时态', 2, 'choice')
ON DUPLICATE KEY UPDATE 
    subject_name = VALUES(subject_name),
    chapter_name = VALUES(chapter_name),
    knowledge_point = VALUES(knowledge_point),
    difficulty_level = VALUES(difficulty_level),
    question_type = VALUES(question_type),
    update_time = CURRENT_TIMESTAMP;

-- 8. 创建用户域宽表（示例）
CREATE TABLE IF NOT EXISTS user_wide_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL COMMENT '事件ID',
    domain VARCHAR(50) NOT NULL COMMENT '业务域',
    type VARCHAR(100) NOT NULL COMMENT '事件类型',
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    business_data JSON COMMENT '业务数据',
    process_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
    data_source VARCHAR(20) DEFAULT 'realtime' COMMENT '数据来源',
    processor_class VARCHAR(200) COMMENT '处理器类名',
    UNIQUE KEY uk_event_id (event_id),
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_process_time (process_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户域宽表';

-- 9. 查看配置结果
SELECT '路由配置' as table_name, COUNT(*) as record_count FROM flink_routing_config
UNION ALL
SELECT '用户维表', COUNT(*) FROM user_dim
UNION ALL
SELECT '题目维表', COUNT(*) FROM question_dim;

-- 显示路由配置
SELECT 
    domain,
    event_type,
    processor_class,
    JSON_EXTRACT(output_config, '$.sinks') as output_sinks,
    is_enabled,
    priority
FROM flink_routing_config 
ORDER BY domain, priority DESC;

COMMIT;
