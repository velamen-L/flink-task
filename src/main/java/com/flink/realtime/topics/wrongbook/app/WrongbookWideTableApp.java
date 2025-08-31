package com.flink.realtime.topics.wrongbook.app;

import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.bean.ProcessedEvent;
import com.flink.realtime.common.AliyunFlinkUtils;
import com.flink.realtime.common.FlinkUtils;
import com.flink.realtime.config.RoutingConfig;
import com.flink.realtime.config.RoutingConfigManager;
import com.flink.realtime.function.DynamicRoutingProcessFunction;
import com.flink.realtime.function.OutputRoutingProcessFunction;
import com.flink.realtime.metrics.AliyunMetricsReporter;
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

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Map;

/**
 * 错题本系统实时宽表作业 - DataStream API版本
 * 
 * 功能特性：
 * - 动态路由：支持事件类型动态路由
 * - 热部署：支持配置热更新
 * - 故障隔离：支持故障隔离机制
 * - 监控告警：完整的监控和告警体系
 * 
 * 支持的事件类型:
 * - wrongbook_add: 错题添加事件
 * - wrongbook_fix: 错题订正事件
 * - wrongbook_delete: 错题删除事件
 * 
 * @author AI代码生成器
 * @date 2025-08-29
 */
public class WrongbookWideTableApp {
    
    private static final Logger logger = LoggerFactory.getLogger(WrongbookWideTableApp.class);
    
    public static void main(String[] args) throws Exception {
        
        String domain = "wrongbook";
        logger.info("启动错题本宽表作业，业务域: {}", domain);
        
        // 1. 创建执行环境
        StreamExecutionEnvironment env = AliyunFlinkUtils.getAliyunStreamExecutionEnvironment(
                ConfigUtils.getInt("flink.parallelism", 4));
        
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
                getSqlParameterSetter(),
                1000
        )).name(domain + "宽表输出");
        
        // 7. 执行作业
        env.execute("Wrongbook Wide Table Job - DataStream API");
    }
    
    /**
     * 获取插入SQL语句
     */
    private static String getInsertSQL() {
        return "INSERT INTO dwd_wrong_record_wide_delta (" +
                "id, wrong_id, user_id, subject, subject_name, question_id, question, " +
                "pattern_id, pattern_name, teach_type_id, teach_type_name, " +
                "collect_time, fix_id, fix_time, fix_result, fix_result_desc, " +
                "event_id, event_type, process_time" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "subject = VALUES(subject), subject_name = VALUES(subject_name), " +
                "question = VALUES(question), pattern_name = VALUES(pattern_name), " +
                "teach_type_id = VALUES(teach_type_id), teach_type_name = VALUES(teach_type_name), " +
                "fix_id = VALUES(fix_id), fix_time = VALUES(fix_time), " +
                "fix_result = VALUES(fix_result), fix_result_desc = VALUES(fix_result_desc), " +
                "event_id = VALUES(event_id), event_type = VALUES(event_type), " +
                "process_time = VALUES(process_time)";
    }
    
    /**
     * 获取参数设置器
     */
    private static AliyunMySQLSinkFunction.SqlParameterSetter<ProcessedEvent> getSqlParameterSetter() {
        return (PreparedStatement ps, ProcessedEvent event) -> {
            Map<String, Object> data = (Map<String, Object>) event.getProcessedData();
            
            int index = 1;
            ps.setLong(index++, (Long) data.get("id"));
            ps.setString(index++, (String) data.get("wrong_id"));
            ps.setString(index++, (String) data.get("user_id"));
            ps.setString(index++, (String) data.get("subject"));
            ps.setString(index++, (String) data.get("subject_name"));
            ps.setString(index++, (String) data.get("question_id"));
            ps.setString(index++, (String) data.get("question"));
            ps.setString(index++, (String) data.get("pattern_id"));
            ps.setString(index++, (String) data.get("pattern_name"));
            ps.setString(index++, (String) data.get("teach_type_id"));
            ps.setString(index++, (String) data.get("teach_type_name"));
            
            // 时间字段处理
            Object collectTime = data.get("collect_time");
            if (collectTime instanceof Timestamp) {
                ps.setTimestamp(index++, (Timestamp) collectTime);
            } else {
                ps.setTimestamp(index++, null);
            }
            
            ps.setString(index++, (String) data.get("fix_id"));
            
            Object fixTime = data.get("fix_time");
            if (fixTime instanceof Timestamp) {
                ps.setTimestamp(index++, (Timestamp) fixTime);
            } else {
                ps.setTimestamp(index++, null);
            }
            
            Object fixResult = data.get("fix_result");
            if (fixResult instanceof Number) {
                ps.setLong(index++, ((Number) fixResult).longValue());
            } else {
                ps.setLong(index++, 0L);
            }
            
            ps.setString(index++, (String) data.get("fix_result_desc"));
            ps.setString(index++, (String) data.get("event_id"));
            ps.setString(index++, (String) data.get("event_type"));
            
            Object processTime = data.get("process_time");
            if (processTime instanceof Timestamp) {
                ps.setTimestamp(index++, (Timestamp) processTime);
            } else {
                ps.setTimestamp(index++, new Timestamp(System.currentTimeMillis()));
            }
        };
    }
}
