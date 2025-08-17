# AI编程脚手架使用指南

## 1. 概述

本AI编程脚手架可以根据您提供的源表结构、维表结构、结果表结构，自动生成完整的Flink作业代码，包括DataStream API版本和Flink SQL版本。

## 2. 快速开始

### 2.1 环境准备

```bash
# 安装Python依赖
pip install jinja2

# 进入脚手架目录
cd scripts
```

### 2.2 配置作业信息

创建作业配置文件（参考`examples/user-job-config.json`）：

```json
{
  "domain": "user",
  "job_name": "用户事件处理作业",
  "description": "处理用户相关事件",
  "source_table": { ... },
  "dim_tables": [ ... ],
  "result_table": { ... },
  "event_types": [ ... ]
}
```

### 2.3 生成代码

```bash
# 生成用户域作业代码
python job-generator.py examples/user-job-config.json

# 生成订单域作业代码
python job-generator.py examples/order-job-config.json
```

### 2.4 输出结构

```
generated/user/
├── UserDataStreamApp.java      # DataStream API应用
├── UserSqlApp.java            # Flink SQL应用
├── UserEventProcessor.java    # 事件处理器
├── UserEvent.java            # 事件模型
└── user-application.properties # 配置文件
```

## 3. 配置文件详解

### 3.1 基本信息配置

```json
{
  "domain": "user",              // 业务域名称
  "job_name": "用户事件处理作业",   // 作业显示名称
  "description": "处理用户相关事件" // 作业描述
}
```

### 3.2 源表结构配置

```json
{
  "source_table": {
    "name": "user_event_source",    // 源表名称
    "fields": [                     // 字段定义
      {
        "name": "domain",           // 字段名
        "type": "STRING",           // Flink SQL数据类型
        "comment": "业务域"          // 字段注释
      },
      {
        "name": "timestamp",
        "type": "BIGINT",
        "comment": "事件时间戳"
      }
      // ... 更多字段
    ]
  }
}
```

**支持的数据类型：**
- `STRING` - 字符串类型
- `BIGINT` - 长整型
- `INT` - 整型
- `DOUBLE` - 双精度浮点型
- `TIMESTAMP(3)` - 带毫秒的时间戳
- `BOOLEAN` - 布尔型

### 3.3 维表结构配置

```json
{
  "dim_tables": [
    {
      "name": "user_dim",           // 维表名称
      "alias": "d1",               // SQL中的别名
      "key_field": "user_id",      // 主键字段
      "join_field": "userId",      // 关联字段（在payload中的路径）
      "fields": [                  // 维表字段定义
        {
          "name": "user_id",
          "type": "STRING",
          "comment": "用户ID"
        },
        {
          "name": "user_name", 
          "type": "STRING",
          "comment": "用户名称"
        }
      ],
      "output_fields": ["user_name", "user_level"] // 输出到结果的字段
    }
  ]
}
```

### 3.4 结果表结构配置

```json
{
  "result_table": {
    "name": "user_wide_table",      // 结果表名称
    "key_field": "event_id",        // 主键字段
    "fields": [                     // 结果表字段定义
      {
        "name": "event_id",
        "type": "STRING", 
        "comment": "事件ID"
      }
      // ... 更多字段
    ],
    "insert_fields": [              // 插入字段列表
      "event_id", "domain", "type"
    ],
    "placeholders": ["?", "?", "?"], // SQL占位符
    "field_types": ["String", "String", "String"], // PreparedStatement类型
    "java_types": ["String", "String", "String"],  // Java类型
    "select_fields": [              // SQL查询字段
      "s.eventId as event_id",
      "s.domain",
      "s.type"
    ]
  }
}
```

### 3.5 事件类型配置

```json
{
  "event_types": ["user_login", "user_purchase", "user_view"], // 支持的事件类型
  
  "event_processing_fields": {    // 每种事件的处理字段
    "user_login": [
      {
        "name": "user_id",          // 结果字段名
        "source": "userId",         // 源字段名（在payload中）
        "type": "Text"              // 数据类型（Text/Long/Double/Int）
      }
    ]
  }
}
```

## 4. 生成代码说明

### 4.1 DataStream API应用

生成的DataStream应用包含以下功能：

1. **Kafka数据源**：自动配置Kafka消费者
2. **维表关联**：使用广播流实现维表关联
3. **事件处理**：调用事件处理器进行业务逻辑处理
4. **结果输出**：写入MySQL结果表

**关键特性：**
- 支持状态管理
- 支持Checkpoint机制
- 支持异常处理
- 支持维表缓存

### 4.2 Flink SQL应用

生成的Flink SQL应用包含以下功能：

1. **DDL语句**：自动生成源表、维表、结果表DDL
2. **业务SQL**：生成标准的关联查询SQL
3. **维表Lookup**：使用Lookup Join关联维表

**关键特性：**
- 声明式SQL开发
- 支持维表缓存
- 支持水印和窗口
- 支持标准SQL函数

### 4.3 事件处理器

生成的事件处理器包含：

1. **类型路由**：根据事件类型调用不同的处理方法
2. **业务逻辑**：每种事件类型的具体处理逻辑（需要补充）
3. **异常处理**：完善的异常处理机制
4. **日志记录**：关键节点的日志记录

### 4.4 配置文件

生成的配置文件包含：

1. **基础配置**：应用名称、版本、域名
2. **Kafka配置**：连接地址、Topic、消费组
3. **MySQL配置**：连接地址、用户名、密码
4. **Flink配置**：并行度、Checkpoint等
5. **业务配置**：自定义业务参数

## 5. 示例配置

### 5.1 用户域示例

