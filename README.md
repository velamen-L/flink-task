# Flink实时计算项目 - 混合架构版 🚀

这是一个基于Apache Flink的实时数据流处理项目脚手架，采用**混合架构**设计，支持**动态路由**和**热部署**，提供了完整的事件驱动架构实现和AI编程脚手架工具。

## 🎯 核心特性

### **架构特性**
- **动态路由**: 基于数据库配置的事件类型路由，30秒内热更新
- **热部署**: 新增事件处理器无需重启作业  
- **故障隔离**: 单个事件类型处理失败不影响其他类型
- **多输出支持**: 支持主流（宽表）和侧流（告警、指标、审计）输出

### **开发特性**
- **统一事件模型**: 标准化的业务事件数据格式
- **双模式支持**: DataStream API和Flink SQL两种开发模式
- **维表关联**: 支持MySQL维表实时关联
- **AI代码生成**: 根据表结构自动生成完整作业代码
- **阿里云优化**: 针对阿里云Flink的深度优化
- **容器化部署**: 完整的Docker部署方案

## 📁 项目结构

```
flink-realtime-project/
├── src/main/java/com/flink/realtime/
│   ├── app/                    # 应用程序入口
│   │   ├── BusinessDataStreamApp.java   # DataStream API应用
│   │   ├── BusinessSqlApp.java          # Flink SQL应用
│   │   ├── DataStreamApp.java           # 原始示例应用
│   │   └── SqlApp.java                  # 原始SQL应用
│   ├── bean/                   # 数据模型
│   │   ├── BaseBean.java       # 基础数据对象
│   │   ├── BusinessEvent.java  # 业务事件模型
│   │   └── UserEvent.java      # 用户事件示例
│   ├── processor/              # 事件处理器
│   │   ├── EventProcessor.java          # 处理器接口
│   │   ├── EventProcessorFactory.java   # 处理器工厂
│   │   └── impl/
│   │       └── UserEventProcessor.java  # 用户事件处理器示例
│   ├── source/                 # 数据源
│   │   └── MySQLDimSource.java  # MySQL维表数据源
│   ├── sink/                   # 数据输出
│   │   ├── MySQLSinkFunction.java # MySQL结果输出
│   │   └── PrintSinkFunction.java # 控制台输出
│   ├── function/               # 序列化器和UDF
│   │   ├── BusinessEventDeserializationSchema.java
│   │   └── JsonDeserializationSchema.java
│   ├── common/                 # 通用工具类
│   │   └── FlinkUtils.java     # Flink环境配置
│   └── util/                   # 工具类
│       └── ConfigUtils.java    # 配置管理
├── src/main/resources/
│   ├── application.properties  # 应用配置
│   ├── log4j2.xml             # 日志配置
│   └── sql/                   # SQL脚本
├── docs/                      # 文档
│   ├── 架构介绍.md             # 项目架构详细介绍
│   ├── 开发规范.md             # Flink作业开发规范
│   └── AI编程脚手架使用指南.md   # AI工具使用指南
├── scripts/                   # AI编程脚手架
│   ├── job-generator.py       # 代码生成器
│   ├── run-example.sh        # 运行示例
│   └── examples/             # 配置示例
│       └── user-job-config.json
└── docker/
    └── docker-compose.yml     # Docker容器编排
```

## ⚡ 快速开始

### 1. 环境准备

```bash
# 克隆项目
git clone <repository-url>
cd flink-realtime-project

# 确保已安装JDK 11+、Maven、MySQL、Docker
java -version
mvn -version
mysql --version
docker --version
```

### 2. 初始化混合架构

```bash
# 编译项目
mvn clean package

# 初始化数据库配置
mysql -u root -p < sql/init_dynamic_routing.sql

# 启动错题本域动态路由作业（示例）
scripts/start-dynamic-routing.sh wrongbook --init-config --parallelism 4
```

### 3. 使用AI脚手架生成代码

```bash
# 进入脚手架目录
cd scripts

# 生成错题本场景的纯SQL版本（阿里云部署）
python3 aliyun-sql-generator.py examples/wrongbook-job-config.json

# 生成DataStream API版本
python3 job-generator.py examples/wrongbook-job-config.json
```

### 4. 动态配置管理

```bash
# 查看当前配置
scripts/start-dynamic-routing.sh wrongbook --config-only

# 添加新的事件类型（热更新，30秒内生效）
java -cp target/flink-realtime-project-1.0-SNAPSHOT.jar \
  com.flink.realtime.config.RoutingConfigManager \
  add wrongbook wrongbook_review com.flink.realtime.processor.impl.WrongbookReviewProcessor
```

## 🏗️ 架构概述

### 混合架构流向

