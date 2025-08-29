package com.flink.realtime.business;

import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.bean.ProcessedEvent;
import com.flink.realtime.config.RoutingConfig;
import com.flink.realtime.common.AliyunFlinkUtils;
import com.flink.realtime.function.BusinessEventDeserializationSchema;
import com.flink.realtime.function.DynamicRoutingProcessFunction;
import com.flink.realtime.function.OutputRoutingProcessFunction;
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

/**
 * 错题本实时宽表 - AI生成
 * 业务域: wrongbook
 * 生成时间: 2025-08-18 19:55:48
 * 
 * 支持的事件类型:
 * - wrongbook_add: 错题添加事件
 * - wrongbook_fix: 错题订正事件
 * - wrongbook_delete: 错题删除事件
 * 
 * @author AI代码生成器
 */
public class WrongbookWideTableApp {
    
    private static final Logger logger = LoggerFactory.getLogger(WrongbookWideTableApp.class);
    
    public static void main(String[] args) throws Exception {
        
        String domain = "wrongbook";
        logger.info("启动{}宽表作业，业务域: {}", "错题本实时宽表", domain);
        
        // 1. 创建执行环境
        StreamExecutionEnvironment env = AliyunFlinkUtils.getAliyunStreamExecutionEnvironment(
                ConfigUtils.getInt("flink.parallelism", 2));
        
        // 2. 创建事件源
        KafkaSource<BusinessEvent> eventSource = AliyunKafkaSourceBuilder.buildAliyunKafkaSource(
                ConfigUtils.getString("kafka.bootstrap.servers"),
                domain + "-events",
                domain + "-wide-table-group",
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
                new MapStateDescriptor<>("routing-config", 
                        TypeInformation.of(String.class), 
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
        
        SingleOutputStreamOperator<ProcessedEvent> routedStream = processedStream
                .process(new OutputRoutingProcessFunction(alertTag, metricsTag, null))
                .name("Output Routing")
                .uid("output-routing-" + domain);
        
        // 6. 输出到宽表
        routedStream.addSink(new AliyunMySQLSinkFunction<>(
                getInsertSQL(),
                getParameterSetter(),
                100
        )).name("wrongbook宽表输出");
        
        // 7. 执行作业
        env.execute("错题本实时宽表 Dynamic Job");
    }
    
    private static String getInsertSQL() {
        return "INSERT INTO dwd_wrong_record_wide_delta " +
               "(id, event_id, event_type, user_id, question_id, process_time) " +
               "VALUES (?, ?, ?, ?, ?, ?)";
    }
    
    private static AliyunMySQLSinkFunction.SqlParameterSetter<ProcessedEvent> getParameterSetter() {
        return (ps, event) -> {
            ps.setLong(1, event.getOriginalEvent().getEventId().hashCode());
            ps.setString(2, event.getOriginalEvent().getEventId());
            ps.setString(3, event.getOriginalEvent().getType());
            // 根据具体业务添加字段映射
            ps.setTimestamp(6, new java.sql.Timestamp(event.getProcessTime()));
        };
    }
}
