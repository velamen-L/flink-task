# Wrongbook Kafka测试数据

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
      "userId": "user_002",
      "subject": "ENGLISH",
      "questionId": "q_002",
      "patternId": "pattern_002",
      "fixResult": 0,
      "createTime": 1703123456789,
      "submitTime": 1703124056789
    },
    "event_time": "2024-12-27T12:01:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_003",
      "wrongId": "wrong_003",
      "userId": "user_003",
      "subject": "PHYSICS",
      "questionId": "q_003",
      "patternId": "pattern_003",
      "fixResult": 1,
      "createTime": 1703123456789,
      "submitTime": 1703123556789
    },
    "event_time": "2024-12-27T12:02:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_004",
      "wrongId": "wrong_004",
      "userId": "user_004",
      "subject": "CHEMISTRY",
      "questionId": "q_004",
      "patternId": "pattern_004",
      "fixResult": 1,
      "createTime": 1703123456789,
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
      "userId": "user_005",
      "subject": "BIOLOGY",
      "questionId": "q_005",
      "patternId": "pattern_005",
      "fixResult": 0,
      "createTime": 1703123456789,
      "submitTime": 1703124256789
    },
    "event_time": "2024-12-27T12:04:00.000Z"
  }
]
```

## 测试数据说明

### 字段说明
- **domain**: 固定为 "wrongbook"
- **type**: 事件类型，固定为 "wrongbook_fix"
- **fixId**: 修正记录唯一标识
- **wrongId**: 错题唯一标识
- **userId**: 用户唯一标识
- **subject**: 学科类型 (MATH/ENGLISH/PHYSICS/CHEMISTRY/BIOLOGY/CHINESE)
- **questionId**: 题目唯一标识
- **patternId**: 题型唯一标识
- **fixResult**: 修正结果 (1-成功, 0-失败)
- **createTime**: 错题创建时间戳
- **submitTime**: 修正提交时间戳
- **event_time**: 事件时间 (ISO-8601格式)

### 测试场景
1. **快速修正**: fix_001 - 5分钟内完成修正
2. **修正失败**: fix_002 - 修正结果为0
3. **超快修正**: fix_003 - 1分钟内完成修正
4. **正常修正**: fix_004 - 10分钟内完成修正
5. **修正超时**: fix_005 - 修正失败

### 使用说明
1. 将JSON数据复制到Kafka管理平台
2. 发送到topic: `biz_statistic_wrongbook-test`
3. 确保Flink作业已启动并监听该topic
4. 观察结果表 `dwd_wrong_record_wide_delta` 的数据变化