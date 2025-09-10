# Wrongbook Kafka测试数据

## 单条测试数据

### 测试数据1 - 数学题修正成功
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

### 测试数据2 - 英语题修正失败
```json
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
    "submitTime": 1703123756789
  },
  "event_time": "2024-12-27T12:00:00.000Z"
}
```

### 测试数据3 - 物理题快速修正
```json
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
  "event_time": "2024-12-27T12:00:00.000Z"
}
```

### 测试数据4 - 化学题长时间修正
```json
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
    "submitTime": 1703125256789
  },
  "event_time": "2024-12-27T12:00:00.000Z"
}
```

### 测试数据5 - 生物题未修正
```json
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
      "fixId": "fix_batch_001",
      "wrongId": "wrong_batch_001",
      "userId": "user_batch_001",
      "subject": "MATH",
      "questionId": "q_batch_001",
      "patternId": "pattern_batch_001",
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
      "fixId": "fix_batch_002",
      "wrongId": "wrong_batch_002",
      "userId": "user_batch_002",
      "subject": "ENGLISH",
      "questionId": "q_batch_002",
      "patternId": "pattern_batch_002",
      "fixResult": 0,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:00:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_batch_003",
      "wrongId": "wrong_batch_003",
      "userId": "user_batch_003",
      "subject": "PHYSICS",
      "questionId": "q_batch_003",
      "patternId": "pattern_batch_003",
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
      "fixId": "fix_batch_004",
      "wrongId": "wrong_batch_004",
      "userId": "user_batch_004",
      "subject": "CHEMISTRY",
      "questionId": "q_batch_004",
      "patternId": "pattern_batch_004",
      "fixResult": 1,
      "createTime": 1703123456789,
      "submitTime": 1703125256789
    },
    "event_time": "2024-12-27T12:00:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_batch_005",
      "wrongId": "wrong_batch_005",
      "userId": "user_batch_005",
      "subject": "BIOLOGY",
      "questionId": "q_batch_005",
      "patternId": "pattern_batch_005",
      "fixResult": 0,
      "createTime": 1703123456789,
      "submitTime": 1703123756789
    },
    "event_time": "2024-12-27T12:00:00.000Z"
  },
  {
    "domain": "wrongbook",
    "type": "wrongbook_fix",
    "payload": {
      "fixId": "fix_batch_006",
      "wrongId": "wrong_batch_006",
      "userId": "user_batch_006",
      "subject": "CHINESE",
      "questionId": "q_batch_006",
      "patternId": "pattern_batch_006",
      "fixResult": 1,
      "createTime": 1703123456789,
      "submitTime": 1703123656789
    },
    "event_time": "2024-12-27T12:00:00.000Z"
  }
]
```

## 测试数据说明

### 字段说明
- **domain**: 固定为 "wrongbook"
- **type**: 固定为 "wrongbook_fix"
- **payload.fixId**: 修正记录唯一ID
- **payload.wrongId**: 错题ID
- **payload.userId**: 用户ID
- **payload.subject**: 学科 (MATH/ENGLISH/PHYSICS/CHEMISTRY/BIOLOGY/CHINESE)
- **payload.questionId**: 题目ID
- **payload.patternId**: 题型ID
- **payload.fixResult**: 修正结果 (1=成功, 0=失败)
- **payload.createTime**: 错题创建时间戳(毫秒)
- **payload.submitTime**: 修正提交时间戳(毫秒)
- **event_time**: 事件时间

### 测试场景覆盖
1. **不同学科**: 数学、英语、物理、化学、生物、语文
2. **不同修正结果**: 成功修正(fixResult=1)和失败修正(fixResult=0)
3. **不同时间间隔**: 
   - 快速修正: 5分钟内完成
   - 正常修正: 30分钟内完成
   - 长时间修正: 30分钟以上完成
4. **批量测试**: 包含多种场景的批量数据

### 使用说明
1. 单条测试数据可用于功能验证
2. 批量测试数据可用于性能测试
3. 时间戳可根据实际测试需要调整
4. 所有数据都符合Kafka JSON格式要求