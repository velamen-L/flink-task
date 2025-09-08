# Flink AI工作流 Gradle插件使用指南

## 📖 概述

Flink AI工作流Gradle插件将强大的AI驱动端到端工作流集成到Gradle构建系统中，让你可以通过简单的命令行操作享受完整的AI自动化开发体验。

### 🎯 核心价值
- **🔧 构建集成**: 将AI工作流无缝集成到现有的Gradle构建流程
- **⚡ 命令行操作**: 通过简单的gradle命令执行复杂的AI工作流
- **🎛️ 灵活配置**: 丰富的配置选项，适应不同的项目需求
- **📊 构建报告**: 集成到Gradle的任务报告和依赖管理中
- **🔄 自动化CI/CD**: 轻松集成到持续集成和部署流程中

---

## 🚀 快速开始

### 1. 安装插件

#### 方式1：使用插件DSL（推荐）
```groovy
// build.gradle
plugins {
    id 'java'
    id 'com.flink.ai.workflow' version '1.0.0'
}
```

#### 方式2：传统方式
```groovy
// build.gradle
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath 'com.flink.ai:flink-ai-gradle-plugin:1.0.0'
    }
}

apply plugin: 'com.flink.ai.workflow'
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
# 执行完整的端到端工作流
gradle runAiWorkflow

# 查看所有可用任务
gradle tasks --group flink-ai
```

---

## 📋 可用任务

### 🔄 核心工作流任务

#### `runAiWorkflow`
执行完整的端到端AI工作流
```bash
gradle runAiWorkflow

# 带参数执行
gradle runAiWorkflow -Pdomain=wrongbook -Penv=dev
```

**执行流程**:
1. 📝 阶段1: SQL生成 (~45秒)
2. 🔍 阶段2: 数据验证 (~2.5分钟)
3. 🗄️ 阶段3: ER知识库更新 (~1.3分钟)

**输出结果**:
- `build/ai-workflow/{domain}/sql/` - 生成的SQL文件
- `build/ai-workflow/{domain}/deployment/` - 部署配置
- `build/ai-workflow/{domain}/validation/` - 验证报告
- `build/ai-workflow/{domain}/workflow/` - 执行报告

#### `generateFlinkSql`
仅执行SQL生成阶段
```bash
gradle generateFlinkSql
```

#### `validateFlinkSql`
仅执行数据验证阶段
```bash
gradle validateFlinkSql
```

#### `updateErKnowledgeBase`
仅执行ER知识库更新阶段
```bash
gradle updateErKnowledgeBase
```

### 🛠️ 辅助任务

#### `initAiWorkflow`
初始化AI工作流环境
```bash
gradle initAiWorkflow
```
- 创建必要的目录结构
- 复制配置模板
- 验证环境依赖

#### `createFlinkDomain`
为新业务域创建标准化脚手架
```bash
gradle createFlinkDomain -Pdomain=new-domain
```

#### `generateQualityReport`
生成项目整体质量报告
```bash
gradle generateQualityReport
```

#### `generateDeploymentConfig`
生成Kubernetes部署配置
```bash
gradle generateDeploymentConfig
```

### 📊 监控和维护任务

#### `watchRequestFiles`
监控request文件变更，自动触发工作流
```bash
gradle watchRequestFiles
```

#### `checkKnowledgeBaseConsistency`
检查ER知识库一致性
```bash
gradle checkKnowledgeBaseConsistency
```

---

## ⚙️ 详细配置

### 基础配置
```groovy
flinkAiWorkflow {
    // 目录配置
    workspaceDir = 'job'                    // 工作空间目录
    rulesDir = '.cursor/rules'              // AI规则文件目录
    configDir = 'job/ai-config'             // 配置文件目录
    outputDir = 'build/ai-workflow'         // 输出目录
    knowledgeBaseDir = 'job/knowledge-base' // 知识库目录
    
    // AI引擎配置
    aiProvider = 'cursor'                   // AI提供者: cursor, openai, azure, custom
    model = 'gpt-4'                         // AI模型
    workflowTimeoutMinutes = 10             // 超时时间
    maxRetries = 3                          // 重试次数
    
    // 功能开关
    enableWatch = false                     // 文件监控
    enableDeploymentGeneration = true       // 部署配置生成
    enableParallelExecution = false         // 并行执行
    enableCache = true                      // 缓存
    enableBackup = true                     // 备份
}
```

