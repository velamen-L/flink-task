package com.flink.realtime.source;

import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.function.BusinessEventDeserializationSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Properties;

/**
 * 阿里云Kafka数据源构建器
 * 符合阿里云Kafka实例的配置规范
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class AliyunKafkaSourceBuilder {
    
    /**
     * 创建符合阿里云规范的Kafka数据源
     * 
     * @param bootstrapServers Kafka集群地址
     * @param topic 主题名称
     * @param groupId 消费者组ID
     * @param username SASL用户名（阿里云Kafka需要）
     * @param password SASL密码（阿里云Kafka需要）
     * @return KafkaSource
     */
    public static KafkaSource<BusinessEvent> buildAliyunKafkaSource(
            String bootstrapServers,
            String topic,
            String groupId,
            String username,
            String password) {
        
        // 阿里云Kafka特有的配置
        Properties kafkaProps = new Properties();
        
        // 基础配置
        kafkaProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        kafkaProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // 阿里云Kafka SASL认证配置
        if (username != null && password != null) {
            kafkaProps.setProperty("security.protocol", "SASL_PLAINTEXT");
            kafkaProps.setProperty("sasl.mechanism", "PLAIN");
            kafkaProps.setProperty("sasl.jaas.config", 
                String.format("org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"%s\" password=\"%s\";", username, password));
        }
        
        // 阿里云推荐的消费者配置
        kafkaProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        kafkaProps.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        kafkaProps.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        kafkaProps.setProperty(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "3000");
        kafkaProps.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500");
        kafkaProps.setProperty(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, "500");
        
        // 阿里云网络优化配置
        kafkaProps.setProperty(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, "300000");
        kafkaProps.setProperty(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "60000");
        
        return KafkaSource.<BusinessEvent>builder()
                .setBootstrapServers(bootstrapServers)
                .setTopics(topic)
                .setGroupId(groupId)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new BusinessEventDeserializationSchema())
                .setProperties(kafkaProps)
                .build();
    }
    
    /**
     * 创建简化版本的阿里云Kafka数据源（使用环境变量配置）
     */
    public static KafkaSource<BusinessEvent> buildSimpleAliyunKafkaSource(
            String topic, String groupId) {
        
        String bootstrapServers = System.getProperty("kafka.bootstrap.servers");
        String username = System.getProperty("kafka.username");
        String password = System.getProperty("kafka.password");
        
        return buildAliyunKafkaSource(bootstrapServers, topic, groupId, username, password);
    }
    
    /**
     * 创建用于阿里云VPC环境的Kafka数据源
     */
    public static KafkaSource<BusinessEvent> buildVPCKafkaSource(
            String internalBootstrapServers,
            String topic,
            String groupId) {
        
        Properties kafkaProps = new Properties();
        
        // VPC内网访问配置
        kafkaProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, internalBootstrapServers);
        kafkaProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // VPC环境推荐的网络配置
        kafkaProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        kafkaProps.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        kafkaProps.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        kafkaProps.setProperty(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "3000");
        
        // VPC网络优化
        kafkaProps.setProperty(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, "180000");
        kafkaProps.setProperty(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");
        
        return KafkaSource.<BusinessEvent>builder()
                .setBootstrapServers(internalBootstrapServers)
                .setTopics(topic)
                .setGroupId(groupId)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new BusinessEventDeserializationSchema())
                .setProperties(kafkaProps)
                .build();
    }
}
