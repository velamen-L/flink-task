# Flink实时计算架构时序图 - Mermaid格式

## 📊 优化后架构完整时序图

```mermaid
sequenceDiagram
    participant KS as Kafka事件源
    participant CWT as CommonWideTableApp<br/>(统一入口)
    participant RC as RoutingConfig<br/>(路由配置)
    participant DRF as DynamicRoutingProcessFunction
    participant EPF as EventProcessorFactory
    participant EP as EventProcessor<br/>(业务处理器)
    participant DTS as DimTableQueryService<br/>(维表查询服务)
    participant USS as UnifiedSinkService<br/>(统一输出服务)
    participant MySQL as MySQL数据库
    participant KO as Kafka输出

    Note over KS, KO: Flink实时计算架构时序图 - 优化版

    %% 1. 作业启动阶段
    Note over CWT: 1. 作业启动阶段
    CWT->>CWT: 创建执行环境
    CWT->>KS: 创建Kafka事件源
    CWT->>RC: 创建路由配置广播流
    CWT->>DRF: 创建动态路由处理函数
    Note over CWT: execute(topic) 启动作业

    %% 2. 事件处理阶段  
    Note over KS, KO: 2. 事件处理阶段

    KS->>DRF: 接收BusinessEvent<br/>{domain:"wrongbook", type:"wrongbook_add"}
    RC->>DRF: 广播路由配置<br/>{wrongbook:wrongbook_add → WrongbookAddProcessor}
    
    DRF->>DRF: 构建路由键<br/>routingKey = "wrongbook:wrongbook_add"
    DRF->>EPF: 获取处理器<br/>getProcessor(routingKey)
    EPF-->>DRF: 返回WrongbookAddProcessor实例
    
    %% 3. 业务处理阶段
    Note over EP, USS: 3. 业务处理阶段
    DRF->>EP: process(event, collector)
    
    EP->>EP: 解析payload<br/>WrongbookAddPayload
    
    %% 维表查询
    EP->>DTS: queryWithCache("pattern_info", patternId)
    DTS->>DTS: 检查缓存
    alt 缓存未命中
        DTS->>MySQL: JDBC查询维表
        MySQL-->>DTS: 返回维表数据
        DTS->>DTS: 更新缓存(30分钟TTL)
    end
    DTS-->>EP: 返回丰富后的数据
    
    EP->>EP: 构建宽表数据<br/>enrichWithDimensions()
    
    %% 4. 输出决策阶段
    Note over EP, KO: 4. 输出决策阶段
    EP->>EP: 业务规则判断<br/>shouldOutputToMySQL()
    EP->>EP: 业务规则判断<br/>shouldOutputToKafka()
    
    %% MySQL输出
    alt 需要输出到MySQL
        EP->>USS: createMySQLSink("dwd_wrong_record_wide_delta")
        USS->>USS: 获取/创建连接池
        USS-->>EP: 返回MySQLSink实例
        EP->>MySQL: 批量写入宽表数据<br/>(1000条/批或5秒间隔)
        MySQL-->>EP: 写入确认
    end
    
    %% Kafka输出
    alt 需要输出到Kafka
        EP->>USS: createKafkaSink("wrongbook-wide-table-output")
        USS->>USS: 获取/创建生产者池
        USS-->>EP: 返回KafkaSink实例
        EP->>KO: 发送处理结果<br/>序列化后的ProcessedEvent
        KO-->>EP: 发送确认
    end
    
    %% 5. 监控收集阶段
    Note over EP, CWT: 5. 监控收集阶段
    EP->>DRF: collector.collect(ProcessedEvent)<br/>用于监控指标
    DRF->>CWT: 事件处理完成<br/>更新性能指标
    
    %% 异常处理
    Note over DRF, USS: 异常处理机制
    alt 处理失败
        EP->>EP: 记录错误日志
        EP->>USS: 重试机制(最多3次)
        alt 重试仍失败
            EP->>KO: 发送到死信队列<br/>"error-events"
        end
    end
```

## 🔍 时序图说明

### 📋 参与者说明
- **Kafka事件源**: 接收业务事件的数据源
- **CommonWideTableApp**: 统一的作业入口，负责路由转发
- **RoutingConfig**: 动态路由配置，支持热更新
- **DynamicRoutingProcessFunction**: 核心路由处理函数
- **EventProcessorFactory**: 处理器工厂，负责创建处理器实例
- **EventProcessor**: 业务处理器，负责具体业务逻辑和输出决策
- **DimTableQueryService**: 维表查询服务，提供缓存功能
- **UnifiedSinkService**: 统一输出服务，封装MySQL和Kafka输出
- **MySQL数据库**: 宽表数据存储
- **Kafka输出**: 下游数据传输

### 🚀 处理流程
1. **作业启动**: 创建执行环境和各种数据流
2. **事件接收**: 从Kafka接收业务事件
3. **动态路由**: 根据事件类型路由到对应处理器
4. **业务处理**: 执行具体业务逻辑，包括维表查询
5. **输出决策**: 根据业务规则决定输出目标
6. **数据输出**: 写入MySQL和/或Kafka
7. **监控收集**: 收集处理指标和性能数据
8. **异常处理**: 完善的错误处理和重试机制

### ✅ 关键特性
- **职责分离**: 入口类只负责路由，处理器负责业务和输出
- **统一输出**: UnifiedSinkService提供统一的输出服务
- **缓存优化**: 维表查询自动缓存，提升性能
- **灵活配置**: 支持动态路由配置热更新
- **错误处理**: 完整的重试机制和死信队列
