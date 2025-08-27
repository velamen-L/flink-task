# AI编程脚手架使用指南 - 简化版

## 🎯 核心理念

基于**动态路由 + 热部署 + 阿里云Catalog**的AI编程脚手架，实现：
- ✅ **Topic中包含完整数据**，无需关联额外维表
- ✅ **动态路由**支持新事件类型热部署  
- ✅ **阿里云Catalog**简化表配置
- ✅ **AI代码生成**，模板驱动开发

## 📁 优化后的文件结构

```
flink-task/
├── sql/
│   └── wrongbook_wide_table_catalog.sql   # 基于Catalog的SQL
├── src/main/java/com/flink/realtime/
│   └── business/
│       └── WrongbookWideTableApp.java     # 错题本专用应用（已集成动态路由）
├── scripts/
│   ├── ai-job-generator.py               # AI作业生成器 ⭐
│   ├── examples/
│   │   └── wrongbook-wide-table-config.json  # 配置文件示例
│   └── templates/
│       └── dynamic-routing-template.json  # 动态路由模板 ⭐
└── docs/
    └── AI编程脚手架使用指南-简化版.md
```

### 🗑️ 已删除的冗余文件
- ❌ `WrongRecordDataStreamApp.java` (CDC模式)
- ❌ `WrongQuestionFixRecord.java` (CDC专用实体)
- ❌ `wrong_record_wide_table.sql` (CDC SQL)
- ❌ 相关配置和文档文件

## 🚀 快速开始

### 1. 使用AI生成器创建新作业

```bash
# 基于配置生成完整作业
python scripts/ai-job-generator.py \
  scripts/examples/wrongbook-wide-table-config.json \
  --type both

# 只生成SQL
python scripts/ai-job-generator.py \
  scripts/examples/wrongbook-wide-table-config.json \
  --type sql
```

### 2. 配置文件示例

```json
{
  "domain": "wrongbook",
  "job_name": "错题本实时宽表",
  "event_types": [
    {
      "type": "wrongbook_add",
      "description": "错题添加事件",
      "payload_fields": [
        {"name": "userId", "type": "String", "required": true},
        {"name": "questionId", "type": "String", "required": true},
        {"name": "subject", "type": "String", "required": false},
        {"name": "subjectName", "type": "String", "required": false}
      ]
    }
  ]
}
```

### 3. 运行生成的作业

```bash
# 执行生成的启动脚本
./generated/start-wrongbook-job.sh

# 或手动提交
$FLINK_HOME/bin/flink run \
  --class com.flink.realtime.business.WrongbookWideTableApp \
  target/flink-task-1.0-SNAPSHOT.jar wrongbook
```

## 🔄 动态路由和热部署

### 新增事件类型的步骤

1. **更新路由配置**（无需重启作业）
```json
{
  "event_type": "wrongbook_review",
  "processor_class": "com.flink.realtime.processor.impl.WrongbookReviewProcessor",
  "enabled": true,
  "hot_deployable": true
}
```

2. **添加处理器类**
```java
public class WrongbookReviewProcessor implements EventProcessor {
    @Override
    public Object process(BusinessEvent event) throws Exception {
        // 新的业务逻辑
    }
}
```

3. **配置生效**（通过配置源热部署）
- 数据库配置表更新
- 30秒内自动生效
- 支持版本控制和回滚

## 📊 Topic数据格式

### 统一事件格式（已包含完整业务数据）
```json
{
  "domain": "wrongbook",
  "type": "wrongbook_add", 
  "timestamp": 1640995200000,
  "eventId": "evt_123456",
  "payload": {
    "userId": "user_123",
    "questionId": "q_456",
    "subject": "math",
    "subjectName": "数学",
    "patternId": "pattern_789",
    "chapterId": "chapter_001",
    "chapterName": "二次函数",
    "wrongTime": 1640995200000,
    "createTime": 1640995100000
  }
}
```

**关键优化**: Topic中已包含`subject`、`subjectName`、`chapterId`、`chapterName`等字段，**无需关联`wrong_question_record`表**。

## 🏗️ 架构优势

### 1. 简化的数据流
```
Kafka Topic (完整数据)
    ↓ 事件过滤
    ↓ 动态路由 (热部署)
    ↓ 必要维表关联 (仅pattern/teaching_type)
    ↓ MySQL输出
```

### 2. 支持的场景
- ✅ **新事件类型**：通过配置热部署，无需代码重新部署
- ✅ **业务逻辑变更**：通过处理器类热替换
- ✅ **多租户**：不同域使用相同架构
- ✅ **A/B测试**：通过路由配置控制流量

### 3. 阿里云集成
- **VVP平台**：直接提交生成的SQL
- **Catalog管理**：表结构统一管理，无需重复定义
- **实时计算**：支持DataStream和SQL两种模式

## 🔧 开发工作流

### 日常开发流程
1. **定义配置**：编写业务配置JSON
2. **AI生成**：使用生成器创建作业代码
3. **测试验证**：本地测试或提交集群
4. **热部署**：通过配置更新支持新需求

### 新业务域接入
1. **复制模板**：基于`wrongbook`模板
2. **修改配置**：更新域名和事件类型
3. **生成代码**：使用AI生成器
4. **部署运行**：一键启动

## 📈 监控和运维

### 关键指标
- **事件处理速率**：实时监控各事件类型处理情况
- **热部署成功率**：配置更新的成功率
- **新事件类型采用率**：新增事件类型的使用情况

### 故障排查
- **配置验证**：自动验证路由配置正确性
- **版本回滚**：支持配置版本管理和快速回滚
- **处理器隔离**：单个处理器故障不影响其他事件类型

## 🎉 总结

这套AI编程脚手架实现了：

1. **架构简化**：去除不必要的表关联和文件
2. **动态扩展**：支持新事件类型的热部署
3. **开发提效**：AI代码生成，模板驱动
4. **运维友好**：集成阿里云生态，监控完善

**核心价值**：从手工编码到配置驱动，从静态部署到动态路由，实现真正的敏捷开发和运维。
