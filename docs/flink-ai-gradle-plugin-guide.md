# Flink AI Gradle 插件使用指南

## 🎯 插件概述

Flink AI Gradle 插件是一个智能化的Flink SQL开发工具，提供以下核心功能：

1. **🔄 Flink SQL自动生成** - 基于结构化输入文件生成标准的Flink SQL作业
2. **📊 ER图知识库管理** - 自动维护和更新企业级ER图知识库
3. **🔍 数据质量验证** - 多维度验证生成的Flink SQL的正确性和性能

## 🚀 快速开始

### 1. 插件配置

在项目根目录的 `build.gradle` 文件中配置插件：

```gradle
plugins {
    id 'com.flink.ai.generator'
}

flinkAiGenerator {
    // 请求文件路径（必须）
    requestFile = file('job/wrongbook/flink-sql-request-v3.md')
    
    // 输出目录（可选，默认：build/flink-ai-output）
    outputDir = layout.buildDirectory.dir('flink-ai-output')
    
    // ER知识库目录（可选，默认：er-knowledge-base）
    knowledgeBaseDir = file('er-knowledge-base')
    
    // 业务域名称（必须）
    domain = 'wrongbook'
    
    // 启用详细日志（可选，默认：false）
    verbose = true
    
    // 跳过数据验证（可选，默认：false）
    skipValidation = false
    
    // 强制更新ER知识库（可选，默认：false）
    forceERUpdate = false
}
```

### 2. 准备输入文件

创建请求文件 `job/{domain}/flink-sql-request-v3.md`，参考模板：

```markdown
# 业务域Flink SQL作业请求

## 📋 作业基本信息
job_info:
  name: "作业名称"
  domain: "业务域"
  event_type: "事件类型"
  # ... 其他配置

## 🗄️ 源表配置
- BusinessEvent标准事件流
- Payload结构定义

## 🔗 维表配置  
- 维表列表（仅包含过滤条件）
- CREATE TABLE DDL

## 🎯 结果表配置
- 结果表定义
- CREATE TABLE DDL

## 🔄 字段映射配置
- 字段映射规则

## 🗺️ ER图定义
- 关联关系定义（提供JOIN条件）
```

## 📋 可用任务

### 核心任务

```bash
# 执行完整工作流（推荐）
gradle flinkAiWorkflow

# 仅生成Flink SQL
gradle generateFlinkSql

# 仅更新ER知识库
gradle updateERKnowledgeBase

# 仅执行数据验证
gradle validateFlinkSqlData
```

### 便捷任务

```bash
# 生成特定域的Flink SQL
gradle generateWrongbookFlinkSql

# 生成所有域的Flink SQL
gradle generateAllFlinkSql

# 清理生成的输出
gradle cleanFlinkAiOutput
```

## 📂 输出结构

执行成功后，将在 `build/flink-ai-output/{domain}/` 目录下生成：

```
build/flink-ai-output/wrongbook/
├── sql/
│   └── wrongbook_wide_table.sql          # 生成的Flink SQL文件
├── deployment/
│   └── deploy-wrongbook.sh               # 部署脚本
├── config/
│   └── wrongbook-job-config.yaml         # 作业配置文件
└── validation/
    ├── validation-report.html            # 数据验证报告
    ├── validation-summary.json           # 验证摘要
    └── performance-analysis.md           # 性能分析报告
```

## 🔧 高级配置

### 自定义任务

```gradle
// 创建自定义生成任务
task generateCustomDomain(type: com.flink.ai.FlinkAiWorkflowTask) {
    group = 'flink-ai'
    description = 'Generate Flink SQL for custom domain'
    
    requestFile = file('job/custom/request.md')
    outputDir = layout.buildDirectory.dir('flink-ai-output/custom')
    knowledgeBaseDir = file('er-knowledge-base')
    domain = 'custom'
}
```

### 批量处理

