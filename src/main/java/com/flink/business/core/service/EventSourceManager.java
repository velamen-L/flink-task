package com.flink.business.core.service;

import com.flink.business.core.config.JobDomainConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.springframework.stereotype.Service;

/**
 * 事件源管理器
 * 
 * 职责：
 * 1. 注册和管理所有事件源表
 * 2. 注册和管理所有维表
 * 3. 管理表的生命周期
 * 4. 提供表的元数据信息
 * 
 * 设计特点：
 * - 配置驱动的表注册
 * - 支持多种连接器类型
 * - 统一的表管理接口
 * - 完整的错误处理和日志
 */
@Slf4j
@Service
public class EventSourceManager {

    /**
     * 注册事件源表
     */
    public void registerEventSource(StreamTableEnvironment tableEnv, JobDomainConfig.EventSourceConfig sourceConfig) {
        String sourceName = sourceConfig.getSourceName();
        String topicName = sourceConfig.getTopicName();
        
        log.info("注册事件源: {} -> {}", sourceName, topicName);
        
        try {
            String createTableSql = buildEventSourceCreateTableSql(sourceConfig);
            tableEnv.executeSql(createTableSql);
            
            log.info("事件源注册成功: {}", sourceName);
            
        } catch (Exception e) {
            log.error("注册事件源失败: {}", sourceName, e);
            throw new RuntimeException("注册事件源失败: " + sourceName, e);
        }
    }

    /**
     * 注册维表
     */
    public void registerDimTable(StreamTableEnvironment tableEnv, JobDomainConfig.DimTableConfig dimConfig) {
        String tableName = dimConfig.getTableName();
        
        log.info("注册维表: {}", tableName);
        
        try {
            String createTableSql = buildDimTableCreateTableSql(dimConfig);
            tableEnv.executeSql(createTableSql);
            
            log.info("维表注册成功: {}", tableName);
            
        } catch (Exception e) {
            log.error("注册维表失败: {}", tableName, e);
            throw new RuntimeException("注册维表失败: " + tableName, e);
        }
    }

    /**
     * 构建事件源建表SQL
     */
    private String buildEventSourceCreateTableSql(JobDomainConfig.EventSourceConfig sourceConfig) {
        String sourceName = sourceConfig.getSourceName();
        String topicName = sourceConfig.getTopicName();
        JobDomainConfig.ConsumerConfig consumerConfig = sourceConfig.getConsumer();
        
        return String.format("""
            CREATE TABLE %s (
                domain STRING,
                type STRING,
                payload STRING,
                event_time TIMESTAMP(3),
                source STRING,
                -- 定义水位线
                WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
            ) WITH (
                'connector' = 'kafka',
                'topic' = '%s',
                'properties.bootstrap.servers' = '${kafka.bootstrap.servers:localhost:9092}',
                'properties.group.id' = '%s',
                'scan.startup.mode' = '%s',
                'format' = 'json',
                'json.ignore-parse-errors' = 'true',
                'json.fail-on-missing-field' = 'false'
            )
        """, sourceName, topicName, consumerConfig.getGroupId(), consumerConfig.getStartupMode());
    }

    /**
     * 构建维表建表SQL
     */
    private String buildDimTableCreateTableSql(JobDomainConfig.DimTableConfig dimConfig) {
        String tableName = dimConfig.getTableName();
        String connector = dimConfig.getConnector();
        JobDomainConfig.CacheConfig cacheConfig = dimConfig.getCache();
        
        // 根据表名确定表结构（这里可以从元数据服务获取）
        String tableSchema = getTableSchema(tableName);
        
        // 构建连接器配置
        String connectorConfig = buildConnectorConfig(connector, dimConfig.getConnectionProperties(), cacheConfig);
        
        return String.format("""
            CREATE TABLE %s %s
            WITH %s
        """, tableName, tableSchema, connectorConfig);
    }

