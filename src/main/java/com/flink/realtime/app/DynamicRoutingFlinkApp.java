package com.flink.realtime.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.bean.ProcessedEvent;
import com.flink.realtime.config.RoutingConfig;
import com.flink.realtime.common.AliyunFlinkUtils;
import com.flink.realtime.function.BusinessEventDeserializationSchema;
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
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;

/**
 * 动态路由Flink应用
 * 支持热配置更新的混合架构实现
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class DynamicRoutingFlinkApp {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicRoutingFlinkApp.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void main(String[] args) throws Exception {
        
        if (args.length < 1) {
            logger.error("请提供业务域参数，例如：java -jar app.jar wrongbook");
            System.exit(1);
        }
        
        String domain = args[0];
        logger.info("启动动态路由Flink作业，业务域: {}", domain);
        
        // 1. 创建阿里云优化的执行环境
        StreamExecutionEnvironment env = AliyunFlinkUtils.getAliyunStreamExecutionEnvironment(
                ConfigUtils.getInt("flink.parallelism", 4));
        
        // 2. 创建事件源
        KafkaSource<BusinessEvent> eventSource = AliyunKafkaSourceBuilder.buildAliyunKafkaSource(
                ConfigUtils.getString("kafka.bootstrap.servers"),
                domain + "-events",
                domain + "-processor-group",
                ConfigUtils.getString("kafka.username", null),
                ConfigUtils.getString("kafka.password", null)
        );
        
        DataStreamSource<BusinessEvent> eventStream = env.fromSource(
                eventSource,
                WatermarkStrategy.<BusinessEvent>forMonotonousTimestamps()
                        .withTimestampAssigner((event, timestamp) -> event.getTimestamp()),
                domain + " Event Source"
        );
        
        // 3. 创建动态路由配置源
        DataStreamSource<RoutingConfig> configStream = env.addSource(
                new DynamicRoutingConfigSource(domain), "Routing Config Source");
        
        MapStateDescriptor<String, RoutingConfig> configDescriptor = 
                new MapStateDescriptor<>("routing-config", String.class, 
                        TypeInformation.of(RoutingConfig.class));
        BroadcastStream<RoutingConfig> configBroadcast = configStream.broadcast(configDescriptor);
        
        // 4. 动态路由处理
        SingleOutputStreamOperator<ProcessedEvent> processedStream = eventStream
                .connect(configBroadcast)
                .process(new DynamicRoutingProcessFunction(configDescriptor))
                .name("Dynamic Event Processing")
                .uid("dynamic-event-processing-" + domain);
        
        // 5. 输出路由
        OutputTag<ProcessedEvent> alertTag = new OutputTag<ProcessedEvent>("alert", 
                TypeInformation.of(ProcessedEvent.class));
        OutputTag<ProcessedEvent> metricsTag = new OutputTag<ProcessedEvent>("metrics", 
                TypeInformation.of(ProcessedEvent.class));
        OutputTag<ProcessedEvent> auditTag = new OutputTag<ProcessedEvent>("audit", 
                TypeInformation.of(ProcessedEvent.class));
        
        SingleOutputStreamOperator<ProcessedEvent> routedStream = processedStream
                .process(new OutputRoutingProcessFunction(alertTag, metricsTag, auditTag))
                .name("Output Routing")
                .uid("output-routing-" + domain);
        
        // 6. 输出到不同目标
        
        // 主流：写入宽表
        routedStream.addSink(new AliyunMySQLSinkFunction<>(
                getWideTableInsertSQL(domain),
                getWideTableParameterSetter(domain),
                100 // 批大小
        )).name(domain + " Wide Table Sink");
        
        // 侧流：告警数据
        routedStream.getSideOutput(alertTag)
                .addSink(new AliyunKafkaProducer<>("alert-topic"))
                .name("Alert Sink");
        
        // 侧流：指标数据
        routedStream.getSideOutput(metricsTag)
                .addSink(new AliyunKafkaProducer<>("metrics-topic"))
                .name("Metrics Sink");
        
        // 侧流：审计数据
        routedStream.getSideOutput(auditTag)
                .addSink(new AliyunKafkaProducer<>("audit-topic"))
                .name("Audit Sink");
        
        // 7. 执行作业
        env.execute(domain + " Dynamic Routing Job");
    }
    
    /**
     * 获取宽表插入SQL
     */
    private static String getWideTableInsertSQL(String domain) {
        return String.format(
                "INSERT INTO %s_wide_table (event_id, domain, type, user_id, business_data, " +
                "process_time, data_source, processor_class) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", domain);
    }
    
    /**
     * 获取宽表参数设置器
     */
    private static AliyunMySQLSinkFunction.SqlParameterSetter<ProcessedEvent> getWideTableParameterSetter(String domain) {
        return (ps, event) -> {
            ps.setString(1, event.getOriginalEvent().getEventId());
            ps.setString(2, event.getOriginalEvent().getDomain());
            ps.setString(3, event.getOriginalEvent().getType());
            // 从payload中提取用户ID
            ps.setString(4, extractUserId(event.getOriginalEvent()));
            ps.setString(5, objectMapper.writeValueAsString(event.getProcessedData()));
            ps.setTimestamp(6, new Timestamp(event.getProcessTime()));
            ps.setString(7, "realtime");
            ps.setString(8, event.getProcessorClass());
        };
    }
    
    /**
     * 从事件中提取用户ID
     */
    private static String extractUserId(BusinessEvent event) {
        try {
            JsonNode payload = event.getPayload();
            if (payload.has("userId")) {
                return payload.get("userId").asText();
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return "unknown";
    }
}