```gradle
// 批量处理多个域
task batchGenerate {
    dependsOn tasks.matching { task -> 
        task.name.startsWith('generate') && task.name.endsWith('FlinkSql')
    }
}
```

### CI/CD 集成

```gradle
// 在构建时自动生成
build.dependsOn generateWrongbookFlinkSql

// 在发布前验证
publish.dependsOn validateFlinkSqlData
```

## 📊 ER图知识库

### 知识库结构

```
er-knowledge-base/
├── er-knowledge-base.md                  # 主知识库文件
├── conflicts.md                          # 冲突报告
├── diagrams/
│   ├── wrongbook-enhanced-er.mermaid     # Mermaid ER图
│   ├── wrongbook-enhanced-er.puml        # PlantUML ER图
│   └── wrongbook-analysis.md             # 分析报告
└── history/
    └── updates-2024-12-27.md             # 更新历史
```

### 冲突处理

当检测到ER图冲突时：

1. **查看冲突报告**：`er-knowledge-base/conflicts.md`
2. **手动解决冲突**：编辑相关表结构
3. **强制更新**：设置 `forceERUpdate = true`

## 🔍 数据验证功能

### 验证维度

1. **语法检查**
   - SQL语法正确性
   - Flink特定语法验证
   - 表名和字段名检查

2. **结构验证**
   - 表结构一致性
   - 字段类型兼容性
   - 主外键关系验证

3. **数据质量检查**
   - 必填字段验证
   - 数据范围检查
   - 业务规则验证

4. **性能分析**
   - JOIN性能评估
   - 索引使用建议
   - 查询优化建议

### 验证报告

生成的验证报告包含：

- **HTML报告**：可视化验证结果
- **JSON摘要**：结构化验证数据
- **Markdown分析**：详细的性能分析

## 🛠️ 故障排除

### 常见问题

1. **插件找不到**
   ```bash
   # 确保插件已正确应用
   gradle tasks --group flink-ai
   ```

2. **请求文件解析失败**
   ```bash
   # 检查YAML格式
   # 确保字段映射语法正确
   ```

3. **ER图冲突**
   ```bash
   # 查看冲突详情
   cat er-knowledge-base/conflicts.md
   
   # 强制更新（谨慎使用）
   gradle updateERKnowledgeBase -PforceERUpdate=true
   ```

4. **数据验证失败**
   ```bash
   # 查看详细验证报告
   open build/flink-ai-output/validation/validation-report.html
   ```

### 调试模式

```gradle
// 启用详细日志
gradle flinkAiWorkflow --info

// 启用调试日志
gradle flinkAiWorkflow --debug
```

## 📈 最佳实践

### 1. 文件组织

```
project/
├── job/                          # 作业定义目录
│   ├── wrongbook/               # 错题本域
│   │   └── flink-sql-request-v3.md
│   ├── homework/                # 作业域
│   │   └── flink-sql-request-v3.md
│   └── analytics/               # 分析域
│       └── flink-sql-request-v3.md
├── er-knowledge-base/           # ER知识库
└── build.gradle                # 构建配置
```

### 2. 版本控制

- ✅ 提交请求文件（`job/*/request.md`）
- ✅ 提交ER知识库（`er-knowledge-base/`）
- ❌ 不提交生成的输出（`build/flink-ai-output/`）

### 3. 团队协作

- 使用统一的请求文件模板
- 定期同步ER知识库
- 建立代码审查流程

### 4. 持续集成

```yaml
# .github/workflows/flink-ai.yml
name: Flink AI Generation
on: [push, pull_request]

jobs:
  generate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Generate Flink SQL
        run: ./gradlew flinkAiWorkflow
      - name: Upload artifacts
        uses: actions/upload-artifact@v3
        with:
          name: flink-sql-artifacts
          path: build/flink-ai-output/
```

## 🔗 相关资源

- [Flink SQL模板参考](../templates/)
- [ER图设计规范](./er-diagram-standards.md)
- [数据验证规则](./data-validation-rules.md)
- [性能优化指南](./performance-optimization.md)
