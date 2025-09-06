# 错题本系统DataStream API部署说明

## 📋 概述

本文档描述了错题本系统DataStream API版本的完整部署方案，包括动态路由配置、热部署机制和故障隔离策略。

## 🏗️ 架构设计

### 核心特性
- ✅ **动态路由**: 支持事件类型动态路由
- ✅ **热部署**: 支持配置热更新
- ✅ **故障隔离**: 支持故障隔离机制
- ✅ **监控告警**: 完整的监控和告警体系

### 数据流架构
```
Kafka事件源 → 动态路由处理 → 事件处理器 → 输出路由 → MySQL宽表
     ↓              ↓              ↓           ↓
  配置广播流 → 路由配置管理 → 处理器工厂 → 多路输出
```

## 📦 项目结构

### 包结构
```
com.flink.realtime.topics.wrongbook
├── app
│   └── WrongbookWideTableApp.java          # 主应用类
├── bean
│   └── WrongbookPayload.java               # Payload数据结构
└── processor
    ├── WrongbookAddProcessor.java          # 错题添加处理器
    └── WrongbookFixProcessor.java          # 错题订正处理器
```

### 文件结构
```
topics/wrongbook/
├── sql/
│   └── wrongbook_wide_table_hybrid.sql     # Flink SQL作业
├── docs/
│   ├── DataStream API部署说明.md           # 本文档
│   ├── 错题本Payload数据结构说明.md       # Payload文档
│   └── 错题本Flink SQL作业配置说明.md     # SQL作业文档
└── java/                                   # Java源码（已移至src目录）
```

## 🔧 配置管理

### 1. 应用配置 (application.properties)
```properties
# Flink配置
flink.parallelism=4
flink.checkpoint.interval=60000
flink.checkpoint.timeout=30000

# Kafka配置
kafka.bootstrap.servers=localhost:9092
kafka.username=your_username
kafka.password=your_password

# MySQL配置
mysql.url=jdbc:mysql://pc-bp1ivlu7lykwyzx9x.rwlb.rds.aliyuncs.com:3306/shuxue
mysql.username=zstt_server
mysql.password=******

# 动态路由配置
routing.config.source.type=database
routing.config.database.url=jdbc:mysql://localhost:3306/config_db
routing.config.database.table=routing_configs
routing.config.refresh.interval=30000

# 监控配置
metrics.reporter.type=aliyun
metrics.reporter.interval=60000
```

### 2. 动态路由配置表
```sql
CREATE TABLE routing_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    domain VARCHAR(50) NOT NULL COMMENT '业务域',
    event_type VARCHAR(100) NOT NULL COMMENT '事件类型',
    processor_class VARCHAR(200) NOT NULL COMMENT '处理器类名',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    priority INT DEFAULT 0 COMMENT '优先级',
    config_json TEXT COMMENT '处理器配置JSON',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_domain_type (domain, event_type)
) COMMENT '动态路由配置表';
```

### 3. 路由配置示例
```json
{
  "domain": "wrongbook",
  "event_type": "wrongbook_add",
  "processor_class": "com.flink.realtime.topics.wrongbook.processor.WrongbookAddProcessor",
  "enabled": 1,
  "priority": 0,
  "config_json": {
    "cache_ttl": "10min",
    "batch_size": 1000,
    "retry_times": 3
  }
}
```

## 🚀 部署步骤

### 1. 环境准备
```bash
# 检查Java版本
java -version  # 需要JDK 17

# 检查Maven版本
mvn -version   # 需要Maven 3.6+

# 编译项目
mvn clean compile package -DskipTests
```

### 2. 配置文件准备
```bash
# 复制配置文件模板
cp config/env-template.env config/application.properties

# 编辑配置文件
vim config/application.properties
```

### 3. 数据库初始化
```sql
-- 创建配置数据库
CREATE DATABASE IF NOT EXISTS config_db;
USE config_db;

-- 创建路由配置表
CREATE TABLE routing_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    domain VARCHAR(50) NOT NULL COMMENT '业务域',
    event_type VARCHAR(100) NOT NULL COMMENT '事件类型',
    processor_class VARCHAR(200) NOT NULL COMMENT '处理器类名',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    priority INT DEFAULT 0 COMMENT '优先级',
    config_json TEXT COMMENT '处理器配置JSON',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_domain_type (domain, event_type)
) COMMENT '动态路由配置表';

-- 插入初始配置
INSERT INTO routing_configs (domain, event_type, processor_class, enabled, priority) VALUES
('wrongbook', 'wrongbook_add', 'com.flink.realtime.topics.wrongbook.processor.WrongbookAddProcessor', 1, 0),
('wrongbook', 'wrongbook_fix', 'com.flink.realtime.topics.wrongbook.processor.WrongbookFixProcessor', 1, 0);
```

