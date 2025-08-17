package com.flink.realtime.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flink.realtime.util.ConfigUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * 阿里云Kafka生产者
 * 用于向Kafka Topic发送数据
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class AliyunKafkaProducer<T> extends RichSinkFunction<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(AliyunKafkaProducer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String topicName;
    private transient KafkaProducer<String, String> producer;
    
    public AliyunKafkaProducer(String topicName) {
        this.topicName = topicName;
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        
        Properties props = new Properties();
        
        // 基础配置
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                ConfigUtils.getString("kafka.bootstrap.servers"));
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                StringSerializer.class.getName());
        
        // 阿里云Kafka SASL认证配置
        String username = ConfigUtils.getString("kafka.username", null);
        String password = ConfigUtils.getString("kafka.password", null);
        if (username != null && password != null) {
            props.setProperty("security.protocol", "SASL_PLAINTEXT");
            props.setProperty("sasl.mechanism", "PLAIN");
            props.setProperty("sasl.jaas.config", 
                String.format("org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"%s\" password=\"%s\";", username, password));
        }
        
        // 性能优化配置
        props.setProperty(ProducerConfig.ACKS_CONFIG, "1");
        props.setProperty(ProducerConfig.RETRIES_CONFIG, "3");
        props.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, "16384");
        props.setProperty(ProducerConfig.LINGER_MS_CONFIG, "1");
        props.setProperty(ProducerConfig.BUFFER_MEMORY_CONFIG, "33554432");
        
        // 创建生产者
        producer = new KafkaProducer<>(props);
        
        logger.info("Kafka生产者已初始化，Topic: {}", topicName);
    }
    
    @Override
    public void invoke(T value, Context context) throws Exception {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            ProducerRecord<String, String> record = new ProducerRecord<>(topicName, jsonValue);
            
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("发送消息到Kafka失败，Topic: {}", topicName, exception);
                } else {
                    logger.debug("消息发送成功，Topic: {}, Partition: {}, Offset: {}", 
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
            
        } catch (Exception e) {
            logger.error("序列化消息失败，Topic: {}", topicName, e);
            throw e;
        }
    }
    
    @Override
    public void close() throws Exception {
        if (producer != null) {
            producer.flush();
            producer.close();
        }
        logger.info("Kafka生产者已关闭，Topic: {}", topicName);
        super.close();
    }
}
