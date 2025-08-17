package com.flink.realtime.app;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flink DataStream API 实时数据流处理应用
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class DataStreamApp {
    
    private static final Logger logger = LoggerFactory.getLogger(DataStreamApp.class);
    
    public static void main(String[] args) throws Exception {
        // 创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // 设置并行度
        env.setParallelism(1);
        
        // 开启checkpoint
        env.enableCheckpointing(5000);
        
        logger.info("Flink实时数据流处理应用启动...");
        
        // 创建Kafka数据源（示例）
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers("localhost:9092")
                .setTopics("input-topic")
                .setGroupId("flink-consumer-group")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
        
        // 读取数据流
        DataStreamSource<String> dataStream = env.fromSource(
                kafkaSource, 
                WatermarkStrategy.noWatermarks(), 
                "Kafka Source"
        );
        
        // 数据处理逻辑
        dataStream
                .map(value -> {
                    logger.info("接收到数据: {}", value);
                    return value.toUpperCase();
                })
                .print("处理结果");
        
        // 执行任务
        env.execute("Flink实时数据流处理任务");
    }
}
