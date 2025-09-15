# 错题本Kafka测试数据

## 单条测试数据

```json
{
  "domain": "wrongbook",
  "type": "wrongbook_fix",
  "payload": {
    "fixId": "fix_001",
    "wrongId": "wrong_001",
    "userId": "user_001",
    "subject": "MATH",
    "questionId": "q_001",
    "patternId": "pattern_001",
    "fixResult": 1,
    "createTime": 1703123456789,
    "submitTime": 1703123756789
  },
  "event_time": "2024-12-27T12:00:00.000Z"
}
```

## 批量测试数据

```json
[
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_001",
      "wrongId": "wrong_001",
      "userId": "user_001",
      "subject": "MATH",
      "questionId": "q_001",
      "patternId": "pattern_001",
      "fixResult": 1,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:00:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_002",
      "wrongId": "wrong_002",
      "userId": "user_001",
      "subject": "ENGLISH",
      "questionId": "q_002",
      "patternId": "pattern_002",
      "fixResult": 0,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:01:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_003",
      "wrongId": "wrong_003",
      "userId": "user_002",
      "subject": "CHINESE",
      "questionId": "q_003",
      "patternId": "pattern_003",
      "fixResult": 1,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:02:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_004",
      "wrongId": "wrong_004",
      "userId": "user_002",
      "subject": "PHYSICS",
      "questionId": "q_004",
      "patternId": "pattern_004",
      "fixResult": 1,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:03:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_005",
      "wrongId": "wrong_005",
      "userId": "user_003",
      "subject": "CHEMISTRY",
      "questionId": "q_005",
      "patternId": "pattern_005",
      "fixResult": 0,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:04:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_006",
      "wrongId": "wrong_006",
      "userId": "user_003",
      "subject": "BIOLOGY",
      "questionId": "q_006",
      "patternId": "pattern_006",
      "fixResult": 1,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:05:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_007",
      "wrongId": "wrong_007",
      "userId": "user_001",
      "subject": "MATH",
      "questionId": "q_007",
      "patternId": "pattern_007",
      "fixResult": 1,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:06:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_008",
      "wrongId": "wrong_008",
      "userId": "user_002",
      "subject": "ENGLISH",
      "questionId": "q_008",
      "patternId": "pattern_008",
      "fixResult": 0,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:07:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_009",
      "wrongId": "wrong_009",
      "userId": "user_003",
      "subject": "CHINESE",
      "questionId": "q_009",
      "patternId": "pattern_009",
      "fixResult": 1,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:08:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_010",
      "wrongId": "wrong_010",
      "userId": "user_001",
      "subject": "PHYSICS",
      "questionId": "q_010",
      "patternId": "pattern_010",
      "fixResult": 1,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:09:00.000Z"
  }
]
```

## 测试数据说明

### 字段说明
- **fixId**: 修正记录唯一标识
- **wrongId**: 错题唯一标识
- **userId**: 用户唯一标识
- **subject**: 学科（MATH, ENGLISH, CHINESE, PHYSICS, CHEMISTRY, BIOLOGY）
- **questionId**: 题目唯一标识
- **patternId**: 题型唯一标识
- **fixResult**: 修正结果（1=订正成功，0=订正失败）
- **createTime**: 错题创建时间戳（毫秒）
- **submitTime**: 修正提交时间戳（毫秒）

### 测试场景
1. **多用户测试**: 包含user_001, user_002, user_003三个用户
2. **多学科测试**: 涵盖数学、英语、语文、物理、化学、生物六个学科
3. **修正结果测试**: 包含成功和失败两种修正结果
4. **时间间隔测试**: createTime和submitTime之间有5分钟间隔，用于测试学习效率计算

### 使用说明
1. 将JSON数据发送到Kafka topic: `biz_statistic_wrongbook-test`
2. 确保维表数据已存在（tower_pattern, tower_teaching_type_pt, tower_teaching_type）
3. 观察结果表`dwd_wrong_record_wide_delta`的数据变化
4. 验证智能指标字段的计算结果