    /**
     * 获取表结构定义
     */
    private String getTableSchema(String tableName) {
        return switch (tableName) {
            case "tower_pattern" -> """
                (
                    id STRING NOT NULL,
                    name STRING,
                    type INT,
                    subject STRING,
                    difficulty DECIMAL(5, 3),
                    modify_time BIGINT,
                    PRIMARY KEY (id) NOT ENFORCED
                )
                COMMENT '知识点模式维表'
            """;
            case "tower_teaching_type_pt" -> """
                (
                    id BIGINT NOT NULL,
                    teaching_type_id BIGINT,
                    pt_id STRING,
                    order_num INT,
                    is_delete TINYINT,
                    modify_time TIMESTAMP(3),
                    PRIMARY KEY (id) NOT ENFORCED
                )
                COMMENT '教学类型-知识点映射维表'
            """;
            case "tower_teaching_type" -> """
                (
                    id BIGINT NOT NULL,
                    chapter_id STRING,
                    teaching_type_name STRING,
                    is_delete TINYINT,
                    modify_time TIMESTAMP(3),
                    PRIMARY KEY (id) NOT ENFORCED
                )
                COMMENT '教学类型维表'
            """;
            case "user_profile" -> """
                (
                    user_id STRING NOT NULL,
                    name STRING,
                    age INT,
                    grade STRING,
                    school STRING,
                    create_time TIMESTAMP(3),
                    update_time TIMESTAMP(3),
                    PRIMARY KEY (user_id) NOT ENFORCED
                )
                COMMENT '用户画像维表'
            """;
            default -> """
                (
                    id STRING NOT NULL,
                    data STRING,
                    create_time TIMESTAMP(3),
                    PRIMARY KEY (id) NOT ENFORCED
                )
                COMMENT '通用维表'
            """;
        };
    }

    /**
     * 构建连接器配置
     */
    private String buildConnectorConfig(String connector, 
                                      java.util.Map<String, String> connectionProperties, 
                                      JobDomainConfig.CacheConfig cacheConfig) {
        return switch (connector) {
            case "jdbc" -> buildJdbcConnectorConfig(connectionProperties, cacheConfig);
            case "odps" -> buildOdpsConnectorConfig(connectionProperties);
            case "kafka" -> buildKafkaConnectorConfig(connectionProperties);
            default -> throw new IllegalArgumentException("不支持的连接器类型: " + connector);
        };
    }

    /**
     * 构建JDBC连接器配置
     */
    private String buildJdbcConnectorConfig(java.util.Map<String, String> properties, 
                                          JobDomainConfig.CacheConfig cacheConfig) {
        return String.format("""
            (
                'connector' = 'jdbc',
                'url' = '${mysql.url:jdbc:mysql://localhost:3306/tower}',
                'table-name' = '%s',
                'username' = '${mysql.username:username}',
                'password' = '${mysql.password:password}',
                'lookup.cache.max-rows' = '%d',
                'lookup.cache.ttl' = '%s',
                'lookup.cache.caching-missing-key' = '%s',
                'lookup.max-retries' = '%d'
            )
        """, 
            properties.getOrDefault("table-name", "unknown"),
            cacheConfig.getMaxRows(),
            cacheConfig.getTtl(),
            cacheConfig.isEnableMissingKeyCache(),
            cacheConfig.getMaxRetries()
        );
    }

    /**
     * 构建ODPS连接器配置
     */
    private String buildOdpsConnectorConfig(java.util.Map<String, String> properties) {
        return String.format("""
            (
                'connector' = 'odps',
                'accessId' = '${odps.access.id}',
                'accessKey' = '${odps.access.key}',
                'endpoint' = '${odps.endpoint}',
                'project' = '${odps.project}',
                'tableName' = '%s',
                'sink.operation' = '%s',
                'enableUpsert' = 'true',
                'upsert.write.bucket.num' = '16'
            )
        """, 
            properties.getOrDefault("table-name", "unknown"),
            properties.getOrDefault("operation", "upsert")
        );
    }

    /**
     * 构建Kafka连接器配置
     */
    private String buildKafkaConnectorConfig(java.util.Map<String, String> properties) {
        return String.format("""
            (
                'connector' = 'kafka',
                'topic' = '%s',
                'properties.bootstrap.servers' = '${kafka.bootstrap.servers:localhost:9092}',
                'format' = 'json'
            )
        """, properties.getOrDefault("topic", "unknown"));
    }

    /**
     * 检查表是否存在
     */
    public boolean tableExists(StreamTableEnvironment tableEnv, String tableName) {
        try {
            tableEnv.from(tableName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 删除表
     */
    public void dropTable(StreamTableEnvironment tableEnv, String tableName) {
        try {
            tableEnv.executeSql("DROP TABLE IF EXISTS " + tableName);
            log.info("删除表成功: {}", tableName);
        } catch (Exception e) {
            log.warn("删除表失败: {}", tableName, e);
        }
    }
}
