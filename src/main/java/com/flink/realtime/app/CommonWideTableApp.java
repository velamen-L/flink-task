package com.flink.realtime.app;

import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.bean.ProcessedEvent;
import com.flink.realtime.common.AliyunFlinkUtils;
import com.flink.realtime.config.RoutingConfig;
import com.flink.realtime.function.DynamicRoutingProcessFunction;
import com.flink.realtime.function.OutputRoutingProcessFunction;
import com.flink.realtime.sink.AliyunKafkaProducer;
import com.flink.realtime.sink.AliyunMySQLSinkFunction;
import com.flink.realtime.source.AliyunKafkaSourceBuilder;
import com.flink.realtime.source.DynamicRoutingConfigSource;
import com.flink.realtime.util.ConfigUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用宽表作业入口类
 * 
 * 功能特性：
 * - 支持任意topic的动态路由处理
 * - 统一的事件处理流程
 * - 支持MySQL和Kafka双输出
 * - 完整的监控和异常处理
 * 
 * 使用方式：
 * 1. 直接调用: CommonWideTableApp.execute("wrongbook", mysqlSink, kafkaSink)
 * 2. 继承使用: 继承该类并实现具体的Sink逻辑
 * 
 * @author AI代码生成器
 * @date 2024-12-27
 */
public class CommonWideTableApp {
    
    private static final Logger logger = LoggerFactory.getLogger(CommonWideTableApp.class);
    
    /**
     * 执行通用宽表作业
     * 
     * @param topic 业务topic名称
     * @param mysqlSink MySQL输出Sink（可选）
     * @param kafkaSink Kafka输出Sink（可选）
     */
    public static void execute(String topic, 
                              AliyunMySQLSinkFunction<ProcessedEvent> mysqlSink,
                              AliyunKafkaProducer<ProcessedEvent> kafkaSink) throws Exception {
        
        logger.info("启动通用宽表作业，Topic: {}", topic);
        
        // 1. 创建执行环境
        StreamExecutionEnvironment env = AliyunFlinkUtils.getAliyunStreamExecutionEnvironment(
                ConfigUtils.getInt("flink.parallelism", 4));
        
        // 2. 创建事件源
        DataStream<BusinessEvent> eventStream = createEventSource(env, topic);
        
        // 3. 创建动态路由配置广播流
        BroadcastStream<RoutingConfig> configBroadcast = createConfigBroadcast(env, topic);
        
        // 4. 动态路由处理
        SingleOutputStreamOperator<ProcessedEvent> processedStream = createDynamicRouting(
                eventStream, configBroadcast, topic);
        
        // 5. 输出路由处理
        DataStream<ProcessedEvent> routedStream = createOutputRouting(processedStream, topic);
        
        // 6. 配置输出Sink
        configureSinks(routedStream, mysqlSink, kafkaSink, topic);
        
        // 7. 执行作业
        String jobName = String.format("%s Wide Table Job - Common", 
                topic.substring(0, 1).toUpperCase() + topic.substring(1));
        env.execute(jobName);
    }
    
    /**
     * 创建Kafka事件源
     */
    private static DataStream<BusinessEvent> createEventSource(StreamExecutionEnvironment env, String topic) {
        
        KafkaSource<BusinessEvent> eventSource = AliyunKafkaSourceBuilder.buildAliyunKafkaSource(
                ConfigUtils.getString("kafka.bootstrap.servers"),
                topic + "-events",
                topic + "-wide-table-group",
                ConfigUtils.getString("kafka.username", null),
                ConfigUtils.getString("kafka.password", null)
        );
        
        DataStreamSource<BusinessEvent> eventStream = env.fromSource(
                eventSource,
                WatermarkStrategy.<BusinessEvent>forMonotonousTimestamps()
                        .withTimestampAssigner((event, timestamp) -> event.getTimestamp()),
                topic + " Event Source"
        );
        
        logger.info("Kafka事件源创建完成，Topic: {}", topic);
        return eventStream;
    }
    
    /**
     * 创建动态路由配置广播流
     */
    private static BroadcastStream<RoutingConfig> createConfigBroadcast(StreamExecutionEnvironment env, String topic) {
        
        DataStreamSource<RoutingConfig> configStream = env.addSource(
                new DynamicRoutingConfigSource(topic), "Routing Config Source");
        
        MapStateDescriptor<String, RoutingConfig> configDescriptor = 
                new MapStateDescriptor<>("routing-config", 
                        TypeInformation.of(String.class), 
                        TypeInformation.of(RoutingConfig.class));
        
        BroadcastStream<RoutingConfig> configBroadcast = configStream.broadcast(configDescriptor);
        
        logger.info("动态路由配置广播流创建完成，Topic: {}", topic);
        return configBroadcast;
    }
    
