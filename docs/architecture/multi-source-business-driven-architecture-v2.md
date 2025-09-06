# 多源业务驱动架构 v2.0

## 🎯 架构概述

多源业务驱动架构 v2.0 是为了解决事件域和作业域不匹配问题而设计的新一代 Flink 实时计算架构。该架构支持一个作业消费多个事件域的数据，实现真正的业务驱动开发模式。

### 🔥 核心特性

- **事件域与作业域分离**：事件按产生上下文分域，作业按业务目标分域
- **多源数据处理**：支持 UNION、JOIN、AGGREGATE 三种多源处理模式
- **配置驱动开发**：所有业务逻辑通过配置文件定义，无需修改代码
- **统一技术栈**：基于 Table API，声明式编程，易于维护
- **企业级特性**：完整的监控、告警、数据质量保障体系

## 🏗️ 架构分层设计

### 第一层：事件域层 (Event Domain Layer)
```
错题本域 (wrongbook)     答题域 (answer)        用户域 (user)
├── wrongbook_fix       ├── answer_submit      ├── user_login  
├── wrongbook_collect   ├── answer_complete    ├── user_profile_update
└── wrongbook_delete    └── answer_review      └── user_logout
```

### 第二层：事件总线层 (Event Bus Layer)
```
biz_statistic_wrongbook → 错题本域所有事件
biz_statistic_answer    → 答题域所有事件  
biz_statistic_user      → 用户域所有事件
```

### 第三层：作业域层 (Job Domain Layer)
```
错题本作业            用户日统计作业           学习分析作业
├── 消费: wrongbook   ├── 消费: wrongbook     ├── 消费: wrongbook
└── 目标: 错题宽表    ├── 消费: answer        ├── 消费: answer  
                     ├── 消费: user          └── 目标: 学习分析
                     └── 目标: 日活统计
```

## 📁 工程目录结构

```
flink-multi-source-business/
├── jobdomain/                                    # 作业域配置目录
│   ├── wrongbook/                                # 错题本作业域
│   │   ├── request.md                            # 业务需求配置
│   │   ├── config/wrongbook-job.yml              # 作业配置
│   │   ├── sql/wrongbook_wide_table.sql          # 生成的SQL
│   │   └── docs/business-logic.md                # 业务文档
│   ├── user-daily-stats/                         # 用户统计作业域
│   └── learning-analysis/                        # 学习分析作业域
├── src/main/java/com/flink/business/
│   ├── MultiSourceBusinessApplication.java       # 主应用
│   ├── core/                                     # 核心框架
│   │   ├── config/GlobalConfig.java              # 全局配置
│   │   ├── service/JobDomainManager.java         # 作业域管理
│   │   └── processor/AbstractBusinessProcessor.java # 处理器基类
│   └── domain/                                   # 域处理器
│       ├── wrongbook/WrongbookJobProcessor.java  # 错题本处理器
│       ├── userdailystats/                       # 用户统计处理器
│       └── learninganalysis/                     # 学习分析处理器
└── docs/                                         # 项目文档
```

## 🔧 三种处理模式详解

### 🟢 UNION 模式：单域多事件处理

**适用场景**：处理同一域的多种事件类型  
**典型案例**：错题本作业（处理 wrongbook_fix + wrongbook_collect）

```yaml
processing-strategy:
  processing-mode: "UNION"
  
event-sources:
  - event-domain: "wrongbook"
    interested-event-types: ["wrongbook_fix", "wrongbook_collect"]
```

**处理流程**：
1. 从 `biz_statistic_wrongbook` 消费数据
2. 过滤出感兴趣的事件类型
3. UNION 合并多种事件
4. 统一业务逻辑处理

### 🟡 JOIN 模式：多域事件关联处理

**适用场景**：需要关联多个域的事件  
**典型案例**：用户日统计（wrongbook + answer + user 三域关联）

```yaml
processing-strategy:
  processing-mode: "JOIN"
  
event-sources:
  - event-domain: "wrongbook"
    interested-event-types: ["wrongbook_fix"]
  - event-domain: "answer"
    interested-event-types: ["answer_submit"]
  - event-domain: "user"
    interested-event-types: ["user_login"]
```

**处理流程**：
1. 分别消费三个域的事件
2. 按 user_id + event_date 进行 JOIN
3. 计算跨域综合指标
4. 输出用户日活统计

### 🔴 AGGREGATE 模式：多域数据聚合分析

**适用场景**：需要对多个域进行聚合计算  
**典型案例**：学习分析（wrongbook + answer 域聚合分析）

```yaml
processing-strategy:
  processing-mode: "AGGREGATE"
  time-window:
    window-type: "TUMBLING"
    window-size: 3600000  # 1小时窗口
```