```
业务系统 → Kafka按域分Topic → Flink动态路由 → 多输出流
                                    ↓
                        ┌────────────────────────────┐
                        │    动态路由处理引擎        │
                        │  ┌──────────────────────┐  │
                        │  │  配置数据库          │  │
                        │  │  (30秒热更新)      │  │
                        │  └──────────────────────┘  │
                        │  ┌──────────────────────┐  │
                        │  │  处理器缓存          │  │
                        │  │  (动态加载)        │  │
                        │  └──────────────────────┘  │
                        └────────────────────────────┘
                                    ↓
                    ┌─────────────────────────────────┐
                    │          输出路由               │
                    └─────────────────────────────────┘
                ├─────────┼─────────┼─────────┼─────────┤
               主流      告警流    指标流    审计流    死信流
                ↓         ↓        ↓        ↓         ↓
           MySQL宽表   Kafka     Kafka    Kafka   DeadLetter
                      Alert     Metrics   Audit     Queue
```

### 统一事件格式

```json
{
  "domain": "user",           // 业务域
  "type": "user_login",       // 事件类型
  "timestamp": 1642567890000, // 事件时间戳
  "eventId": "uuid-123",      // 事件唯一ID
  "payload": {                // 载荷数据
    "userId": "user123",
    "loginTime": 1642567890000,
    "device": "mobile"
  },
  "version": "1.0",           // 数据版本
  "source": "user-service"    // 来源系统
}
```

## 🤖 AI编程脚手架

### 功能特性

- **表结构输入**: 支持源表、维表、结果表结构定义
- **双模式生成**: 自动生成DataStream API和Flink SQL版本
- **完整代码**: 包含应用入口、事件处理器、配置文件等
- **最佳实践**: 遵循开发规范，包含异常处理和日志记录

### 使用步骤

1. **定义配置文件**: 参考`scripts/examples/user-job-config.json`
2. **运行生成器**: `python job-generator.py config.json`
3. **集成代码**: 将生成的代码集成到项目中
4. **自定义逻辑**: 根据业务需求调整处理逻辑

### 配置示例

```json
{
  "domain": "user",
  "job_name": "用户事件处理作业",
  "source_table": {
    "name": "user_event_source",
    "fields": [...]
  },
  "dim_tables": [...],
  "result_table": {...},
  "event_types": ["user_login", "user_purchase"]
}
```

## 📖 开发指南

### DataStream API模式

```java
// 1. 创建执行环境
StreamExecutionEnvironment env = FlinkUtils.getStreamExecutionEnvironment(1);

// 2. 创建Kafka数据源
KafkaSource<BusinessEvent> kafkaSource = createKafkaSource();

// 3. 事件处理和维表关联
processedStream = eventStream
    .connect(broadcastDimStream)
    .process(new BusinessEventProcessor());

// 4. 写入结果表
processedStream.addSink(new MySQLSinkFunction<>(...));
```

### Flink SQL模式

```java
// 1. 创建表环境
StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

// 2. 创建源表、维表、结果表
createKafkaSourceTable(tableEnv);
createMySQLDimTable(tableEnv);
createMySQLSinkTable(tableEnv);

// 3. 执行业务SQL
tableEnv.executeSql("INSERT INTO result_table SELECT ... FROM source_table ...");
```

### 事件处理器开发

```java
public class UserEventProcessor implements EventProcessor {
    @Override
    public Object process(BusinessEvent event) throws Exception {
        switch (event.getType()) {
            case "user_login":
                return processUserLogin(event.getPayload());
            // 更多事件类型处理...
        }
    }
}
```

## 🔧 配置说明

项目配置文件位于 `src/main/resources/application.properties`：

```properties
# Kafka配置
kafka.bootstrap.servers=localhost:9092
kafka.input.topic=business-events
kafka.group.id=flink-consumer-group

# MySQL配置
mysql.url=jdbc:mysql://localhost:3306/flink_db
mysql.username=root
mysql.password=root

# Flink配置
flink.parallelism=1
flink.checkpoint.interval=5000
```

## 🐳 部署方案

### 本地部署

```bash
# 启动依赖服务
cd docker
docker-compose up -d

# 启动Flink集群
start-cluster.sh

# 提交作业
flink run target/flink-realtime-project-1.0-SNAPSHOT.jar
```

### 生产部署

详细的生产部署指南请参考：[docs/开发规范.md](docs/开发规范.md)

## 📚 文档

- [架构介绍](docs/架构介绍.md) - 详细的系统架构说明
- [混合架构升级说明](docs/混合架构升级说明.md) - 混合架构特性和使用指南 🔥
- [开发规范](docs/开发规范.md) - Flink作业开发标准规范
- [AI编程脚手架使用指南](docs/AI编程脚手架使用指南.md) - 代码生成器详细使用说明
- [阿里云架构调整说明](docs/阿里云架构调整说明.md) - 阿里云Flink优化指南

## 🤝 贡献指南

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙋‍♂️ 支持

如有问题或建议，请：

1. 查看文档和示例
2. 提交Issue
3. 联系维护者

---

**注意**: 这是一个脚手架项目，生成的代码需要根据具体业务需求进行调整和完善。