### 4. 启动作业
```bash
# 方式1：本地运行
mvn exec:java -Dexec.mainClass="com.flink.realtime.topics.wrongbook.app.WrongbookWideTableApp"

# 方式2：提交到Flink集群
flink run -c com.flink.realtime.topics.wrongbook.app.WrongbookWideTableApp \
    target/flink-realtime-project-1.0.0.jar

# 方式3：阿里云Flink VVR
# 通过阿里云控制台提交作业
```

## 🔄 动态路由机制

### 1. 路由配置源
```java
// DynamicRoutingConfigSource.java
public class DynamicRoutingConfigSource implements SourceFunction<RoutingConfig> {
    
    @Override
    public void run(SourceContext<RoutingConfig> ctx) throws Exception {
        while (isRunning) {
            // 从数据库查询最新配置
            List<RoutingConfig> configs = queryLatestConfigs();
            
            // 发送配置更新
            for (RoutingConfig config : configs) {
                ctx.collect(config);
            }
            
            // 等待下次刷新
            Thread.sleep(refreshInterval);
        }
    }
}
```

### 2. 动态路由处理
```java
// DynamicRoutingProcessFunction.java
public class DynamicRoutingProcessFunction extends BroadcastProcessFunction<BusinessEvent, RoutingConfig, ProcessedEvent> {
    
    @Override
    public void processElement(BusinessEvent event, ReadOnlyContext ctx, Collector<ProcessedEvent> out) throws Exception {
        // 获取当前路由配置
        ReadOnlyBroadcastState<String, RoutingConfig> configState = ctx.getBroadcastState(configDescriptor);
        
        // 根据事件类型查找处理器
        String eventType = event.getType();
        RoutingConfig config = configState.get(eventType);
        
        if (config != null && config.isEnabled()) {
            // 动态创建处理器
            EventProcessor processor = EventProcessorFactory.createProcessor(config.getProcessorClass());
            
            // 处理事件
            Object result = processor.process(event);
            
            // 构建输出事件
            ProcessedEvent processedEvent = new ProcessedEvent(event, result, config.getOutputConfig());
            out.collect(processedEvent);
        }
    }
}
```

### 3. 处理器工厂
```java
// EventProcessorFactory.java
public class EventProcessorFactory {
    
    private static final Map<String, EventProcessor> processorCache = new ConcurrentHashMap<>();
    
    public static EventProcessor createProcessor(String className) throws Exception {
        return processorCache.computeIfAbsent(className, k -> {
            try {
                Class<?> clazz = Class.forName(className);
                return (EventProcessor) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create processor: " + className, e);
            }
        });
    }
}
```

## 🔥 热部署机制

### 1. 配置热更新
```java
@Override
public void processBroadcastElement(RoutingConfig config, Context ctx, Collector<ProcessedEvent> out) throws Exception {
    // 更新广播状态
    BroadcastState<String, RoutingConfig> configState = ctx.getBroadcastState(configDescriptor);
    configState.put(config.getEventType(), config);
    
    logger.info("路由配置已更新: {} -> {}", config.getEventType(), config.getProcessorClass());
}
```

### 2. 处理器热加载
```java
public class HotDeployProcessorLoader {
    
    public static EventProcessor loadProcessor(String className) throws Exception {
        // 使用自定义类加载器
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl});
        
        // 加载处理器类
        Class<?> processorClass = classLoader.loadClass(className);
        
        // 创建实例
        return (EventProcessor) processorClass.getDeclaredConstructor().newInstance();
    }
}
```

### 3. 热部署配置
```properties
# 热部署配置
hot.deploy.enabled=true
hot.deploy.jar.path=/path/to/processor-jar.jar
hot.deploy.check.interval=30000
hot.deploy.class.path=com.flink.realtime.topics.wrongbook.processor
```

## 🛡️ 故障隔离机制