**处理流程**：
1. 分别聚合各域数据
2. 跨域聚合生成综合指标
3. 窗口化处理支持实时分析
4. 输出学习效果评估

## 💻 核心组件说明

### 1. MultiSourceBusinessApplication
主应用入口，支持指定作业域启动：
```bash
java -jar app.jar wrongbook                    # 启动错题本作业
java -jar app.jar user-daily-stats            # 启动用户统计作业
```

### 2. JobDomainManager
作业域管理器，负责：
- 作业域配置验证和加载
- 处理器创建和生命周期管理
- 运行时监控和状态管理

### 3. AbstractBusinessProcessor
业务处理器基类，提供：
- 统一的多源处理框架
- 标准的数据源注册流程
- 可扩展的业务规则接口
- 完整的监控指标收集

### 4. 域处理器（Domain Processors）
具体业务逻辑实现：
- **WrongbookJobProcessor**：错题本宽表生成
- **UserDailyStatsJobProcessor**：用户日活统计  
- **LearningAnalysisJobProcessor**：学习效果分析

## 📊 配置驱动开发

### 业务需求配置 (request.md)
```markdown
## 作业基本信息
job_name: "wrongbook-wide-table-job"
job_type: "SINGLE_DOMAIN"
business_goals: ["错题宽表", "订正分析"]

## 事件源配置
- wrongbook_fix (错题订正)
- wrongbook_collect (错题收集)

## 字段映射配置
- id: payload.fixId
- user_id: payload.userId
- subject_name: 科目名称转换规则
```

### 作业配置 (wrongbook-job.yml)
```yaml
metadata:
  job-name: "wrongbook-wide-table-job"
  job-type: "SINGLE_DOMAIN"

event-sources:
  - source-name: "wrongbook_events"
    topic-name: "biz_statistic_wrongbook"
    interested-event-types: ["wrongbook_fix", "wrongbook_collect"]

dim-tables:
  - table-name: "tower_pattern"
    join-condition: "pt.id = payload.patternId"

outputs:
  - target-name: "dwd_wrong_record_wide_delta"
    connector: "odps"
```

## 🚀 开发工作流

### 1. 新增作业域
```bash
# 1. 创建作业域目录
mkdir -p jobdomain/new-domain/{config,sql,docs}

# 2. 编写业务需求
cp templates/request-template.md jobdomain/new-domain/request.md

# 3. 生成作业配置
# 使用 AI 规则生成配置文件

# 4. 实现域处理器（可选，大部分场景使用通用处理器）
# 创建 NewDomainJobProcessor.java
```

### 2. 本地开发测试
```bash
# 启动特定作业域
mvn spring-boot:run -Dspring-boot.run.arguments="new-domain"

# 查看监控指标
curl http://localhost:8080/actuator/metrics
```

### 3. 部署到生产
```bash
# 构建JAR包
mvn clean package

# 提交到Flink集群
flink run -c com.flink.business.MultiSourceBusinessApplication \
  target/multi-source-business-app.jar new-domain
```

## 📈 监控和运维

### 1. 核心监控指标
- **业务指标**：事件处理量、JOIN成功率、数据质量
- **性能指标**：处理延迟、吞吐量、资源使用率
- **可用性指标**：作业健康度、错误率、恢复时间

### 2. 告警规则
```yaml
alerts:
  - metric: "join_success_rate"
    condition: "< 0.95"
    severity: "WARNING"
  - metric: "processing_latency_p95"
    condition: "> 300000"  # 5分钟
    severity: "CRITICAL"
```

### 3. 数据质量保障
- **字段完整性检查**：必需字段不能为空
- **值范围验证**：枚举值和数值范围检查
- **业务规则校验**：复杂业务逻辑验证
- **实时质量监控**：质量指标实时计算和告警

## 🎯 架构优势总结

### ✅ 解决核心问题
- **域不匹配**：事件域和作业域灵活映射
- **跨域处理**：支持一个作业消费多个事件域
- **配置复杂**：声明式配置，简化开发

### ✅ 企业级特性
- **可维护性**：配置驱动，业务逻辑清晰
- **可扩展性**：新增作业域无需修改框架代码
- **可观测性**：完整的监控和调试体系
- **高可用性**：容错机制和状态管理

### ✅ 开发效率
- **标准化**：统一的开发模式和最佳实践
- **自动化**：AI辅助的配置生成和代码生成
- **协作友好**：清晰的目录结构和文档规范

## 📚 扩展阅读

- [作业域开发指南](../development/job-domain-development-guide.md)
- [配置文件规范](../development/configuration-specification.md)
- [监控运维手册](../deployment/monitoring-ops-guide.md)
- [最佳实践案例](../examples/best-practices.md)
