# 一对一学情统计Kafka测试数据

## 应用ID到学科映射维表数据

### MySQL维表数据 (app_subject_mapping)
```sql
-- 需要在MySQL中创建并插入以下数据
CREATE TABLE app_subject_mapping (
    app_id VARCHAR(100) PRIMARY KEY,
    subject VARCHAR(50)
);

INSERT INTO app_subject_mapping VALUES
('com.jzx.client.math1v1', 'math'),
('com.jzx.client.math', 'math'),
('com.jzx.client.chinese', 'chinese'),
('com.jzx.client.english', 'english'),
('com.jzx.client.physics', 'physics'),
('com.jzx.client.physics1v1', 'physics'),
('com.jzx.client.chemistry', 'chemistry'),
('com.jzx.client.chemistry1v1', 'chemistry'),
('com.jzx.client.biology', 'biology'),
('com.jzx.client.biology1v1', 'biology');
```

## 答题提交事件测试数据

### 单条测试数据
```json
{
  "domain": "answer",
  "type": "answer_submitted",
  "user_id": "user_001",
  "device_id": "device_001",
  "app_id": "com.jzx.client.math",
  "payload": {
    "id": "answer_001",
    "pattern_id": "pattern_001",
    "chapter_id": "chapter_001",
    "result": 1,
    "subject": "math",
    "stage": 1
  },
  "event_time": "2024-12-27T12:00:00.000Z"
}
```

### 批量测试数据
```json
[
  {
    "domain": "answer",
    "type": "answer_submitted",
    "user_id": "user_001",
    "device_id": "device_001",
    "app_id": "com.jzx.client.math",
    "payload": {
      "id": "answer_001",
      "pattern_id": "pattern_001",
      "chapter_id": "chapter_001",
      "result": 1,
      "subject": "math",
      "stage": 1
    },
    "event_time": "2024-12-27T12:00:00.000Z"
  },
  {
    "domain": "answer",
    "type": "answer_submitted",
    "user_id": "user_001",
    "device_id": "device_001",
    "app_id": "com.jzx.client.math",
    "payload": {
      "id": "answer_002",
      "pattern_id": "pattern_002",
      "chapter_id": "chapter_001",
      "result": 0,
      "subject": "math",
      "stage": 1
    },
    "event_time": "2024-12-27T12:05:00.000Z"
  },
  {
    "domain": "answer",
    "type": "answer_submitted",
    "user_id": "user_002",
    "device_id": "device_002",
    "app_id": "com.jzx.client.chinese",
    "payload": {
      "id": "answer_003",
      "pattern_id": "pattern_003",
      "chapter_id": "chapter_002",
      "result": 1,
      "subject": "chinese",
      "stage": 2
    },
    "event_time": "2024-12-27T12:10:00.000Z"
  }
]
```

## 学习任务完成事件测试数据

### 单条测试数据
```json
{
  "domain": "study",
  "type": "study_interactive_task_studied",
  "user_id": "user_001",
  "device_id": "device_001",
  "app_id": "com.jzx.client.math",
  "payload": {
    "subject": "math",
    "stage": 1,
    "teaching_type_id": "teaching_001",
    "teaching_type_name": "知识点讲解",
    "task_pt_id": "task_001",
    "task_pt_name": "数学基础练习"
  },
  "event_time": "2024-12-27T12:15:00.000Z"
}
```

### 批量测试数据
```json
[
  {
    "domain": "study",
    "type": "study_interactive_task_studied",
    "user_id": "user_001",
    "device_id": "device_001",
    "app_id": "com.jzx.client.math",
    "payload": {
      "subject": "math",
      "stage": 1,
      "teaching_type_id": "teaching_001",
      "teaching_type_name": "知识点讲解",
      "task_pt_id": "task_001",
      "task_pt_name": "数学基础练习"
    },
    "event_time": "2024-12-27T12:15:00.000Z"
  },
  {
    "domain": "study",
    "type": "study_interactive_task_studied",
    "user_id": "user_001",
    "device_id": "device_001",
    "app_id": "com.jzx.client.chinese",
    "payload": {
      "subject": "chinese",
      "stage": 2,
      "teaching_type_id": "teaching_002",
      "teaching_type_name": "例题讲解",
      "task_pt_id": "task_002",
      "task_pt_name": "语文阅读理解"
    },
    "event_time": "2024-12-27T12:20:00.000Z"
  },
  {
    "domain": "study",
    "type": "study_interactive_task_studied",
    "user_id": "user_002",
    "device_id": "device_002",
    "app_id": "com.jzx.client.physics",
    "payload": {
      "subject": "physics",
      "stage": 3,
      "teaching_type_id": "teaching_003",
      "teaching_type_name": "知识点讲解",
      "task_pt_id": "task_003",
      "task_pt_name": "物理力学基础"
    },
    "event_time": "2024-12-27T12:25:00.000Z"
  }
]
```

