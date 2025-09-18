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
    "subject": "CHINESE",
    "questionId": "q_001",
    "patternId": "pattern_001",
    "fixResult": 1,
    "createTime": 1703123456789,
    "submitTime": 1703123556789
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
      "subject": "CHINESE",
      "questionId": "q_001",
      "patternId": "pattern_001",
      "fixResult": 1,
      "createTime": 1703123456789,
      "submitTime": 1703123556789
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
      "subject": "MATH",
      "questionId": "q_002",
      "patternId": "pattern_002",
      "fixResult": 0,
      "createTime": 1703123656789,
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
      "subject": "ENGLISH",
      "questionId": "q_003",
      "patternId": "pattern_003",
      "fixResult": 1,
      "createTime": 1703123856789,
      "submitTime": 1703123956789
    },
    "event_time": "2024-12-27T12:02:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_004",
      "wrongId": "wrong_004",
      "userId": "user_001",
      "subject": "CHINESE",
      "questionId": "q_004",
      "patternId": "pattern_004",
      "fixResult": 1,
      "createTime": 1703124056789,
      "submitTime": 1703124156789
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
      "subject": "PHYSICS",
      "questionId": "q_005",
      "patternId": "pattern_005",
      "fixResult": 0,
      "createTime": 1703124256789,
      "submitTime": 1703124356789
    },
    "event_time": "2024-12-27T12:04:00.000Z"
  }
]
```

## 测试数据说明

### 字段说明
- **fixId**: 订正记录唯一标识
- **wrongId**: 错题唯一标识
- **userId**: 用户唯一标识
- **subject**: 学科 (CHINESE/MATH/ENGLISH/PHYSICS/CHEMISTRY/BIOLOGY)
- **questionId**: 题目唯一标识
- **patternId**: 题型唯一标识
- **fixResult**: 订正结果 (1=订正成功, 0=订正失败)
- **createTime**: 错题收集时间戳
- **submitTime**: 订正提交时间戳

### 测试场景
1. **用户user_001**: 语文订正2次(成功)，数学订正1次(失败)
2. **用户user_002**: 英语订正1次(成功)
3. **用户user_003**: 物理订正1次(失败)

### 智能指标测试
- **chinese_fix_num**: 用户user_001当天语文订正数量应为2
- **difficult_fix_rate**: 需要配合维表tower_pattern的difficulty字段测试正确率计算