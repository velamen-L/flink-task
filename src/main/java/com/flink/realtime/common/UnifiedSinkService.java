package com.flink.realtime.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flink.realtime.bean.ProcessedEvent;
import com.flink.realtime.util.ConfigUtils;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一Sink服务
 * 提供MySQL和Kafka输出的统一封装
 * 
 * 功能特性：
 * - 统一的输出接口
 * - 自动配置管理
 * - 连接池和性能优化
 * - 错误处理和重试机制
 * 
 * 使用方式：
 * ```java
 * UnifiedSinkService sinkService = UnifiedSinkService.getInstance();
 * 
 * // MySQL输出
 * sinkService.writeToMySQL(processedEvent, "table_name", sqlBuilder);
 * 
 * // Kafka输出
 * sinkService.writeToKafka(processedEvent, "topic_name");
 * ```
 * 
 * @author AI代码生成器
 * @date 2024-12-27
 */
public class UnifiedSinkService {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedSinkService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 单例实例
    private static volatile UnifiedSinkService instance;
    
    // 缓存的Sink实例
    private final Map<String, SinkFunction<ProcessedEvent>> mysqlSinkCache = new ConcurrentHashMap<>();
    private final Map<String, KafkaSink<ProcessedEvent>> kafkaSinkCache = new ConcurrentHashMap<>();
    
    private UnifiedSinkService() {
        logger.info("统一Sink服务初始化完成");
    }
    
    /**
     * 获取单例实例
     */
    public static UnifiedSinkService getInstance() {
        if (instance == null) {
            synchronized (UnifiedSinkService.class) {
                if (instance == null) {
                    instance = new UnifiedSinkService();
                }
            }
        }
        return instance;
    }
    
    /**
     * 创建MySQL Sink
     * 
     * @param tableName 表名
     * @param sqlBuilder SQL构建器
     * @return MySQL Sink
     */
    public SinkFunction<ProcessedEvent> createMySQLSink(String tableName, 
                                                       MySQLSqlBuilder sqlBuilder) {
        
        String cacheKey = tableName;
        return mysqlSinkCache.computeIfAbsent(cacheKey, key -> {
            
            JdbcConnectionOptions connectionOptions = new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withUrl(ConfigUtils.getString("mysql.url"))
                .withDriverName("com.mysql.cj.jdbc.Driver")
                .withUsername(ConfigUtils.getString("mysql.username"))
                .withPassword(ConfigUtils.getString("mysql.password"))
                .build();
            
            JdbcExecutionOptions executionOptions = JdbcExecutionOptions.builder()
                .withBatchSize(ConfigUtils.getInt("mysql.batch.size", 1000))
                .withBatchIntervalMs(ConfigUtils.getLong("mysql.batch.interval", 5000L))
                .withMaxRetries(ConfigUtils.getInt("mysql.max.retries", 3))
                .build();
            
            return JdbcSink.sink(
                sqlBuilder.buildInsertSQL(tableName),
                (JdbcStatementBuilder<ProcessedEvent>) (ps, event) -> {
                    try {
                        sqlBuilder.setParameters(ps, event);
                    } catch (Exception e) {
                        logger.error("设置SQL参数失败: table={}, event={}", tableName, event, e);
                        throw new SQLException("SQL参数设置失败", e);
                    }
                },
                executionOptions,
                connectionOptions
            );
        });
    }
    
    /**
     * 创建Kafka Sink
     * 
     * @param topicName Topic名称
     * @return Kafka Sink
     */
    public KafkaSink<ProcessedEvent> createKafkaSink(String topicName) {
        
        return kafkaSinkCache.computeIfAbsent(topicName, key -> {
            
            Properties props = new Properties();
            props.setProperty("bootstrap.servers", ConfigUtils.getString("kafka.bootstrap.servers"));
            props.setProperty("transaction.timeout.ms", "900000");
            
            // 如果有认证信息
            String username = ConfigUtils.getString("kafka.username", null);
            String password = ConfigUtils.getString("kafka.password", null);
            if (username != null && password != null) {
                props.setProperty("security.protocol", "SASL_PLAINTEXT");
                props.setProperty("sasl.mechanism", "PLAIN");
                props.setProperty("sasl.jaas.config", 
                    String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";", 
                                  username, password));
            }
            
            return KafkaSink.<ProcessedEvent>builder()
                .setBootstrapServers(ConfigUtils.getString("kafka.bootstrap.servers"))
                .setRecordSerializer(new ProcessedEventKafkaSerializer(topicName))
                .setKafkaProducerConfig(props)
                .build();
        });
    }
    
    /**
     * MySQL SQL构建器接口
     */
    public interface MySQLSqlBuilder {
        
        /**
         * 构建插入SQL
         * @param tableName 表名
         * @return SQL语句
         */
        String buildInsertSQL(String tableName);
        
        /**
         * 设置SQL参数
         * @param ps PreparedStatement
         * @param event 事件数据
         * @throws SQLException SQL异常
         */
        void setParameters(PreparedStatement ps, ProcessedEvent event) throws SQLException;
    }
    

    
    /**
     * Kafka序列化器
     */
    private static class ProcessedEventKafkaSerializer implements KafkaRecordSerializationSchema<ProcessedEvent> {
        
        private final String topicName;
        
        public ProcessedEventKafkaSerializer(String topicName) {
            this.topicName = topicName;
        }
        
        @Override
        public ProducerRecord<byte[], byte[]> serialize(ProcessedEvent event, 
                                                       KafkaSinkContext context, 
                                                       Long timestamp) {
            try {
                String key = event.getOriginalEvent().getEventId();
                String value = serializeEvent(event);
                
                return new ProducerRecord<>(
                    topicName,
                    key.getBytes(StandardCharsets.UTF_8),
                    value.getBytes(StandardCharsets.UTF_8)
                );
                
            } catch (Exception e) {
                logger.error("序列化ProcessedEvent失败: {}", event, e);
                // 返回错误记录
                String errorKey = event.getOriginalEvent().getEventId();
                String errorValue = "{\"error\":\"serialization_failed\",\"event_id\":\"" + 
                                  event.getOriginalEvent().getEventId() + "\"}";
                return new ProducerRecord<>(
                    topicName,
                    errorKey.getBytes(StandardCharsets.UTF_8),
                    errorValue.getBytes(StandardCharsets.UTF_8)
                );
            }
        }
        
        private String serializeEvent(ProcessedEvent event) throws Exception {
            // 直接序列化处理后的数据，而不是整个事件对象
            // 这样Kafka中的数据就是纯粹的业务数据
            @SuppressWarnings("unchecked")
            Map<String, Object> processedData = (Map<String, Object>) event.getProcessedData();
            
            // 添加一些元数据用于追踪
            Map<String, Object> output = new java.util.HashMap<>(processedData);
            output.put("_event_id", event.getOriginalEvent().getEventId());
            output.put("_event_type", event.getOriginalEvent().getType());
            output.put("_domain", event.getOriginalEvent().getDomain());
            output.put("_process_time", event.getProcessTime());
            output.put("_processor_class", event.getProcessorClass());
            
            return objectMapper.writeValueAsString(output);
        }
    }
}