### 1. 处理器隔离
```java
public class IsolatedEventProcessor implements EventProcessor {
    
    private final EventProcessor delegate;
    private final CircuitBreaker circuitBreaker;
    
    @Override
    public Object process(BusinessEvent event) throws Exception {
        return circuitBreaker.executeCallable(() -> delegate.process(event));
    }
}
```

### 2. 熔断器配置
```java
public class CircuitBreaker {
    
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final int failureThreshold;
    private final int successThreshold;
    private volatile State state = State.CLOSED;
    
    public <T> T executeCallable(Callable<T> callable) throws Exception {
        if (state == State.OPEN) {
            throw new CircuitBreakerOpenException("Circuit breaker is open");
        }
        
        try {
            T result = callable.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }
}
```

### 3. 故障恢复策略
```properties
# 故障隔离配置
fault.isolation.enabled=true
fault.isolation.circuit.breaker.enabled=true
fault.isolation.failure.threshold=5
fault.isolation.success.threshold=3
fault.isolation.timeout.ms=30000
fault.isolation.fallback.enabled=true
```

## 📊 监控和告警

### 1. 监控指标
```java
public class WrongbookMetricsReporter {
    
    private final Counter eventProcessedCounter;
    private final Counter eventErrorCounter;
    private final Histogram processingTimeHistogram;
    private final Gauge<Integer> activeProcessorGauge;
    
    public void recordEventProcessed(String eventType) {
        eventProcessedCounter.inc();
    }
    
    public void recordEventError(String eventType, String error) {
        eventErrorCounter.inc();
    }
    
    public void recordProcessingTime(long duration) {
        processingTimeHistogram.update(duration);
    }
}
```

### 2. 告警规则
```yaml
# 告警配置
alerts:
  - name: "processing_latency_high"
    condition: "processing_time > 30s"
    severity: "warning"
    
  - name: "error_rate_high"
    condition: "error_rate > 5%"
    severity: "critical"
    
  - name: "processor_failure"
    condition: "processor_failure_count > 10"
    severity: "critical"
```

### 3. 监控面板
```json
{
  "dashboard": {
    "title": "错题本系统监控",
    "panels": [
      {
        "title": "事件处理速率",
        "type": "graph",
        "targets": ["events_processed_per_second"]
      },
      {
        "title": "处理延迟",
        "type": "graph", 
        "targets": ["processing_latency_p95"]
      },
      {
        "title": "错误率",
        "type": "graph",
        "targets": ["error_rate_percentage"]
      }
    ]
  }
}
```

## 🔧 运维操作

### 1. 配置更新
```sql
-- 更新处理器配置
UPDATE routing_configs 
SET config_json = '{"cache_ttl": "15min", "batch_size": 2000}',
    update_time = CURRENT_TIMESTAMP
WHERE domain = 'wrongbook' AND event_type = 'wrongbook_add';

-- 启用/禁用处理器
UPDATE routing_configs 
SET enabled = 0 
WHERE domain = 'wrongbook' AND event_type = 'wrongbook_delete';
```

### 2. 作业管理
```bash
# 查看作业状态
flink list

# 停止作业
flink cancel <job_id>

# 重启作业
flink run -c com.flink.realtime.topics.wrongbook.app.WrongbookWideTableApp \
    target/flink-realtime-project-1.0.0.jar

# 查看作业日志
flink logs <job_id>
```

### 3. 故障排查
```bash
# 查看错误日志
grep "ERROR" logs/flink-*.log

# 查看性能指标
curl http://localhost:8081/jobs/<job_id>/metrics

# 查看任务管理器状态
curl http://localhost:8081/taskmanagers
```

## 📝 最佳实践

### 1. 性能优化
- 使用合适的并行度（建议4-8）
- 配置合理的检查点间隔（60秒）
- 启用维表缓存（10-30分钟TTL）
- 使用批量写入（1000行/批次）

### 2. 稳定性保障
- 启用故障隔离机制
- 配置熔断器保护
- 设置合理的重试策略
- 监控关键指标

### 3. 扩展性设计
- 支持动态路由配置
- 实现处理器热部署
- 提供配置热更新
- 支持多路输出

## 📚 相关文档

- [Payload数据结构说明](./错题本Payload数据结构说明.md)
- [Flink SQL作业配置说明](./错题本Flink SQL作业配置说明.md)
- [项目架构说明](../../../docs/架构介绍.md)
- [开发规范](../../../docs/开发规范.md)