### 质量控制配置
```groovy
flinkAiWorkflow {
    // 质量门控
    qualityGateMode = 'strict'              // strict, permissive, advisory
    minQualityScore = 85                    // 最低综合评分 (0-100)
    allowWarnings = true                    // 是否允许Warning级别问题
    criticalIssuesThreshold = 0             // Critical问题容忍数量
    
    // 报告配置
    generateDetailedReports = true          // 详细报告
    reportFormat = 'markdown'               // markdown, html, json
}
```

### 业务域配置
```groovy
flinkAiWorkflow {
    // 域管理
    domains = ['wrongbook', 'user-stats']   // 包含的业务域
    excludedDomains = ['deprecated-domain'] // 排除的业务域
    
    // 域特定配置
    domainConfigs = [
        'wrongbook': 'special-config.yml',
        'user-stats': 'standard-config.yml'
    ]
}
```

### ER知识库配置
```groovy
flinkAiWorkflow {
    // 冲突检测
    conflictDetectionSensitivity = 'medium' // low, medium, high
    autoResolveCompatibleConflicts = true   // 自动解决兼容冲突
    erDiagramFormat = 'mermaid'             // mermaid, plantuml, both
}
```

### AI提供者特定配置
```groovy
flinkAiWorkflow {
    // OpenAI配置
    aiProvider = 'openai'
    aiConfig = [
        'api_key': System.getenv('OPENAI_API_KEY'),
        'base_url': 'https://api.openai.com/v1',
        'timeout': '60',
        'temperature': '0.1'
    ]
}

flinkAiWorkflow {
    // Azure配置
    aiProvider = 'azure'
    aiConfig = [
        'api_key': System.getenv('AZURE_API_KEY'),
        'endpoint': System.getenv('AZURE_ENDPOINT'),
        'api_version': '2023-05-15'
    ]
}

flinkAiWorkflow {
    // Cursor配置（默认）
    aiProvider = 'cursor'
    aiConfig = [
        'workspace_dir': '.',
        'rules_priority': 'high'
    ]
}
```

---

## 🌍 环境特定配置

### 开发环境
```groovy
if (project.hasProperty('dev')) {
    flinkAiWorkflow {
        qualityGateMode = 'permissive'      // 宽松的质量门控
        debugMode = true                    // 启用调试模式
        logLevel = 'DEBUG'                  // 详细日志
        workflowTimeoutMinutes = 20         // 更长的超时时间
        generateDetailedReports = true      // 详细报告帮助调试
    }
}
```

### 测试环境
```groovy
if (project.hasProperty('test')) {
    flinkAiWorkflow {
        qualityGateMode = 'strict'          // 严格质量门控
        minQualityScore = 90                // 更高的质量要求
        enableCache = false                 // 禁用缓存确保一致性
        generateDetailedReports = true      // 详细的测试报告
    }
}
```

### 生产环境
```groovy
if (project.hasProperty('prod')) {
    flinkAiWorkflow {
        qualityGateMode = 'strict'          // 最严格的质量门控
        minQualityScore = 95                // 生产级质量标准
        criticalIssuesThreshold = 0         // 不允许任何Critical问题
        allowWarnings = false               // 不允许Warning
        enableBackup = true                 // 强制备份
        maxRetries = 1                      // 减少重试避免延迟
    }
}
```

### 使用环境配置
```bash
# 开发环境
gradle runAiWorkflow -Pdev

# 测试环境
gradle runAiWorkflow -Ptest

# 生产环境
gradle runAiWorkflow -Pprod
```

---

## 🔄 集成到构建流程

### 1. 构建时自动生成SQL
```groovy
// 编译前生成SQL
compileJava.dependsOn 'generateFlinkSql'

// 或者
build.dependsOn 'runAiWorkflow'
```

### 2. 测试前验证质量
```groovy
test.dependsOn 'validateFlinkSql'

task integrationTest {
    dependsOn 'runAiWorkflow'
    
    doLast {
        // 运行集成测试
    }
}
```

