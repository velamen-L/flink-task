# 目录结构调整说明 v2.0

## 🔄 目录结构变更

### 变更前
```
jobdomain/                    # 作业域配置目录
├── wrongbook/
├── user-daily-stats/
└── learning-analysis/
```

### 变更后
```
job/                          # 作业配置目录（更简洁）
├── flink-sql-request-template.md  # SQL作业请求模板
├── wrongbook/
├── user-daily-stats/
└── learning-analysis/
```

## 📋 变更原因

1. **名称简化**：`job` 比 `jobdomain` 更简洁明了
2. **用途明确**：直接表达这是作业配置目录
3. **统一模板**：在根目录提供统一的模板文件
4. **减少层级**：简化目录结构，提高可维护性

## 🏗️ 新的完整目录结构

```
flink-multi-source-business/
├── job/                                          # 作业配置目录
│   ├── flink-sql-request-template.md             # SQL作业请求模板
│   ├── wrongbook/                                # 错题本作业
│   │   ├── request.md                            # JAR作业需求配置
│   │   ├── flink-sql-request.md                  # SQL作业请求配置
│   │   ├── config/wrongbook-job.yml              # 作业配置文件
│   │   ├── sql/                                  # 生成的SQL文件
│   │   │   ├── wrongbook_wide_table.sql          # 错题宽表SQL
│   │   │   ├── wrongbook_wide_table_v2.sql       # 错题宽表SQL v2
│   │   │   └── data_validation_wrongbook_fix.sql # 数据验证SQL
│   │   └── docs/                                 # 作业文档
│   │       ├── DataStream API部署说明.md          # DataStream部署
│   │       ├── 错题本Flink SQL作业配置说明.md      # SQL配置说明
│   │       └── 错题本Payload数据结构说明.md        # Payload结构
│   ├── user-daily-stats/                         # 用户统计作业
│   │   └── request.md                            # 作业需求配置
│   └── learning-analysis/                        # 学习分析作业（预留）
├── src/main/java/com/flink/business/              # Java源码
│   ├── MultiSourceBusinessApplication.java       # 统一主应用入口
│   ├── core/                                     # 核心框架
│   │   ├── config/                               # 全局配置
│   │   │   ├── GlobalConfig.java                 # 全局配置
│   │   │   └── JobDomainConfig.java              # 作业域配置
│   │   ├── service/                              # 核心服务
│   │   │   ├── JobDomainManager.java             # 作业域管理
│   │   │   ├── EventSourceManager.java          # 事件源管理
│   │   │   ├── MetricsCollector.java             # 指标收集
│   │   │   └── ConfigLoader.java                 # 配置加载
│   │   └── processor/                            # 处理器框架
│   │       ├── AbstractBusinessProcessor.java   # 处理器基类
│   │       └── ProcessorFactory.java             # 处理器工厂
│   └── domain/                                   # 域处理器
│       └── wrongbook/                            # 错题本域
│           └── WrongbookJobProcessor.java        # 错题本处理器
├── src/main/resources/                           # 资源文件
│   ├── application.yml                           # 应用配置
│   └── job-domains/                              # 作业域配置（可选）
├── config/                                       # 全局配置
│   ├── catalog-config.yaml                      # Catalog配置
│   └── env-template.env                          # 环境变量模板
└── docs/                                         # 项目文档
    └── architecture/                             # 架构文档
        ├── multi-source-business-driven-architecture-v2.md
        └── directory-structure-update-v2.md
```

## 🔄 相关文件更新

### 1. 配置加载器更新
`ConfigLoader.java` 已更新配置加载路径：
- 从 `jobdomain/{name}/config/` 改为 `job/{name}/config/`
- 配置文件查找逻辑相应调整

### 2. AI规则更新
- `flink-jar-job-generator.mdc` 更新目录结构说明
- `intelligent-sql-job-generator.mdc` 更新输入输出路径

### 3. 架构文档更新
- `multi-source-business-driven-architecture-v2.md` 更新目录结构
- `project-structure-v2.md` 同步最新结构

## 🚀 使用方式

### 启动特定作业
```bash
# 启动错题本作业
java -jar app.jar wrongbook

# 启动用户统计作业  
java -jar app.jar user-daily-stats
```

### 新增作业
```bash
# 1. 创建作业目录
mkdir -p job/new-job/{config,sql,docs}

# 2. 复制模板文件
cp job/flink-sql-request-template.md job/new-job/flink-sql-request.md

# 3. 编辑配置文件
vim job/new-job/flink-sql-request.md

# 4. 使用AI规则生成代码
# 调用 intelligent-sql-job-generator 或 flink-jar-job-generator

# 5. 启动作业
java -jar app.jar new-job
```

### 配置文件结构

#### JAR作业配置 (`request.md`)
用于生成完整的JAR作业，包含Java代码和配置文件。

#### SQL作业配置 (`flink-sql-request.md`)
用于生成纯Flink SQL作业文件。

#### 作业配置文件 (`config/{name}-job.yml`)
用于JAR作业的详细配置，包含事件源、维表、输出等配置。

## 📈 架构优势

### ✅ 更加清晰
- 目录名称更直观，`job` 比 `jobdomain` 更简洁
- 模板文件统一放在根目录，便于查找
- 支持SQL和JAR两种作业类型的配置

### ✅ 易于维护
- 减少目录层级，降低复杂度
- 统一的配置加载逻辑
- 标准化的作业创建流程

### ✅ 扩展友好
- 新增作业只需创建对应目录
- 模板文件复用，保证一致性
- AI规则自动适配新结构

## 🎯 总结

这次目录结构调整是一个渐进式的优化：
1. **保持了核心架构不变**：多源业务驱动模式
2. **简化了目录结构**：减少不必要的复杂性
3. **提升了用户体验**：更直观的目录命名
4. **增强了可维护性**：标准化的模板和配置

新的 `job/` 目录结构更符合开发者的直觉，同时保持了原有架构的所有优势。
