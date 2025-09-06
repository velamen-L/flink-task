# 多源业务驱动架构 - 项目结构 v2.0

## 🏗️ 工程目录结构

```
flink-multi-source-business/
├── job/                                          # 作业配置和文档目录
│   ├── flink-sql-request-template.md             # SQL作业请求模板
│   ├── wrongbook/                                # 错题本作业
│   │   ├── request.md                            # 作业需求配置（JAR作业）
│   │   ├── flink-sql-request.md                  # SQL作业请求配置
│   │   ├── sql/                                  # 生成的SQL文件
│   │   │   ├── wrongbook_wide_table.sql          # 错题宽表SQL
│   │   │   ├── wrongbook_wide_table_v2.sql       # 错题宽表SQL v2
│   │   │   └── data_validation_wrongbook_fix.sql # 数据验证SQL
│   │   ├── docs/                                 # 作业文档
│   │   │   ├── DataStream API部署说明.md          # DataStream部署说明
│   │   │   ├── 错题本Flink SQL作业配置说明.md      # SQL作业配置说明
│   │   │   └── 错题本Payload数据结构说明.md        # Payload结构说明
│   │   └── config/                               # 作业配置
│   │       └── wrongbook-job.yml                 # 作业配置文件
│   ├── user-daily-stats/                         # 用户日统计作业
│   │   └── request.md                            # 作业需求配置
│   └── learning-analysis/                        # 学习分析作业（预留）
├── src/main/java/                                # Java源码
│   └── com/flink/business/
│       ├── MultiSourceBusinessApplication.java   # 主应用入口
│       ├── core/                                 # 核心框架
│       │   ├── config/
│       │   │   ├── MultiSourceConfig.java        # 多源配置基类
│       │   │   ├── JobDomainConfig.java          # 作业域配置
│       │   │   └── GlobalConfig.java             # 全局配置
│       │   ├── processor/
│       │   │   ├── AbstractBusinessProcessor.java # 业务处理器基类
│       │   │   ├── MultiSourceProcessor.java     # 多源处理器
│       │   │   └── ProcessorFactory.java         # 处理器工厂
│       │   ├── service/
│       │   │   ├── EventSourceManager.java       # 事件源管理器
│       │   │   ├── JobDomainManager.java         # 作业域管理器
│       │   │   ├── MetricsCollector.java         # 指标收集器
│       │   │   └── ConfigLoader.java             # 配置加载器
│       │   ├── model/
│       │   │   ├── BusinessEvent.java            # 标准业务事件
│       │   │   ├── JobDomain.java                # 作业域模型
│       │   │   ├── EventSource.java              # 事件源模型
│       │   │   └── ProcessingResult.java         # 处理结果模型
│       │   └── util/
│       │       ├── JsonUtils.java                # JSON工具
│       │       ├── SqlBuilder.java               # SQL构建器
│       │       └── TableUtils.java               # Table工具
│       ├── domain/                               # 作业域处理器
│       │   ├── wrongbook/                        # 错题本域
│       │   │   ├── WrongbookJobProcessor.java    # 错题本作业处理器
│       │   │   ├── WrongbookEventHandler.java    # 错题本事件处理器
│       │   │   ├── model/
│       │   │   │   ├── WrongbookFixPayload.java  # 订正事件载荷
│       │   │   │   └── WrongbookCollectPayload.java # 收集事件载荷
│       │   │   └── service/
│       │   │       ├── WrongbookDimService.java  # 错题本维表服务
│       │   │       └── WrongbookMetrics.java     # 错题本指标
│       │   ├── userdailystats/                   # 用户日统计域
│       │   │   ├── UserDailyStatsJobProcessor.java
│       │   │   ├── UserDailyStatsEventHandler.java
│       │   │   ├── model/
│       │   │   └── service/
│       │   └── learninganalysis/                 # 学习分析域
│       │       ├── LearningAnalysisJobProcessor.java
│       │       ├── LearningAnalysisEventHandler.java
│       │       ├── model/
│       │       └── service/
│       └── infrastructure/                       # 基础设施
│           ├── kafka/
│           │   ├── KafkaSourceBuilder.java       # Kafka源构建器
│           │   └── KafkaConfigManager.java       # Kafka配置管理
│           ├── storage/
│           │   ├── JdbcConnectionManager.java    # JDBC连接管理
│           │   ├── OdpsConnectionManager.java    # ODPS连接管理
│           │   └── CacheManager.java             # 缓存管理
│           ├── monitoring/
│           │   ├── HealthChecker.java            # 健康检查
│           │   ├── AlertManager.java             # 告警管理
│           │   └── MetricsReporter.java          # 指标报告
│           └── deployment/
│               ├── JobSubmitter.java             # 作业提交器
│               └── ResourceManager.java          # 资源管理器
├── src/main/resources/                           # 资源文件
│   ├── application.yml                           # 主配置文件
│   ├── application-dev.yml                       # 开发环境配置
│   ├── application-test.yml                      # 测试环境配置
│   ├── application-prod.yml                      # 生产环境配置
│   ├── job-domains/                              # 作业域配置目录
│   │   ├── wrongbook.yml                         # 错题本作业配置
│   │   ├── user-daily-stats.yml                  # 用户统计作业配置
│   │   └── learning-analysis.yml                 # 学习分析作业配置
│   ├── sql/                                      # SQL脚本
│   │   ├── ddl/                                  # 建表语句
│   │   └── dml/                                  # 数据操作语句
│   └── META-INF/
│       └── spring.factories                      # Spring自动配置
├── src/test/java/                                # 测试代码
│   └── com/flink/business/
│       ├── core/                                 # 核心框架测试
│       ├── domain/                               # 域处理器测试
│       └── integration/                          # 集成测试
├── docs/                                         # 项目文档
│   ├── architecture/                             # 架构文档
│   │   ├── multi-source-architecture.md          # 多源架构说明
│   │   ├── job-domain-design.md                  # 作业域设计
│   │   └── event-flow-design.md                  # 事件流设计
│   ├── development/                              # 开发文档
│   │   ├── development-guide.md                  # 开发指南
│   │   ├── job-creation-guide.md                 # 作业创建指南
│   │   └── testing-guide.md                      # 测试指南
│   ├── deployment/                               # 部署文档
│   │   ├── deployment-guide.md                   # 部署指南
│   │   ├── monitoring-guide.md                   # 监控指南
│   │   └── troubleshooting.md                    # 故障排查
│   └── api/                                      # API文档
│       ├── job-domain-api.md                     # 作业域API
│       └── metrics-api.md                        # 指标API
├── scripts/                                      # 脚本文件
│   ├── build/                                    # 构建脚本
│   ├── deploy/                                   # 部署脚本
│   ├── dev/                                      # 开发脚本
│   └── ops/                                      # 运维脚本
├── docker/                                       # Docker相关
├── kubernetes/                                   # Kubernetes配置
├── monitoring/                                   # 监控配置
└── pom.xml                                       # Maven配置
```

## 🎯 关键设计原则

### 1. 作业域分离
- `/jobdomain/` 目录按作业域组织配置和文档
- 每个作业域独立管理需求、配置、SQL、文档

### 2. 代码分层
- `core/` 核心框架，通用能力
- `domain/` 按作业域分包，具体业务逻辑
- `infrastructure/` 基础设施，技术组件

### 3. 配置驱动
- 作业配置和代码分离
- 支持多环境配置
- 动态加载作业域配置

### 4. 扩展友好
- 新增作业域只需添加目录和配置
- 核心框架支持任意作业域
- 标准化的模板和规范