```json
{
  "domain": "user",
  "job_name": "用户事件处理作业",
  "source_table": {
    "name": "user_event_source",
    "fields": [
      {"name": "domain", "type": "STRING", "comment": "业务域"},
      {"name": "type", "type": "STRING", "comment": "事件类型"},
      {"name": "eventId", "type": "STRING", "comment": "事件ID"},
      {"name": "payload", "type": "STRING", "comment": "载荷数据"}
    ]
  },
  "dim_tables": [
    {
      "name": "user_dim", 
      "key_field": "user_id",
      "join_field": "userId",
      "fields": [
        {"name": "user_id", "type": "STRING"},
        {"name": "user_name", "type": "STRING"}
      ]
    }
  ],
  "result_table": {
    "name": "user_wide_table",
    "fields": [
      {"name": "event_id", "type": "STRING"},
      {"name": "user_id", "type": "STRING"},
      {"name": "user_name", "type": "STRING"}
    ]
  },
  "event_types": ["user_login", "user_purchase"]
}
```

### 5.2 订单域示例

```json
{
  "domain": "order",
  "job_name": "订单事件处理作业",
  "source_table": {
    "name": "order_event_source",
    "fields": [
      {"name": "domain", "type": "STRING"},
      {"name": "type", "type": "STRING"},
      {"name": "eventId", "type": "STRING"},
      {"name": "payload", "type": "STRING"}
    ]
  },
  "dim_tables": [
    {
      "name": "product_dim",
      "key_field": "product_id", 
      "join_field": "productId",
      "fields": [
        {"name": "product_id", "type": "STRING"},
        {"name": "product_name", "type": "STRING"},
        {"name": "category", "type": "STRING"}
      ]
    }
  ],
  "result_table": {
    "name": "order_wide_table",
    "fields": [
      {"name": "event_id", "type": "STRING"},
      {"name": "order_id", "type": "STRING"},
      {"name": "product_name", "type": "STRING"}
    ]
  },
  "event_types": ["order_create", "order_pay", "order_cancel"]
}
```

## 6. 高级功能

### 6.1 多维表关联

```json
{
  "dim_tables": [
    {
      "name": "user_dim",
      "key_field": "user_id",
      "join_field": "userId"
    },
    {
      "name": "product_dim", 
      "key_field": "product_id",
      "join_field": "productId"
    }
  ]
}
```

### 6.2 复杂数据类型

```json
{
  "fields": [
    {"name": "create_time", "type": "TIMESTAMP(3)"},
    {"name": "user_info", "type": "ROW<name STRING, age INT>"},
    {"name": "tags", "type": "ARRAY<STRING>"},
    {"name": "metadata", "type": "MAP<STRING, STRING>"}
  ]
}
```

### 6.3 自定义业务配置

```json
{
  "business_config": {
    "cache.ttl": "300",           // 缓存TTL（秒）
    "batch.size": "100",          // 批处理大小
    "timeout": "30000",           // 超时时间（毫秒）
    "retry.times": "3"            // 重试次数
  }
}
```

## 7. 使用最佳实践

### 7.1 表结构设计

1. **字段命名**：使用下划线分隔，如`user_id`、`create_time`
2. **数据类型**：选择合适的数据类型，避免过度设计
3. **主键设计**：确保主键唯一性和稳定性
4. **注释完整**：每个字段都应有清晰的注释

### 7.2 事件类型设计

1. **命名规范**：使用`{domain}_{action}`格式，如`user_login`
2. **类型粒度**：事件类型粒度要适中，不宜过细或过粗
3. **向前兼容**：考虑事件结构的向前兼容性

### 7.3 维表设计

1. **缓存策略**：根据数据更新频率设置合适的缓存TTL
2. **字段选择**：只包含必要的维度字段，避免冗余
3. **关联字段**：确保关联字段的数据一致性

### 7.4 性能优化

1. **并行度设置**：根据数据量和集群资源合理设置并行度
2. **Checkpoint配置**：平衡数据一致性和性能
3. **内存配置**：根据状态大小调整内存配置

## 8. 故障排除

### 8.1 常见问题

**Q: 生成的代码编译失败？**
A: 检查配置文件中的数据类型映射是否正确，确保Java类型与Flink SQL类型匹配。

**Q: 维表关联查询为空？**
A: 检查关联字段名称是否正确，确保payload中存在对应字段。

**Q: 代码生成器执行失败？**
A: 检查Python环境和依赖是否正确安装，确保配置文件格式正确。

### 8.2 调试技巧

1. **日志输出**：在生成的代码中添加详细的日志输出
2. **单元测试**：为生成的处理器编写单元测试
3. **本地调试**：在本地环境中测试生成的代码
4. **增量发布**：先在测试环境验证，再发布到生产环境

## 9. 扩展开发

### 9.1 自定义模板

如需自定义代码模板，可以修改`job-generator.py`中的模板内容：

```python
template_content = '''
// 您的自定义模板内容
'''
```

### 9.2 新增数据源

如需支持新的数据源类型，可以：

1. 在配置文件中添加新的数据源配置
2. 在代码生成器中添加对应的模板
3. 实现相应的Source Function

### 9.3 新增输出目标

如需支持新的输出目标，可以：

1. 实现对应的Sink Function
2. 在代码生成器中添加对应的模板
3. 更新配置文件结构

## 10. 版本更新

### 10.1 更新日志

- **v1.0.0**: 初始版本，支持基础的DataStream和SQL代码生成
- **v1.1.0**: 增加多维表关联支持
- **v1.2.0**: 增加复杂数据类型支持

### 10.2 兼容性说明

- 配置文件格式向前兼容
- 生成的代码结构保持稳定
- API接口保持向后兼容
