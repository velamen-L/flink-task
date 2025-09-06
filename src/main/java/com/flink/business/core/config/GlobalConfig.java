package com.flink.business.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 全局配置
 * 
 * 管理整个应用的全局配置，包括：
 * - Flink作业基础配置
 * - Checkpoint配置
 * - 监控配置
 * - 基础设施配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "flink.business")
public class GlobalConfig {

    /**
     * 作业基础配置
     */
    private JobConfig job = new JobConfig();

    /**
     * Checkpoint配置
     */
    private CheckpointConfig checkpoint = new CheckpointConfig();

    /**
     * 监控配置
     */
    private MonitoringConfig monitoring = new MonitoringConfig();

    /**
     * 基础设施配置
     */
    private InfrastructureConfig infrastructure = new InfrastructureConfig();

    @Data
    public static class JobConfig {
        private String applicationName = "multi-source-business-app";
        private String version = "2.0.0";
        private int parallelism = 4;
        private String timeZone = "Asia/Shanghai";
        private String defaultDomain;  // 默认作业域
        private List<String> enabledDomains;  // 启用的作业域列表
    }

    @Data
    public static class CheckpointConfig {
        private long interval = 60000L;  // 1分钟
        private long minPause = 30000L;  // 30秒
        private long timeout = 300000L;  // 5分钟
        private String stateBackend = "rocksdb";
        private String checkpointStorage = "filesystem";
        private int maxConcurrentCheckpoints = 1;
        private boolean enableUnalignedCheckpoints = false;
        private long checkpointRetentionTime = 86400000L;  // 1天
    }

    @Data
    public static class MonitoringConfig {
        private boolean enableMetrics = true;
        private long metricsInterval = 60000L;  // 1分钟
        private List<String> enabledReporters = List.of("jmx", "prometheus");
        private Map<String, Object> alertThresholds;
        private boolean enableHealthCheck = true;
        private long healthCheckInterval = 30000L;  // 30秒
    }

    @Data
    public static class InfrastructureConfig {
        private KafkaConfig kafka = new KafkaConfig();
        private StorageConfig storage = new StorageConfig();
        private CacheConfig cache = new CacheConfig();
    }

    @Data
    public static class KafkaConfig {
        private String bootstrapServers = "localhost:9092";
        private Map<String, String> consumerDefaults;
        private Map<String, String> producerDefaults;
        private int retryAttempts = 3;
        private long retryDelay = 1000L;
    }

    @Data
    public static class StorageConfig {
        private JdbcConfig jdbc = new JdbcConfig();
        private OdpsConfig odps = new OdpsConfig();
    }

    @Data
    public static class JdbcConfig {
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private int maxPoolSize = 20;
        private int minIdleSize = 5;
        private long connectionTimeout = 30000L;
        private long idleTimeout = 600000L;
        private long maxLifetime = 1800000L;
    }

    @Data
    public static class OdpsConfig {
        private String endpoint;
        private String project;
        private String accessId;
        private String accessKey;
        private int tunnelQuota = 10;
        private long tunnelTimeout = 300000L;
    }

    @Data
    public static class CacheConfig {
        private String provider = "caffeine";  // caffeine, redis, hazelcast
        private long defaultTtl = 1800000L;  // 30分钟
        private int defaultMaxSize = 10000;
        private boolean enableStatistics = true;
    }
}