### 3. 部署前质量检查
```groovy
task deploymentValidation {
    dependsOn 'validateFlinkSql'
    
    doLast {
        def outputDir = file("${flinkAiWorkflow.outputDir.get()}")
        def validationReports = fileTree(outputDir) {
            include '**/validation/validation-report-*.md'
        }
        
        validationReports.forEach { report ->
            // 检查验证报告中的质量评分
            def content = report.text
            if (content.contains('综合评分') && !content.contains('≥ 85分')) {
                throw new GradleException("质量评分不达标: ${report.name}")
            }
        }
        
        println "✅ 部署前质量验证通过"
    }
}
```

### 4. CI/CD集成
```groovy
// Jenkins Pipeline示例
task ciWorkflow {
    dependsOn 'runAiWorkflow'
    
    doLast {
        // 生成CI友好的报告
        def reportFile = file("${flinkAiWorkflow.outputDir.get()}/ci-report.json")
        // 生成JSON格式报告供CI系统解析
    }
}

// GitHub Actions集成
task githubActionsReport {
    dependsOn 'generateQualityReport'
    
    doLast {
        // 生成GitHub Actions友好的输出
        println "::set-output name=quality-score::${getOverallQualityScore()}"
    }
}
```

---

## 📊 任务输出和报告

### 输出目录结构
```
build/ai-workflow/
├── {domain}/                               # 业务域输出
│   ├── sql/                               # 生成的SQL文件
│   │   └── {domain}_wide_table_v3.sql
│   ├── deployment/                        # 部署配置
│   │   └── deploy-{domain}-v3.yaml
│   ├── validation/                        # 验证报告
│   │   ├── validation-report-{domain}-v3.md
│   │   └── test-data-{domain}-v3.sql
│   ├── docs/                             # 文档
│   │   └── README-AI-Generated-v3.md
│   └── workflow/                         # 工作流报告
│       └── execution-report-{domain}-v3.md
├── reports/                              # 整体报告
│   ├── overall-quality-report.md
│   ├── performance-metrics.json
│   └── task-execution-summary.md
└── cache/                               # 缓存文件（如启用）
    └── {domain}-cache.json
```

### 质量报告示例
```markdown
# Flink AI工作流执行报告

## 执行概览
- **总执行时间**: 4分钟35秒
- **处理业务域**: 2个
- **生成文件**: 22个
- **综合质量评分**: 93.45/100

## 业务域详情
### wrongbook
- ✅ SQL生成: 通过 (45秒)
- ✅ 数据验证: 通过 (质量评分: 93.45/100)
- ✅ ER知识库: 通过 (无冲突)

### user-stats  
- ✅ SQL生成: 通过 (52秒)
- ✅ 数据验证: 通过 (质量评分: 89.12/100)
- ⚠️ ER知识库: 检测到1个兼容性冲突
```

### 集成到Gradle报告
```bash
# 查看任务执行报告
gradle runAiWorkflow --console=verbose

# 生成HTML报告
gradle runAiWorkflow --scan

# 查看构建扫描
open https://gradle.com/s/your-scan-id
```

---

## 🔧 高级用法

### 1. 自定义预处理器
```groovy
flinkAiWorkflow {
    preProcessors = [
        'com.example.CustomRequestProcessor',
        'com.example.ValidationPreProcessor'
    ]
}
```

### 2. 自定义验证器
```groovy
flinkAiWorkflow {
    customValidators = [
        'com.example.BusinessRuleValidator',
        'com.example.SecurityValidator'
    ]
}
```

### 3. 并行执行配置
```groovy
flinkAiWorkflow {
    enableParallelExecution = true
    maxParallelTasks = 3
    
    // 自定义线程池配置
    aiConfig = [
        'thread_pool_size': '4',
        'queue_capacity': '100'
    ]
}
```

### 4. 缓存策略
```groovy
flinkAiWorkflow {
    enableCache = true
    cacheExpiryHours = 24
    
    // 自定义缓存键策略
    aiConfig = [
        'cache_key_strategy': 'content_hash',
        'cache_compression': 'true'
    ]
}
```

### 5. 监控和告警集成
```groovy
task monitoringIntegration {
    dependsOn 'runAiWorkflow'
    
    doLast {
        // 集成到监控系统
        def metrics = collectWorkflowMetrics()
        sendToPrometheus(metrics)
        
        // 发送告警
        if (hasQualityIssues()) {
            sendSlackNotification("AI工作流检测到质量问题")
        }
    }
}
```

---

## 🚨 故障排查

