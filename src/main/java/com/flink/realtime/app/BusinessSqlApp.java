package com.flink.realtime.app;

import com.flink.realtime.util.ConfigUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 业务事件Flink SQL处理应用
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class BusinessSqlApp {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessSqlApp.class);
    
    public static void main(String[] args) throws Exception {
        // 创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
        
        env.setParallelism(ConfigUtils.getInt("flink.parallelism", 1));
        env.enableCheckpointing(ConfigUtils.getLong("flink.checkpoint.interval", 5000L));
        
        logger.info("业务事件Flink SQL处理应用启动...");
        
        // 1. 创建Kafka源表
        createKafkaSourceTable(tableEnv);
        
        // 2. 创建MySQL维表
        createMySQLDimTable(tableEnv);
        
        // 3. 创建MySQL结果表
        createMySQLSinkTable(tableEnv);
        
        // 4. 执行业务SQL
        executeBusinessSQL(tableEnv);
        
        logger.info("Flink SQL任务提交完成");
    }
    
    /**
     * 创建Kafka源表
     */
    private static void createKafkaSourceTable(StreamTableEnvironment tableEnv) {
        String createKafkaSourceSql = String.format(
            "CREATE TABLE business_event_source (" +
            "  domain STRING," +
            "  type STRING," +
            "  timestamp BIGINT," +
            "  eventId STRING," +
            "  payload STRING," +
            "  version STRING," +
            "  source STRING," +
            "  proc_time AS PROCTIME()," +
            "  event_time AS TO_TIMESTAMP_LTZ(timestamp, 3)," +
            "  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND" +
            ") WITH (" +
            "  'connector' = 'kafka'," +
            "  'topic' = '%s'," +
            "  'properties.bootstrap.servers' = '%s'," +
            "  'properties.group.id' = '%s'," +
            "  'scan.startup.mode' = 'latest-offset'," +
            "  'format' = 'json'" +
            ")",
            ConfigUtils.getString("kafka.input.topic"),
            ConfigUtils.getString("kafka.bootstrap.servers"),
            ConfigUtils.getString("kafka.group.id")
        );
        
        tableEnv.executeSql(createKafkaSourceSql);
        logger.info("Kafka源表创建完成");
    }
    
    /**
     * 创建MySQL维表
     */
    private static void createMySQLDimTable(StreamTableEnvironment tableEnv) {
        String createUserDimSql = String.format(
            "CREATE TABLE user_dim (" +
            "  user_id STRING," +
            "  user_name STRING," +
            "  user_level INT," +
            "  create_time TIMESTAMP(3)," +
            "  PRIMARY KEY (user_id) NOT ENFORCED" +
            ") WITH (" +
            "  'connector' = 'jdbc'," +
            "  'url' = '%s'," +
            "  'username' = '%s'," +
            "  'password' = '%s'," +
            "  'table-name' = 'user_dim'," +
            "  'lookup.cache.max-rows' = '1000'," +
            "  'lookup.cache.ttl' = '60s'" +
            ")",
            ConfigUtils.getString("mysql.url"),
            ConfigUtils.getString("mysql.username"),
            ConfigUtils.getString("mysql.password")
        );
        
        tableEnv.executeSql(createUserDimSql);
        logger.info("MySQL维表创建完成");
    }
    
    /**
     * 创建MySQL结果表
     */
    private static void createMySQLSinkTable(StreamTableEnvironment tableEnv) {
        String createSinkSql = String.format(
            "CREATE TABLE business_wide_table (" +
            "  event_id STRING," +
            "  domain STRING," +
            "  type STRING," +
            "  user_id STRING," +
            "  user_name STRING," +
            "  user_level INT," +
            "  payload_data STRING," +
            "  event_time TIMESTAMP(3)," +
            "  process_time TIMESTAMP(3)," +
            "  PRIMARY KEY (event_id) NOT ENFORCED" +
            ") WITH (" +
            "  'connector' = 'jdbc'," +
            "  'url' = '%s'," +
            "  'username' = '%s'," +
            "  'password' = '%s'," +
            "  'table-name' = 'business_wide_table'" +
            ")",
            ConfigUtils.getString("mysql.url"),
            ConfigUtils.getString("mysql.username"),
            ConfigUtils.getString("mysql.password")
        );
        
        tableEnv.executeSql(createSinkSql);
        logger.info("MySQL结果表创建完成");
    }
    
    /**
     * 执行业务SQL逻辑
     */
    private static void executeBusinessSQL(StreamTableEnvironment tableEnv) {
        // 示例：用户行为事件关联用户维表
        String businessSql = 
            "INSERT INTO business_wide_table " +
            "SELECT " +
            "  s.eventId as event_id," +
            "  s.domain," +
            "  s.type," +
            "  JSON_VALUE(s.payload, '$.userId') as user_id," +
            "  COALESCE(d.user_name, 'Unknown') as user_name," +
            "  COALESCE(d.user_level, 0) as user_level," +
            "  s.payload as payload_data," +
            "  s.event_time," +
            "  s.proc_time as process_time " +
            "FROM business_event_source s " +
            "LEFT JOIN user_dim FOR SYSTEM_TIME AS OF s.proc_time AS d " +
            "  ON JSON_VALUE(s.payload, '$.userId') = d.user_id " +
            "WHERE s.type IN ('user_login', 'user_purchase', 'user_view')";
        
        tableEnv.executeSql(businessSql);
        logger.info("业务SQL执行完成");
    }
}
