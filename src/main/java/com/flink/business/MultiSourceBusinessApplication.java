package com.flink.business;

import com.flink.business.core.service.JobDomainManager;
import com.flink.business.core.config.GlobalConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 多源业务驱动Flink应用
 * 
 * 核心特性：
 * - 支持多个作业域的配置和处理
 * - 事件域和作业域灵活映射
 * - 基于Table API的声明式编程
 * - 配置驱动的架构设计
 * 
 * 架构模式：
 * - 一个应用支持多个作业域
 * - 每个作业域可以消费多个事件域
 * - 统一的技术框架和运维规范
 * 
 * @author 数据开发团队
 * @version 2.0.0
 * @since 2024-12-27
 */
@Slf4j
@SpringBootApplication
public class MultiSourceBusinessApplication implements CommandLineRunner {

    @Autowired
    private JobDomainManager jobDomainManager;

    @Autowired
    private GlobalConfig globalConfig;

    public static void main(String[] args) {
        // 支持指定作业域参数
        if (args.length > 0) {
            System.setProperty("job.domain.active", args[0]);
            log.info("指定作业域: {}", args[0]);
        }
        
        SpringApplication.run(MultiSourceBusinessApplication.class, args);
    }

    @Bean
    public StreamExecutionEnvironment streamExecutionEnvironment() {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // 配置Checkpoint
        env.enableCheckpointing(globalConfig.getCheckpoint().getInterval());
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(globalConfig.getCheckpoint().getMinPause());
        env.getCheckpointConfig().setCheckpointTimeout(globalConfig.getCheckpoint().getTimeout());
        
        // 配置并行度
        env.setParallelism(globalConfig.getJob().getParallelism());
        
        // 配置状态后端
        if ("rocksdb".equals(globalConfig.getCheckpoint().getStateBackend())) {
            // 这里可以配置RocksDB状态后端
            log.info("使用RocksDB状态后端");
        }
        
        log.info("Flink StreamExecutionEnvironment 配置完成");
        return env;
    }

    @Bean
    public StreamTableEnvironment streamTableEnvironment(StreamExecutionEnvironment env) {
        EnvironmentSettings settings = EnvironmentSettings
            .newInstance()
            .inStreamingMode()
            .build();
            
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        
        // 配置Table API相关参数
        tableEnv.getConfig().getConfiguration()
            .setString("table.exec.source.idle-timeout", "30s");
        tableEnv.getConfig().getConfiguration()
            .setString("table.optimizer.join-reorder-enabled", "true");
        tableEnv.getConfig().getConfiguration()
            .setString("table.exec.mini-batch.enabled", "true");
        tableEnv.getConfig().getConfiguration()
            .setString("table.exec.mini-batch.allow-latency", "1s");
        tableEnv.getConfig().getConfiguration()
            .setString("table.exec.mini-batch.size", "1000");
            
        log.info("Flink StreamTableEnvironment 配置完成");
        return tableEnv;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("启动多源业务驱动Flink应用...");
            
            // 获取激活的作业域
            String activeDomain = System.getProperty("job.domain.active", 
                globalConfig.getJob().getDefaultDomain());
            
            if (activeDomain == null || activeDomain.trim().isEmpty()) {
                log.error("未指定作业域，请通过命令行参数或配置文件指定");
                throw new IllegalArgumentException("未指定作业域");
            }
            
            log.info("激活作业域: {}", activeDomain);
            
            // 执行指定作业域的处理
            jobDomainManager.executeJobDomain(activeDomain);
            
            log.info("多源业务驱动Flink应用启动完成，等待作业执行...");
            
        } catch (Exception e) {
            log.error("多源业务驱动Flink应用启动失败", e);
            throw e;
        }
    }
}
