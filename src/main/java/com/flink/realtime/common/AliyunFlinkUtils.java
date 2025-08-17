package com.flink.realtime.common;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.concurrent.TimeUnit;

/**
 * 阿里云Flink环境配置工具类
 * 根据阿里云Flink最佳实践配置执行环境
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class AliyunFlinkUtils {
    
    /**
     * 获取针对阿里云优化的Flink流执行环境
     * 
     * @param parallelism 并行度
     * @return StreamExecutionEnvironment
     */
    public static StreamExecutionEnvironment getAliyunStreamExecutionEnvironment(int parallelism) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // 设置并行度
        env.setParallelism(parallelism);
        
        // 配置checkpoint（阿里云推荐配置）
        configureAliyunCheckpoint(env);
        
        // 配置重启策略（阿里云推荐）
        configureAliyunRestartStrategy(env);
        
        // 配置状态后端（阿里云推荐）
        configureAliyunStateBackend(env);
        
        // 阿里云特有的性能优化配置
        configureAliyunPerformance(env);
        
        return env;
    }
    
    /**
     * 配置阿里云推荐的checkpoint设置
     */
    private static void configureAliyunCheckpoint(StreamExecutionEnvironment env) {
        // 开启checkpoint，阿里云推荐间隔60秒
        env.enableCheckpointing(60000);
        
        CheckpointConfig checkpointConfig = env.getCheckpointConfig();
        
        // 设置checkpoint模式为EXACTLY_ONCE（阿里云默认推荐）
        checkpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        
        // 设置checkpoint超时时间为10分钟（阿里云推荐）
        checkpointConfig.setCheckpointTimeout(600000);
        
        // 设置同时进行的checkpoint最大数量为1（阿里云推荐）
        checkpointConfig.setMaxConcurrentCheckpoints(1);
        
        // 设置两次checkpoint之间的最小间隔（阿里云推荐）
        checkpointConfig.setMinPauseBetweenCheckpoints(5000);
        
        // 允许checkpoint失败时作业继续运行（阿里云推荐）
        checkpointConfig.setTolerableCheckpointFailureNumber(3);
        
        // 当作业取消时，保留checkpoint（阿里云推荐）
        checkpointConfig.setExternalizedCheckpointCleanup(
                CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
    }
    
    /**
     * 配置阿里云推荐的重启策略
     */
    private static void configureAliyunRestartStrategy(StreamExecutionEnvironment env) {
        // 阿里云推荐：固定延迟重启策略，重启3次，每次间隔10秒
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                3, // 重启次数
                Time.of(10, TimeUnit.SECONDS) // 重启间隔
        ));
    }
    
    /**
     * 配置阿里云推荐的状态后端
     */
    private static void configureAliyunStateBackend(StreamExecutionEnvironment env) {
        // 阿里云推荐使用RocksDB状态后端（需要在pom.xml中添加依赖）
        // env.setStateBackend(new RocksDBStateBackend("oss://your-bucket/checkpoints"));
        
        // 如果不使用RocksDB，使用HashMapStateBackend
        env.setStateBackend(new HashMapStateBackend());
        
        // 注意：在阿里云环境中，checkpoint存储会自动配置到阿里云的OSS
    }
    
    /**
     * 配置阿里云特有的性能优化
     */
    private static void configureAliyunPerformance(StreamExecutionEnvironment env) {
        Configuration config = new Configuration();
        
        // 启用对象重用（阿里云推荐）
        config.setBoolean("pipeline.object-reuse", true);
        
        // 网络缓冲区配置（阿里云推荐）
        config.setString("taskmanager.memory.network.fraction", "0.1");
        config.setString("taskmanager.memory.network.min", "64mb");
        config.setString("taskmanager.memory.network.max", "1gb");
        
        // 设置空闲状态保留时间（阿里云推荐）
        config.setString("table.exec.state.ttl", "3600000"); // 1小时
        
        env.configure(config);
    }
    
    /**
     * 获取阿里云环境变量配置
     */
    public static class AliyunConfig {
        
        /**
         * 获取阿里云Kafka配置
         */
        public static String getKafkaBootstrapServers() {
            // 阿里云Kafka实例的连接地址格式
            return System.getProperty("kafka.bootstrap.servers", 
                "your-kafka-instance.kafka.cn-hangzhou.aliyuncs.com:9092");
        }
        
        /**
         * 获取阿里云RDS MySQL配置
         */
        public static String getMySQLUrl() {
            // 阿里云RDS MySQL连接地址格式
            return System.getProperty("mysql.url", 
                "jdbc:mysql://your-rds-instance.mysql.rds.aliyuncs.com:3306/your_db?" +
                "useSSL=true&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC");
        }
        
        /**
         * 获取阿里云VPC内网连接配置
         */
        public static boolean isVPCEnvironment() {
            return "true".equals(System.getProperty("aliyun.vpc.enabled", "false"));
        }
    }
}