### 常见问题和解决方案

#### Q1: 插件无法找到规则文件
```groovy
// 确保规则文件路径正确
flinkAiWorkflow {
    rulesDir = '.cursor/rules'  // 检查路径是否存在
}
```

#### Q2: AI API调用失败
```groovy
// 检查API配置
flinkAiWorkflow {
    aiProvider = 'openai'
    aiConfig = [
        'api_key': System.getenv('OPENAI_API_KEY'),  // 确保环境变量设置
        'timeout': '120'  // 增加超时时间
    ]
}
```

#### Q3: 质量门控失败
```bash
# 查看详细的验证报告
gradle validateFlinkSql --info

# 使用宽松模式进行调试
gradle runAiWorkflow -Pdev
```

#### Q4: 内存不足错误
```groovy
// 增加JVM内存
gradle runAiWorkflow -Xmx4g

// 或在gradle.properties中设置
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
```

#### Q5: 并发执行问题
```groovy
flinkAiWorkflow {
    enableParallelExecution = false  // 禁用并行执行
    maxParallelTasks = 1            // 限制并发数
}
```

### 调试技巧

#### 1. 启用详细日志
```bash
gradle runAiWorkflow --debug --info
```

#### 2. 使用调试模式
```groovy
flinkAiWorkflow {
    debugMode = true
    logLevel = 'DEBUG'
}
```

#### 3. 检查中间文件
```bash
# 检查缓存状态
ls -la build/ai-workflow/cache/

# 查看状态文件
cat job/{domain}/.workflow/state.json
```

#### 4. 手动验证配置
```bash
gradle initAiWorkflow --dry-run
```

---

## 📚 示例项目

### 完整的项目示例
参考 `flink-ai-gradle-plugin/examples/` 目录中的示例项目：

```
examples/
├── build.gradle                    # 完整的插件配置示例
├── gradle.properties              # 项目属性配置
├── settings.gradle                # 项目设置
└── job/                           # 工作空间示例
    ├── wrongbook/
    │   └── wrongbook-request-v3.md
    ├── user-stats/
    │   └── user-stats-request-v3.md
    └── ai-config/
        ├── end-to-end-workflow-config.yml
        └── validation-config.yml
```

### 快速试用
```bash
# 克隆示例项目
git clone https://github.com/yangfanlin/flink-task.git
cd flink-task/flink-ai-gradle-plugin/examples

# 执行示例工作流
gradle runAiWorkflow

# 查看生成结果
ls -la build/ai-workflow/
```

---

## 🔄 版本管理和升级

### 版本兼容性
- **1.0.x**: 基础功能，兼容Gradle 7.0+
- **1.1.x**: 增强功能，兼容Gradle 7.5+
- **1.2.x**: 高级功能，兼容Gradle 8.0+

### 升级指南
```groovy
// 升级插件版本
plugins {
    id 'com.flink.ai.workflow' version '1.1.0'  // 更新版本号
}

// 检查配置兼容性
gradle help --task runAiWorkflow
```

### 版本锁定
```groovy
// gradle.properties
flinkAiWorkflow.version=1.0.0
flinkAiWorkflow.lockVersion=true
```

---

## 📞 技术支持

### 🔧 获取帮助
- **文档**: 查阅完整的[架构文档](ARCHITECTURE-AND-USAGE.md)
- **示例**: 参考`examples/`目录中的示例项目
- **问题排查**: 使用`--debug --info`选项获取详细日志

### 🚀 性能优化建议
1. **并行执行**: 在多核机器上启用并行执行
2. **缓存策略**: 开发环境启用缓存，生产环境根据需要决定
3. **资源配置**: 根据项目规模调整JVM内存和超时时间
4. **质量门控**: 根据环境调整质量门控严格程度

### 🔄 最佳实践
1. **版本控制**: 将生成的文件纳入版本控制
2. **CI/CD集成**: 在持续集成中运行质量检查
3. **监控告警**: 设置质量评分告警阈值
4. **定期维护**: 定期检查和更新知识库一致性

---

*🔧 **Gradle插件让AI工作流触手可及** - 一个命令，享受完整的AI驱动开发体验！*

**立即开始**: 将插件添加到你的`build.gradle`，运行`gradle runAiWorkflow`，体验5分钟完成完整开发周期的魅力！