    /**
     * 创建动态路由处理
     */
    private static SingleOutputStreamOperator<ProcessedEvent> createDynamicRouting(
            DataStream<BusinessEvent> eventStream,
            BroadcastStream<RoutingConfig> configBroadcast,
            String topic) {
        
        MapStateDescriptor<String, RoutingConfig> configDescriptor = 
                new MapStateDescriptor<>("routing-config", 
                        TypeInformation.of(String.class), 
                        TypeInformation.of(RoutingConfig.class));
        
        SingleOutputStreamOperator<ProcessedEvent> processedStream = eventStream
                .connect(configBroadcast)
                .process(new DynamicRoutingProcessFunction(configDescriptor))
                .name("Dynamic Event Processing")
                .uid("dynamic-event-processing-" + topic);
        
        logger.info("动态路由处理创建完成，Topic: {}", topic);
        return processedStream;
    }
    
    /**
     * 创建输出路由处理
     */
    private static DataStream<ProcessedEvent> createOutputRouting(
            SingleOutputStreamOperator<ProcessedEvent> processedStream, String topic) {
        
        // 定义侧流输出标签
        OutputTag<ProcessedEvent> alertTag = new OutputTag<ProcessedEvent>("alert", 
                TypeInformation.of(ProcessedEvent.class));
        OutputTag<ProcessedEvent> metricsTag = new OutputTag<ProcessedEvent>("metrics", 
                TypeInformation.of(ProcessedEvent.class));
        OutputTag<ProcessedEvent> auditTag = new OutputTag<ProcessedEvent>("audit", 
                TypeInformation.of(ProcessedEvent.class));
        
        SingleOutputStreamOperator<ProcessedEvent> routedStream = processedStream
                .process(new OutputRoutingProcessFunction(alertTag, metricsTag, auditTag))
                .name("Output Routing")
                .uid("output-routing-" + topic);
        
        // 处理侧流输出（告警、指标、审计）
        DataStream<ProcessedEvent> alertStream = routedStream.getSideOutput(alertTag);
        DataStream<ProcessedEvent> metricsStream = routedStream.getSideOutput(metricsTag);
        DataStream<ProcessedEvent> auditStream = routedStream.getSideOutput(auditTag);
        
        // 可以在这里添加侧流的处理逻辑
        handleSideStreams(alertStream, metricsStream, auditStream, topic);
        
        logger.info("输出路由处理创建完成，Topic: {}", topic);
        return routedStream;
    }
    
    /**
     * 配置输出Sink
     */
    private static void configureSinks(DataStream<ProcessedEvent> routedStream,
                                     AliyunMySQLSinkFunction<ProcessedEvent> mysqlSink,
                                     AliyunKafkaProducer<ProcessedEvent> kafkaSink,
                                     String topic) {
        
        // MySQL输出
        if (mysqlSink != null) {
            routedStream
                .filter(event -> shouldWriteToMySQL(event))
                .addSink(mysqlSink)
                .name(topic + " MySQL Sink");
            logger.info("MySQL Sink配置完成，Topic: {}", topic);
        }
        
        // Kafka输出
        if (kafkaSink != null) {
            routedStream
                .filter(event -> shouldWriteToKafka(event))
                .addSink(kafkaSink)
                .name(topic + " Kafka Sink");
            logger.info("Kafka Sink配置完成，Topic: {}", topic);
        }
        
        // 如果两个Sink都没有配置，添加一个Print Sink用于调试
        if (mysqlSink == null && kafkaSink == null) {
            routedStream.print().name(topic + " Debug Print");
            logger.warn("未配置任何Sink，使用Print Sink进行调试，Topic: {}", topic);
        }
    }
    
    /**
     * 处理侧流输出
     */
    private static void handleSideStreams(DataStream<ProcessedEvent> alertStream,
                                        DataStream<ProcessedEvent> metricsStream,
                                        DataStream<ProcessedEvent> auditStream,
                                        String topic) {
        
        // 告警流处理
        alertStream.addSink(new AliyunKafkaProducer<>(
                ConfigUtils.getString("kafka.bootstrap.servers"),
                "alert-events",
                event -> event.getOriginalEvent().getEventId(),
                event -> event.toString()
        )).name(topic + " Alert Sink");
        
        // 指标流处理
        metricsStream.addSink(new AliyunKafkaProducer<>(
                ConfigUtils.getString("kafka.bootstrap.servers"),
                "metrics-events", 
                event -> event.getOriginalEvent().getEventId(),
                event -> event.toString()
        )).name(topic + " Metrics Sink");
        
        // 审计流处理
        auditStream.addSink(new AliyunKafkaProducer<>(
                ConfigUtils.getString("kafka.bootstrap.servers"),
                "audit-events",
                event -> event.getOriginalEvent().getEventId(),
                event -> event.toString()
        )).name(topic + " Audit Sink");
        
        logger.info("侧流处理配置完成，Topic: {}", topic);
    }
    
    /**
     * 判断是否应该写入MySQL
     */
    private static boolean shouldWriteToMySQL(ProcessedEvent event) {
        return event.getOutputTargets() == null || 
               event.getOutputTargets().isEmpty() ||
               event.getOutputTargets().contains("mysql") ||
               event.getOutputTargets().contains("wide_table");
    }
    
    /**
     * 判断是否应该写入Kafka
     */
    private static boolean shouldWriteToKafka(ProcessedEvent event) {
        return event.getOutputTargets() != null && 
               (event.getOutputTargets().contains("kafka") ||
                event.getOutputTargets().contains("downstream_topic"));
    }
}
