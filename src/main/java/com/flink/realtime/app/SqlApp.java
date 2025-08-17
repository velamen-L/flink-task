package com.flink.realtime.app;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flink SQL 实时数据处理应用
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class SqlApp {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlApp.class);
    
    public static void main(String[] args) throws Exception {
        // 创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
        
        // 设置并行度
        env.setParallelism(1);
        
        logger.info("Flink SQL实时数据处理应用启动...");
        
        // 创建Kafka源表
        String createKafkaSourceSql = 
            "CREATE TABLE kafka_source (" +
            "  id STRING," +
            "  name STRING," +
            "  age INT," +
            "  create_time TIMESTAMP(3)," +
            "  WATERMARK FOR create_time AS create_time - INTERVAL '5' SECOND" +
            ") WITH (" +
            "  'connector' = 'kafka'," +
            "  'topic' = 'input-topic'," +
            "  'properties.bootstrap.servers' = 'localhost:9092'," +
            "  'properties.group.id' = 'flink-sql-group'," +
            "  'scan.startup.mode' = 'latest-offset'," +
            "  'format' = 'json'" +
            ")";
        
        // 创建输出表（控制台打印）
        String createPrintSinkSql = 
            "CREATE TABLE print_sink (" +
            "  id STRING," +
            "  name STRING," +
            "  age INT," +
            "  create_time TIMESTAMP(3)" +
            ") WITH (" +
            "  'connector' = 'print'" +
            ")";
        
        // 执行DDL
        tableEnv.executeSql(createKafkaSourceSql);
        tableEnv.executeSql(createPrintSinkSql);
        
        // 执行查询并插入结果
        String insertSql = 
            "INSERT INTO print_sink " +
            "SELECT id, name, age, create_time " +
            "FROM kafka_source " +
            "WHERE age > 18";
        
        tableEnv.executeSql(insertSql);
        
        logger.info("Flink SQL任务执行完成");
    }
}
