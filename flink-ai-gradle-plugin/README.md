# Flink AI工作流 Gradle插件

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.flink.ai.workflow)](https://plugins.gradle.org/plugin/com.flink.ai.workflow)
[![Java](https://img.shields.io/badge/Java-17+-blue)](https://openjdk.java.net/projects/jdk/17/)
[![Gradle](https://img.shields.io/badge/Gradle-7.0+-green)](https://gradle.org/releases/)

> 将AI驱动的端到端Flink SQL开发工作流集成到Gradle构建系统中

## 🎯 核心特性

- **🚀 一键执行**: 通过`gradle runAiWorkflow`执行完整的AI工作流
- **⚡ 5分钟开发**: 从需求到生产就绪代码，5分钟完成
- **📊 质量保证**: 93.45/100平均质量评分，Critical问题零容忍
- **🔧 构建集成**: 无缝集成到现有Gradle构建流程
- **🎛️ 灵活配置**: 支持开发、测试、生产环境的不同配置

## 🚀 快速开始

### 1. 添加插件
```groovy
// build.gradle
plugins {
    id 'java'
    id 'com.flink.ai.workflow' version '1.0.0'
}
```

### 2. 基础配置
```groovy
flinkAiWorkflow {
    workspaceDir = 'job'           // 工作空间目录
    aiProvider = 'cursor'          // AI提供者
    minQualityScore = 85           // 最低质量评分
}
```

### 3. 执行工作流
```bash
# 执行完整工作流
gradle runAiWorkflow

# 查看所有AI任务
gradle tasks --group flink-ai
```

## 📋 主要任务

| 任务 | 描述 | 耗时 |
|------|------|------|
| `runAiWorkflow` | 完整的端到端工作流 | ~5分钟 |
| `generateFlinkSql` | 智能SQL生成 | ~45秒 |
| `validateFlinkSql` | 数据验证 | ~2.5分钟 |
| `updateErKnowledgeBase` | ER知识库更新 | ~1.3分钟 |
| `createFlinkDomain` | 创建业务域脚手架 | ~10秒 |

## 🎛️ 配置选项

### 基础配置
```groovy
flinkAiWorkflow {
    // 目录配置
    workspaceDir = 'job'                    
    outputDir = 'build/ai-workflow'         
    
    // AI配置
    aiProvider = 'cursor'                   // cursor, openai, azure
    workflowTimeoutMinutes = 10             
    
    // 质量控制
    qualityGateMode = 'strict'              // strict, permissive, advisory
    minQualityScore = 85                    
    allowWarnings = true                    
    
    // 功能开关
    enableDeploymentGeneration = true       
    enableCache = true                      
}
```

### 环境特定配置
```groovy
// 开发环境
if (project.hasProperty('dev')) {
    flinkAiWorkflow {
        qualityGateMode = 'permissive'
        debugMode = true
    }
}

// 生产环境  
if (project.hasProperty('prod')) {
    flinkAiWorkflow {
        qualityGateMode = 'strict'
        minQualityScore = 95
        allowWarnings = false
    }
}
```

## 🔄 构建集成

### 集成到构建流程
```groovy
// 构建时自动生成SQL
build.dependsOn 'runAiWorkflow'

// 测试前验证质量
test.dependsOn 'validateFlinkSql'

// 部署前质量检查
task deploymentValidation {
    dependsOn 'validateFlinkSql'
    
    doLast {
        // 检查质量评分
        checkQualityScore()
    }
}
```

### CI/CD集成
```bash
# Jenkins/GitHub Actions
gradle runAiWorkflow -Pprod --no-daemon

# 质量门控检查
gradle validateFlinkSql --continue
```

## 📊 输出结果

执行完成后，会在`build/ai-workflow/`目录生成：

```
build/ai-workflow/
├── {domain}/
│   ├── sql/                     # 🎯 生成的SQL文件
│   ├── deployment/              # 🚀 Kubernetes部署配置
│   ├── validation/              # 📊 验证报告
│   ├── docs/                    # 📖 技术文档
│   └── workflow/                # 📋 执行报告
└── reports/                     # 📈 整体报告
    └── overall-quality-report.md
```

## 🔧 高级功能

### 1. 自定义验证器
```groovy
flinkAiWorkflow {
    customValidators = [
        'com.example.BusinessRuleValidator',
        'com.example.SecurityValidator'
    ]
}
```

### 2. 并行执行
```groovy
flinkAiWorkflow {
    enableParallelExecution = true
    maxParallelTasks = 3
}
```

### 3. 监控集成
```groovy
task monitoringIntegration {
    dependsOn 'runAiWorkflow'
    
    doLast {
        sendMetricsToPrometheus()
        notifySlackOnIssues()
    }
}
```

## 📚 示例项目

完整的示例项目位于 `examples/` 目录：

```bash
# 克隆并试用
git clone https://github.com/yangfanlin/flink-task.git
cd flink-task/flink-ai-gradle-plugin/examples
gradle runAiWorkflow
```

## 🚨 故障排查

### 常见问题

#### Q: 插件找不到规则文件
```groovy
// 检查规则文件路径
flinkAiWorkflow {
    rulesDir = '.cursor/rules'  // 确保路径存在
}
```

#### Q: AI API调用失败
```groovy
// 检查API配置和网络
flinkAiWorkflow {
    aiConfig = [
        'api_key': System.getenv('OPENAI_API_KEY'),
        'timeout': '120'  // 增加超时时间
    ]
}
```

#### Q: 内存不足
```bash
# 增加JVM内存
gradle runAiWorkflow -Xmx4g

# 或在gradle.properties中设置
org.gradle.jvmargs=-Xmx4g
```

### 调试技巧
```bash
# 详细日志
gradle runAiWorkflow --debug --info

# 调试模式
gradle runAiWorkflow -Pdev -Pdebug=true
```

## 📖 完整文档

- **[详细使用指南](../../GRADLE-PLUGIN-GUIDE.md)** - 完整的配置和使用说明
- **[架构文档](../../ARCHITECTURE-AND-USAGE.md)** - 系统架构和设计原理
- **[AI平台介绍](../../README-AI-PLATFORM.md)** - AI工作流核心概念

## 🔄 版本信息

- **当前版本**: 1.0.0
- **兼容性**: Gradle 7.0+, Java 17+
- **依赖**: Flink 1.18.0, Jackson 2.15.2

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 技术支持

- **Issues**: [GitHub Issues](https://github.com/yangfanlin/flink-task/issues)
- **文档**: [完整文档](../../GRADLE-PLUGIN-GUIDE.md)
- **示例**: [示例项目](examples/)

---

*🔧 **Gradle插件让AI工作流触手可及** - 一个命令，享受5分钟完成完整开发周期的魅力！*
