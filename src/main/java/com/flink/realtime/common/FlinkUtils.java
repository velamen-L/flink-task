package com.flink.realtime.common;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.concurrent.TimeUnit;

/**
 * Flink 环境配置工具类
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class FlinkUtils {
    
    /**
     * 获取配置好的Flink流执行环境
     * 
     * @param parallelism 并行度
     * @return StreamExecutionEnvironment
     */
    public static StreamExecutionEnvironment getStreamExecutionEnvironment(int parallelism) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // 设置并行度
        env.setParallelism(parallelism);
        
        // 配置checkpoint
        configureCheckpoint(env);
        
        // 配置重启策略
        configureRestartStrategy(env);
        
        // 配置状态后端
        configureStateBackend(env);
        
        return env;
    }
    
    /**
     * 配置checkpoint
     */
    private static void configureCheckpoint(StreamExecutionEnvironment env) {
        // 开启checkpoint，间隔5秒
        env.enableCheckpointing(5000);
        
        CheckpointConfig checkpointConfig = env.getCheckpointConfig();
        
        // 设置checkpoint模式为EXACTLY_ONCE
        checkpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        
        // 设置checkpoint超时时间为1分钟
        checkpointConfig.setCheckpointTimeout(60000);
        
        // 设置同时进行的checkpoint最大数量
        checkpointConfig.setMaxConcurrentCheckpoints(1);
        
        // 设置两次checkpoint之间的最小间隔
        checkpointConfig.setMinPauseBetweenCheckpoints(500);
        
        // 当作业取消时，保留checkpoint
        checkpointConfig.setExternalizedCheckpointCleanup(
                CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
    }
    
    /**
     * 配置重启策略
     */
    private static void configureRestartStrategy(StreamExecutionEnvironment env) {
        // 固定延迟重启策略：重启3次，每次间隔10秒
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                3, // 重启次数
                Time.of(10, TimeUnit.SECONDS) // 重启间隔
        ));
    }
    
    /**
     * 配置状态后端
     */
    private static void configureStateBackend(StreamExecutionEnvironment env) {
        // 使用HashMapStateBackend作为状态后端
        env.setStateBackend(new HashMapStateBackend());
        
        // 可以配置RocksDB状态后端（需要添加相应依赖）
        // env.setStateBackend(new EmbeddedRocksDBStateBackend());
        
        // 设置checkpoint存储路径（生产环境中应使用HDFS等分布式存储）
        // env.getCheckpointConfig().setCheckpointStorage("hdfs://namenode:port/flink-checkpoints");
    }
}
