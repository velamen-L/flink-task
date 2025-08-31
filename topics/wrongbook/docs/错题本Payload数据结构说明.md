# 错题本系统Payload数据结构说明

## 📋 概述

本文档描述了错题本系统的payload数据结构，包括错题添加、订正和删除三种主要事件类型。

## 🏗️ 数据结构设计

### 1. 错题添加事件 (wrongbook_add)

**事件描述**: 学生做错题目，系统记录错题到错题本

**Payload结构**:
```json
{
  "wrong_id": "string",           // 错题ID
  "user_id": "string",            // 用户ID
  "question_id": "string",        // 题目ID
  "pattern_id": "string",         // 题目模式ID
  "subject": "string",            // 科目
  "chapter_id": "string",         // 章节ID
  "answer_record_id": "string",   // 答题记录ID
  "answer_image": "string",       // 答题图片URL
  "result": "integer",            // 答题结果 (0:错误, 1:正确)
  "correct_status": "integer",    // 正确状态
  "origin": "string",             // 来源
  "tag_group": "string",          // 标签组
  "draft_image": "string",        // 草稿图片URL
  "q_type": "integer",            // 题目类型
  "zpd_pattern_id": "string",     // ZPD模式ID
  "create_time": "long",          // 创建时间戳
  "submit_time": "long",          // 提交时间戳
  "question": "string",           // 题目内容
  "answer": "string",             // 正确答案
  "analysis": "string",           // 题目解析
  "difficulty": "decimal",        // 难度系数
  "collect_reason": "string",     // 收集原因
  "collect_source": "string"      // 收集来源
}
```

### 2. 错题订正事件 (wrongbook_fix)

**事件描述**: 学生订正错题，系统记录订正结果

**Payload结构**:
```json
{
  "fix_id": "string",             // 订正ID
  "wrong_id": "string",           // 错题ID
  "user_id": "string",            // 用户ID
  "question_id": "string",        // 题目ID
  "pattern_id": "string",         // 题目模式ID
  "fix_answer": "string",         // 订正答案
  "fix_image": "string",          // 订正图片URL
  "fix_result": "integer",        // 订正结果 (0:错误, 1:正确)
  "fix_result_desc": "string",    // 订正结果描述
  "fix_time": "long",             // 订正时间戳
  "fix_duration": "integer",      // 订正耗时(秒)
  "fix_attempts": "integer",      // 订正尝试次数
  "fix_method": "string",         // 订正方法
  "fix_source": "string",         // 订正来源
  "is_correct": "boolean",        // 是否订正正确
  "confidence_score": "decimal",  // 置信度分数
  "teacher_feedback": "string",   // 教师反馈
  "ai_feedback": "string"         // AI反馈
}
```

### 3. 错题删除事件 (wrongbook_delete)

**事件描述**: 删除错题本中的错题

**Payload结构**:
```json
{
  "wrong_id": "string",           // 错题ID
  "user_id": "string",            // 用户ID
  "delete_reason": "string",      // 删除原因
  "delete_time": "long",          // 删除时间戳
  "delete_source": "string"       // 删除来源
}
```

## 🔄 业务流程

### 错题添加流程
1. 学生做题错误
2. 系统生成 `wrongbook_add` 事件
3. 处理器解析payload，构建宽表数据
4. 关联维表数据（题目模式、教学类型等）
5. 写入结果表 `dwd_wrong_record_wide_delta`

### 错题订正流程
1. 学生订正错题
2. 系统生成 `wrongbook_fix` 事件
3. 处理器解析payload，构建宽表数据
4. 关联维表数据，获取错题基础信息
5. 更新结果表中的订正相关字段

## 📊 维表关联

### 主要维表
- **wrong_question_record**: 错题记录表
- **tower_pattern**: 题目模式表
- **tower_teaching_type_pt**: 教学类型与题目模式关联表
- **tower_teaching_type**: 教学类型表

### 关联逻辑
1. 通过 `wrong_id` 关联错题记录表
2. 通过 `pattern_id` 关联题目模式表
3. 通过 `pattern_id` 关联教学类型表

## 🎯 结果表字段映射

| 字段名 | 来源 | 说明 |
|--------|------|------|
| id | 计算 | wrong_id的hash值 |
| wrong_id | payload | 错题ID |
| user_id | payload | 用户ID |
| subject | payload | 科目 |
| subject_name | 计算 | 科目中文名称 |
| question_id | payload | 题目ID |
| question | payload | 题目内容 |
| pattern_id | payload | 题目模式ID |
| pattern_name | 维表 | 题目模式名称 |
| teach_type_id | 维表 | 教学类型ID |
| teach_type_name | 维表 | 教学类型名称 |
| collect_time | payload | 收集时间 |
| fix_id | payload | 订正ID |
| fix_time | payload | 订正时间 |
| fix_result | payload | 订正结果 |
| fix_result_desc | payload | 订正结果描述 |
| event_id | event | 事件ID |
| event_type | event | 事件类型 |
| process_time | 计算 | 处理时间 |

## 💡 使用示例

### 错题添加事件示例
```json
{
  "domain": "wrongbook",
  "type": "wrongbook_add",
  "timestamp": 1735660800000,
  "eventId": "add_001",
  "version": "1.0",
  "source": "math_app",
  "payload": {
    "wrong_id": "wrong_20250101_001",
    "user_id": "user_12345",
    "question_id": "q_math_001",
    "pattern_id": "pattern_001",
    "subject": "math",
    "question": "解方程：2x + 3 = 7",
    "answer": "x = 2",
    "create_time": 1735660800000
  }
}
```

### 错题订正事件示例
```json
{
  "domain": "wrongbook",
  "type": "wrongbook_fix",
  "timestamp": 1735664400000,
  "eventId": "fix_001",
  "version": "1.0",
  "source": "math_app",
  "payload": {
    "fix_id": "fix_20250101_001",
    "wrong_id": "wrong_20250101_001",
    "user_id": "user_12345",
    "fix_answer": "x = 2",
    "fix_result": 1,
    "fix_result_desc": "订正正确",
    "fix_time": 1735664400000
  }
}
```

## 🔧 处理器实现

### WrongbookAddProcessor
- 处理 `wrongbook_add` 事件
- 解析payload数据
- 构建宽表数据结构
- 返回处理结果

### WrongbookFixProcessor
- 处理 `wrongbook_fix` 事件
- 解析payload数据
- 构建宽表数据结构
- 返回处理结果

## 📝 注意事项

1. **时间戳格式**: 所有时间戳使用毫秒级Unix时间戳
2. **ID生成**: 错题ID和订正ID需要全局唯一
3. **维表关联**: 处理器中预留了维表关联的接口
4. **错误处理**: 需要处理维表查询失败的情况
5. **数据验证**: 建议在处理器中添加数据验证逻辑
