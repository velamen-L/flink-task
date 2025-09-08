-- ============================================================================
-- 新错题本增强版测试数据生成 v3.0
-- 生成时间: 2024-12-27
-- 用途: 验证和测试SQL逻辑正确性
-- ============================================================================

-- 测试用BusinessEvent数据
INSERT INTO BusinessEvent VALUES
-- 基础成功案例
('new-wrongbook', 'enhanced_wrongbook_fix', 
 '{"id":"fix001","wrong_id":"wrong001","user_id":"user001","subject":"MATH","question_id":"q001","pattern_id":"p001","create_time":1703664000000,"submit_time":1703664300000,"result":1,"confidence":0.9,"attempt_count":1,"learning_path":"adaptive","recommendation":"继续练习相似题型","difficulty":0.6,"study_duration":180,"chapter_id":"ch001","isDelete":0}', 
 1703664000000),

-- 高置信度快速掌握案例
('new-wrongbook', 'enhanced_wrongbook_fix',
 '{"id":"fix002","wrong_id":"wrong002","user_id":"user002","subject":"ENGLISH","question_id":"q002","pattern_id":"p002","create_time":1703664060000,"submit_time":1703664360000,"result":1,"confidence":0.85,"attempt_count":2,"learning_path":"standard","recommendation":"可以尝试更难的题目","difficulty":0.7,"study_duration":240,"chapter_id":"ch002","isDelete":0}',
 1703664060000),

-- 低置信度需要复习案例
('new-wrongbook', 'enhanced_wrongbook_fix',
 '{"id":"fix003","wrong_id":"wrong003","user_id":"user003","subject":"PHYSICS","question_id":"q003","pattern_id":"p003","create_time":1703664120000,"submit_time":1703664420000,"result":1,"confidence":0.5,"attempt_count":4,"learning_path":"remedial","recommendation":"建议复习基础概念","difficulty":0.8,"study_duration":450,"chapter_id":"ch003","isDelete":0}',
 1703664120000),

-- 未订正案例
('new-wrongbook', 'enhanced_wrongbook_fix',
 '{"id":"fix004","wrong_id":"wrong004","user_id":"user004","subject":"CHEMISTRY","question_id":"q004","pattern_id":"p004","create_time":1703664180000,"submit_time":1703664480000,"result":0,"confidence":0.3,"attempt_count":3,"learning_path":"guided","recommendation":"需要老师指导","difficulty":0.9,"study_duration":600,"chapter_id":"ch004","isDelete":0}',
 1703664180000),

-- 部分订正案例
('new-wrongbook', 'enhanced_wrongbook_fix',
 '{"id":"fix005","wrong_id":"wrong005","user_id":"user005","subject":"BIOLOGY","question_id":"q005","pattern_id":"p005","create_time":1703664240000,"submit_time":1703664540000,"result":2,"confidence":0.7,"attempt_count":2,"learning_path":"interactive","recommendation":"加强练习","difficulty":0.5,"study_duration":300,"chapter_id":"ch005","isDelete":0}',
 1703664240000),

-- 需要复习案例
('new-wrongbook', 'enhanced_wrongbook_fix',
 '{"id":"fix006","wrong_id":"wrong006","user_id":"user006","subject":"HISTORY","question_id":"q006","pattern_id":"p006","create_time":1703664300000,"submit_time":1703664600000,"result":3,"confidence":0.6,"attempt_count":3,"learning_path":"spaced_repetition","recommendation":"间隔复习","difficulty":0.4,"study_duration":360,"chapter_id":"ch006","isDelete":0}',
 1703664300000),

-- 语文学科章节匹配测试
('new-wrongbook', 'enhanced_wrongbook_fix',
 '{"id":"fix007","wrong_id":"wrong007","user_id":"user007","subject":"CHINESE","question_id":"q007","pattern_id":"p007","create_time":1703664360000,"submit_time":1703664660000,"result":1,"confidence":0.8,"attempt_count":1,"learning_path":"standard","recommendation":"表现优秀","difficulty":0.3,"study_duration":120,"chapter_id":"ch007","isDelete":0}',
 1703664360000),

-- 边界值测试 - 最小置信度
('new-wrongbook', 'enhanced_wrongbook_fix',
 '{"id":"fix008","wrong_id":"wrong008","user_id":"user008","subject":"GEOGRAPHY","question_id":"q008","pattern_id":"p008","create_time":1703664420000,"submit_time":1703664720000,"result":1,"confidence":0.0,"attempt_count":5,"learning_path":"intensive","recommendation":"需要加强训练","difficulty":1.0,"study_duration":900,"chapter_id":"ch008","isDelete":0}',
 1703664420000),

