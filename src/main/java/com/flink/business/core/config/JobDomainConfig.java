package com.flink.business.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 作业域配置基类
 * 
 * 定义每个作业域的标准配置结构，包括：
 * - 作业元数据
 * - 事件源配置
 * - 维表配置
 * - 输出配置
 * - 处理策略配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "job.domain")
public class JobDomainConfig {

    /**
     * 作业域映射，key为域名，value为域配置
     */
    private Map<String, DomainConfig> domains;

    @Data
    public static class DomainConfig {
        /**
         * 作业元数据
         */
        private JobMetadata metadata = new JobMetadata();

        /**
         * 事件源配置列表
         */
        private List<EventSourceConfig> eventSources;

        /**
         * 维表配置列表
         */
        private List<DimTableConfig> dimTables;

        /**
         * 输出配置列表
         */
        private List<OutputConfig> outputs;

        /**
         * 处理策略配置
         */
        private ProcessingStrategy processingStrategy = new ProcessingStrategy();

        /**
         * 数据质量配置
         */
        private DataQualityConfig dataQuality = new DataQualityConfig();
    }

    @Data
    public static class JobMetadata {
        private String jobName;
        private String jobType;  // SINGLE_DOMAIN, MULTI_DOMAIN, CROSS_DOMAIN
        private String description;
        private String version;
        private List<String> businessDomains;  // 涉及的业务域
        private List<String> businessGoals;    // 业务目标
        private String author;
        private String createDate;
    }

    @Data
    public static class EventSourceConfig {
        private String sourceName;
        private String eventDomain;
        private String topicName;
        private List<String> interestedEventTypes;
        private EventFilterConfig filter = new EventFilterConfig();
        private ConsumerConfig consumer = new ConsumerConfig();
        private TransformConfig transform = new TransformConfig();
    }

    @Data
    public static class EventFilterConfig {
        private Map<String, Object> basicFilters;
        private List<String> customFilters;
        private TimeWindowConfig timeWindow;
    }

    @Data
    public static class TimeWindowConfig {
        private boolean enabled = false;
        private String windowType = "TUMBLING";
        private long windowSize = 300000L;  // 5分钟
        private long slideSize = 60000L;    // 1分钟
    }

    @Data
    public static class ConsumerConfig {
        private String groupId;
        private String startupMode = "latest";
        private Map<String, String> kafkaProperties;
    }

    @Data
    public static class TransformConfig {
        private Map<String, String> fieldMapping;
        private Map<String, String> typeConversion;
        private Map<String, String> computedFields;
    }

    @Data
    public static class DimTableConfig {
        private String tableName;
        private String tableAlias;
        private String connector;
        private Map<String, String> connectionProperties;
        private CacheConfig cache = new CacheConfig();
        private List<JoinConfig> joinConditions;
    }

    @Data
    public static class JoinConfig {
        private String eventSource;
        private String joinCondition;
        private String joinType = "LEFT";
        private String additionalConditions;
        private boolean required = false;
    }

    @Data
    public static class CacheConfig {
        private String ttl = "30min";
        private int maxRows = 100000;
        private boolean enableMissingKeyCache = false;
        private int maxRetries = 3;
    }

    @Data
    public static class OutputConfig {
        private String outputName;
        private String outputType;  // TABLE, KAFKA, FILE
        private String targetName;
        private Map<String, String> properties;
        private List<String> primaryKey;
    }

    @Data
    public static class ProcessingStrategy {
        private String processingMode = "UNION";  // UNION, JOIN, AGGREGATE
        private String timeAlignmentStrategy = "EVENT_TIME";
        private LateDataConfig lateData = new LateDataConfig();
        private StateConfig state = new StateConfig();
    }

    @Data
    public static class LateDataConfig {
        private boolean allowLateData = true;
        private long allowedLateness = 300000L;  // 5分钟
        private String lateDataHandling = "IGNORE";  // IGNORE, SIDE_OUTPUT, REPROCESS
    }

    @Data
    public static class StateConfig {
        private String stateBackend = "rocksdb";
        private long stateTtl = 86400000L;  // 1天
        private boolean enableStateCompression = true;
    }

    @Data
    public static class DataQualityConfig {
        private boolean enableValidation = true;
        private List<String> requiredFields;
        private Map<String, Object> valueRanges;
        private double completenessThreshold = 0.95;
        private List<String> businessRules;
    }

    /**
     * 获取指定域的配置
     */
    public DomainConfig getDomainConfig(String domainName) {
        return domains != null ? domains.get(domainName) : null;
    }

    /**
     * 检查域是否存在
     */
    public boolean hasDomain(String domainName) {
        return domains != null && domains.containsKey(domainName);
    }

    /**
     * 获取所有启用的域名
     */
    public List<String> getEnabledDomains() {
        return domains != null ? domains.keySet().stream().toList() : List.of();
    }
}
