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
 * - 纯路由转发，Sink逻辑由Processor决定
 * - 完整的监控和异常处理
 * 
 * 使用方式：
 * ```java
 * CommonWideTableApp.execute("wrongbook");
 * ```
 * 
 * 职责分工：
 * - CommonWideTableApp: 负责路由发现和type转发
 * - Processor: 负责业务逻辑和输出决策
 * - UnifiedSinkService: 负责统一的输出服务
 * 
 * @author AI代码生成器
 * @date 2024-12-27
 */
public class CommonWideTableApp {
    
    private static final Logger logger = LoggerFactory.getLogger(CommonWideTableApp.class);
    
    /**
     * 执行通用宽表作业（简化版本）
     * 
     * @param topic 业务topic名称
     */
    public static void execute(String topic) throws Exception {
        
        logger.info("启动通用宽表作业，Topic: {}", topic);
        
        // 1. 创建执行环境
        StreamExecutionEnvironment env = AliyunFlinkUtils.getAliyunStreamExecutionEnvironment(
                ConfigUtils.getInt("flink.parallelism", 4));
        
        // 2. 创建事件源
        DataStream<BusinessEvent> eventStream = createEventSource(env, topic);
        
        // 3. 创建动态路由配置广播流
        BroadcastStream<RoutingConfig> configBroadcast = createConfigBroadcast(env, topic);
        
        // 4. 动态路由处理（Processor内部处理输出）
        SingleOutputStreamOperator<ProcessedEvent> processedStream = createDynamicRouting(
                eventStream, configBroadcast, topic);
        
        // 5. 注意：不再需要统一的Sink配置，每个Processor自己决定输出
        // Processor内部会调用UnifiedSinkService进行输出
        
        // 6. 执行作业
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
    

}