-- 边界值测试 - 最大置信度
('new-wrongbook', 'enhanced_wrongbook_fix',
 '{"id":"fix009","wrong_id":"wrong009","user_id":"user009","subject":"POLITICS","question_id":"q009","pattern_id":"p009","create_time":1703664480000,"submit_time":1703664780000,"result":1,"confidence":1.0,"attempt_count":1,"learning_path":"accelerated","recommendation":"学习能力强","difficulty":0.9,"study_duration":90,"chapter_id":"ch009","isDelete":0}',
 1703664480000),

-- NULL值测试
('new-wrongbook', 'enhanced_wrongbook_fix',
 '{"id":"fix010","wrong_id":"wrong010","user_id":"user010","subject":"SCIENCE","question_id":"q010","pattern_id":"p010","create_time":1703664540000,"submit_time":1703664840000,"result":1,"chapter_id":"ch010","isDelete":0}',
 1703664540000);

-- 维表测试数据：tower_pattern
INSERT INTO tower_pattern VALUES
('p001', '选择题基础', 1, 'MATH', 0.6, 1703664000000, '基础选择', '代数运算,逻辑推理'),
('p002', '阅读理解', 2, 'ENGLISH', 0.7, 1703664000000, '语言理解', '词汇理解,文本分析'),
('p003', '力学计算', 3, 'PHYSICS', 0.8, 1703664000000, '物理计算', '力学分析,公式运用'),
('p004', '化学方程式', 4, 'CHEMISTRY', 0.9, 1703664000000, '化学反应', '方程式配平,化学计算'),
('p005', '生物分类', 5, 'BIOLOGY', 0.5, 1703664000000, '生物基础', '分类学,基础概念'),
('p006', '历史时间线', 6, 'HISTORY', 0.4, 1703664000000, '历史记忆', '时间概念,历史事件'),
('p007', '诗词鉴赏', 7, 'CHINESE', 0.3, 1703664000000, '文学鉴赏', '诗词理解,文学素养'),
('p008', '地理定位', 8, 'GEOGRAPHY', 1.0, 1703664000000, '空间认知', '地理位置,空间分析'),
('p009', '政治理论', 9, 'POLITICS', 0.9, 1703664000000, '理论理解', '政治概念,理论应用'),
('p010', '科学实验', 10, 'SCIENCE', 0.7, 1703664000000, '实验方法', '科学方法,实验设计');

-- 维表测试数据：tower_teaching_type_pt
INSERT INTO tower_teaching_type_pt VALUES
(1, 1001, 'p001', 1, 0, '2024-12-27 12:00:00', 1.0),
(2, 1002, 'p002', 1, 0, '2024-12-27 12:00:00', 0.9),
(3, 1003, 'p003', 1, 0, '2024-12-27 12:00:00', 1.2),
(4, 1004, 'p004', 1, 0, '2024-12-27 12:00:00', 1.1),
(5, 1005, 'p005', 1, 0, '2024-12-27 12:00:00', 0.8),
(6, 1006, 'p006', 1, 0, '2024-12-27 12:00:00', 0.7),
(7, 1007, 'p007', 1, 0, '2024-12-27 12:00:00', 0.6),
(8, 1008, 'p008', 1, 0, '2024-12-27 12:00:00', 1.3),
(9, 1009, 'p009', 1, 0, '2024-12-27 12:00:00', 1.0),
(10, 1010, 'p010', 1, 0, '2024-12-27 12:00:00', 0.9);

-- 维表测试数据：tower_teaching_type
INSERT INTO tower_teaching_type VALUES
(1001, 'ch001', '数学基础运算', 0, '2024-12-27 12:00:00', 1, '数学基础概念'),
(1002, 'ch002', '英语阅读理解', 0, '2024-12-27 12:00:00', 2, '基础词汇'),
(1003, 'ch003', '物理力学分析', 0, '2024-12-27 12:00:00', 3, '数学基础,物理概念'),
(1004, 'ch004', '化学反应原理', 0, '2024-12-27 12:00:00', 3, '化学基础,数学计算'),
(1005, 'ch005', '生物分类学', 0, '2024-12-27 12:00:00', 1, '生物基础概念'),
(1006, 'ch006', '历史事件记忆', 0, '2024-12-27 12:00:00', 1, '基础历史知识'),
(1007, 'ch007', '古诗词鉴赏', 0, '2024-12-27 12:00:00', 2, '语文基础,文学素养'),
(1008, 'ch008', '地理空间认知', 0, '2024-12-27 12:00:00', 2, '地理基础概念'),
(1009, 'ch009', '政治理论理解', 0, '2024-12-27 12:00:00', 3, '政治基础,理论思维'),
(1010, 'ch010', '科学实验方法', 0, '2024-12-27 12:00:00', 2, '科学思维,实验基础');
