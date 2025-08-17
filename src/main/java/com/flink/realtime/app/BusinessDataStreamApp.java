package com.flink.realtime.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.function.BusinessEventDeserializationSchema;
import com.flink.realtime.processor.EventProcessor;
import com.flink.realtime.processor.EventProcessorFactory;
import com.flink.realtime.sink.MySQLSinkFunction;
import com.flink.realtime.source.MySQLDimSource;
import com.flink.realtime.util.ConfigUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Map;

/**
 * 业务事件DataStream API处理应用
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class BusinessDataStreamApp {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessDataStreamApp.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 维表状态描述符
    private static final MapStateDescriptor<String, Map<String, Object>> DIM_STATE_DESCRIPTOR =
            new MapStateDescriptor<>("dimState", String.class, TypeInformation.of(Map.class));
    
    public static void main(String[] args) throws Exception {
        // 创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(ConfigUtils.getInt("flink.parallelism", 1));
        env.enableCheckpointing(ConfigUtils.getLong("flink.checkpoint.interval", 5000L));
        
        logger.info("业务事件DataStream处理应用启动...");
        
        // 1. 创建Kafka数据源
        KafkaSource<BusinessEvent> kafkaSource = createKafkaSource();
        DataStreamSource<BusinessEvent> eventStream = env.fromSource(
                kafkaSource,
                WatermarkStrategy.<BusinessEvent>forMonotonousTimestamps()
                        .withTimestampAssigner((event, timestamp) -> event.getTimestamp()),
                "Business Event Source"
        );
        
        // 2. 创建维表数据源（广播流）
        DataStreamSource<Map<String, Object>> dimStream = env.addSource(
                new MySQLDimSource(
                        "SELECT * FROM user_dim",
                        "user_id",
                        60000L // 1分钟刷新一次
                )
        );
        BroadcastStream<Map<String, Object>> broadcastDimStream = dimStream.broadcast(DIM_STATE_DESCRIPTOR);
        
        // 3. 关联维表数据并处理业务逻辑
        SingleOutputStreamOperator<Map<String, Object>> processedStream = eventStream
                .connect(broadcastDimStream)
                .process(new BusinessEventProcessor());
        
        // 4. 写入结果表
        processedStream.addSink(new MySQLSinkFunction<>(
                "INSERT INTO business_wide_table (event_id, domain, type, user_id, user_name, data, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                (PreparedStatement ps, Map<String, Object> value) -> {
                    ps.setString(1, (String) value.get("event_id"));
                    ps.setString(2, (String) value.get("domain"));
                    ps.setString(3, (String) value.get("type"));
                    ps.setString(4, (String) value.get("user_id"));
                    ps.setString(5, (String) value.get("user_name"));
                    ps.setString(6, objectMapper.writeValueAsString(value.get("data")));
                    ps.setTimestamp(7, new Timestamp((Long) value.get("timestamp")));
                }
        ));
        
        // 执行任务
        env.execute("Business Event DataStream Processing Job");
    }
    
    /**
     * 创建Kafka数据源
     */
    private static KafkaSource<BusinessEvent> createKafkaSource() {
        return KafkaSource.<BusinessEvent>builder()
                .setBootstrapServers(ConfigUtils.getString("kafka.bootstrap.servers"))
                .setTopics(ConfigUtils.getString("kafka.input.topic"))
                .setGroupId(ConfigUtils.getString("kafka.group.id"))
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new BusinessEventDeserializationSchema())
                .build();
    }
    
    /**
     * 业务事件处理器
     */
    private static class BusinessEventProcessor extends BroadcastProcessFunction<BusinessEvent, Map<String, Object>, Map<String, Object>> {
        
        private MapState<String, Map<String, Object>> dimState;
        
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            dimState = getRuntimeContext().getMapState(DIM_STATE_DESCRIPTOR);
        }
        
        @Override
        public void processElement(BusinessEvent event, ReadOnlyContext ctx, Collector<Map<String, Object>> out) throws Exception {
            try {
                // 根据事件类型获取处理器
                EventProcessor processor = EventProcessorFactory.getProcessor(event.getType());
                if (processor == null) {
                    logger.warn("未找到事件类型 {} 的处理器，跳过处理", event.getType());
                    return;
                }
                
                // 处理事件
                Object processedData = processor.process(event);
                
                // 关联维表数据
                Map<String, Object> result = enrichWithDimData(event, processedData);
                
                out.collect(result);
                logger.debug("事件处理完成: {}", event.getEventId());
            } catch (Exception e) {
                logger.error("处理事件失败: {}", event, e);
            }
        }
        
        @Override
        public void processBroadcastElement(Map<String, Object> dimData, Context ctx, Collector<Map<String, Object>> out) throws Exception {
            // 更新维表状态
            String key = (String) dimData.get("_key");
            String table = (String) dimData.get("_table");
            
            if (key != null && table != null) {
                dimState.put(table + ":" + key, dimData);
                logger.debug("更新维表数据: {}:{}", table, key);
            }
        }
        
        /**
         * 关联维表数据
         */
        private Map<String, Object> enrichWithDimData(BusinessEvent event, Object processedData) throws Exception {
            Map<String, Object> result = new java.util.HashMap<>();
            
            // 基础事件信息
            result.put("event_id", event.getEventId());
            result.put("domain", event.getDomain());
            result.put("type", event.getType());
            result.put("timestamp", event.getTimestamp());
            result.put("data", processedData);
            
            // 关联用户维表（示例）
            JsonNode payload = event.getPayload();
            if (payload.has("userId")) {
                String userId = payload.get("userId").asText();
                Map<String, Object> userDim = dimState.get("user_dim:" + userId);
                if (userDim != null) {
                    result.put("user_id", userId);
                    result.put("user_name", userDim.get("user_name"));
                    result.put("user_level", userDim.get("user_level"));
                }
            }
            
            return result;
        }
    }
}