## PT掌握事件测试数据

### 单条测试数据
```json
{
  "domain": "study",
  "type": "study_pt_mastered",
  "user_id": "user_001",
  "device_id": "device_001",
  "app_id": "com.jzx.client.math",
  "payload": {
    "ptId": "pt_001",
    "chapterId": "chapter_001",
    "masterStatus": "MASTERED",
    "subject": "math",
    "stage": 1
  },
  "event_time": "2024-12-27T12:30:00.000Z"
}
```

### 批量测试数据
```json
[
  {
    "domain": "study",
    "type": "study_pt_mastered",
    "user_id": "user_001",
    "device_id": "device_001",
    "app_id": "com.jzx.client.math",
    "payload": {
      "ptId": "pt_001",
      "chapterId": "chapter_001",
      "masterStatus": "MASTERED",
      "subject": "math",
      "stage": 1
    },
    "event_time": "2024-12-27T12:30:00.000Z"
  },
  {
    "domain": "study",
    "type": "study_pt_mastered",
    "user_id": "user_001",
    "device_id": "device_001",
    "app_id": "com.jzx.client.math",
    "payload": {
      "ptId": "pt_002",
      "chapterId": "chapter_001",
      "masterStatus": "WEAK",
      "subject": "math",
      "stage": 1
    },
    "event_time": "2024-12-27T12:35:00.000Z"
  },
  {
    "domain": "study",
    "type": "study_pt_mastered",
    "user_id": "user_002",
    "device_id": "device_002",
    "app_id": "com.jzx.client.chinese",
    "payload": {
      "ptId": "pt_003",
      "chapterId": "chapter_002",
      "masterStatus": "MASTERED",
      "subject": "chinese",
      "stage": 2
    },
    "event_time": "2024-12-27T12:40:00.000Z"
  }
]
```

## 测试数据说明

### Topic配置
- **答题提交事件**: `biz_statistic_answer-test`
- **学习任务完成事件**: `biz_statistic_study-test`
- **PT掌握事件**: `biz_statistic_study-test`

### 字段说明
1. **domain**: 业务域标识
   - `answer`: 答题相关事件
   - `study`: 学习相关事件

2. **type**: 事件类型
   - `answer_submitted`: 答题提交
   - `study_interactive_task_studied`: 学习任务完成
   - `study_pt_mastered`: PT掌握

3. **payload字段**:
   - **答题事件**: id, pattern_id, chapter_id, result, subject, stage
   - **学习任务事件**: subject, stage, teaching_type_id, teaching_type_name, task_pt_id, task_pt_name
   - **PT掌握事件**: ptId, chapterId, masterStatus, subject, stage

4. **学科映射**:
   - `com.jzx.client.math` / `com.jzx.client.math1v1` → `math`
   - `com.jzx.client.chinese` → `chinese`
   - `com.jzx.client.english` → `english`
   - `com.jzx.client.physics` / `com.jzx.client.physics1v1` → `physics`
   - `com.jzx.client.chemistry` / `com.jzx.client.chemistry1v1` → `chemistry`
   - `com.jzx.client.biology` / `com.jzx.client.biology1v1` → `biology`

### 使用说明
1. **先创建维表**: 在MySQL的guarder数据库中创建app_subject_mapping表并插入映射数据
2. **发送Kafka数据**: 将JSON数据发送到对应的Kafka topic
3. **数据格式**: 确保数据格式符合Flink SQL中定义的schema
4. **测试场景**: 测试数据包含多种学科和场景，可用于验证业务逻辑
5. **时间格式**: 时间字段使用ISO-8601格式，便于Flink处理

### 优化说明
- ✅ **学科映射优化**: 通过app_subject_mapping维表统一管理app_id到subject的映射关系
- ✅ **SQL简化**: 主查询中直接使用subject字段关联，避免重复的CASE WHEN逻辑
- ✅ **性能提升**: 维表缓存机制提高查询效率
- ✅ **维护性**: 新增app_id只需在维表中添加记录，无需修改SQL代码