package com.flink.business.core.service;

import com.flink.business.core.processor.AbstractBusinessProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.table.api.Table;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 指标收集器
 * 
 * 职责：
 * 1. 收集各个作业域的运行指标
 * 2. 记录业务处理指标和性能指标
 * 3. 提供统一的指标查询接口
 * 4. 支持自定义指标扩展
 * 
 * 设计特点：
 * - 线程安全的指标收集
 * - 支持多维度指标分类
 * - 内存高效的指标存储
 * - 可扩展的指标类型
 */
@Slf4j
@Service
public class MetricsCollector {

    /**
     * 域执行次数计数器
     */
    private final Map<String, AtomicLong> domainExecutionCount = new ConcurrentHashMap<>();

    /**
     * 域错误次数计数器
     */
    private final Map<String, AtomicLong> domainErrorCount = new ConcurrentHashMap<>();

    /**
     * 域最后执行时间
     */
    private final Map<String, Long> domainLastExecutionTime = new ConcurrentHashMap<>();

    /**
     * 自定义指标存储
     */
    private final Map<String, Map<String, Object>> customMetrics = new ConcurrentHashMap<>();

    /**
     * 处理器指标注册表
     */
    private final Map<String, AbstractBusinessProcessor> registeredProcessors = new ConcurrentHashMap<>();

    /**
     * 注册域指标
     */
    public void registerDomainMetrics(String domainName, AbstractBusinessProcessor processor) {
        registeredProcessors.put(domainName, processor);
        domainExecutionCount.putIfAbsent(domainName, new AtomicLong(0));
        domainErrorCount.putIfAbsent(domainName, new AtomicLong(0));
        
        log.info("注册域指标: {}", domainName);
    }

    /**
     * 记录域执行
     */
    public void recordDomainExecution(String domainName) {
        domainExecutionCount.computeIfAbsent(domainName, k -> new AtomicLong(0)).incrementAndGet();
        domainLastExecutionTime.put(domainName, System.currentTimeMillis());
        
        log.debug("记录域执行: {}", domainName);
    }

    /**
     * 记录域错误
     */
    public void recordDomainError(String domainName, Exception error) {
        domainErrorCount.computeIfAbsent(domainName, k -> new AtomicLong(0)).incrementAndGet();
        
        // 记录错误详情
        Map<String, Object> errorMetrics = customMetrics.computeIfAbsent(
            domainName + "_errors", k -> new ConcurrentHashMap<>());
        errorMetrics.put("last_error", error.getMessage());
        errorMetrics.put("last_error_time", System.currentTimeMillis());
        
        log.warn("记录域错误: {} - {}", domainName, error.getMessage());
    }

    /**
     * 记录自定义指标
     */
    public void recordCustomMetrics(String domainName, String metricName, Object metricValue) {
        String key = domainName + "_" + metricName;
        Map<String, Object> metrics = customMetrics.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        
        if (metricValue instanceof Table) {
            // 对于Table类型，记录基本信息
            metrics.put("type", "table");
            metrics.put("timestamp", System.currentTimeMillis());
        } else {
            metrics.put("value", metricValue);
            metrics.put("timestamp", System.currentTimeMillis());
        }
        
        log.debug("记录自定义指标: {} = {}", key, metricValue);
    }

    /**
     * 记录处理延迟
     */
    public void recordProcessingLatency(String domainName, long latencyMs) {
        String key = domainName + "_latency";
        Map<String, Object> latencyMetrics = customMetrics.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        
        latencyMetrics.put("current_latency", latencyMs);
        latencyMetrics.put("timestamp", System.currentTimeMillis());
        
        // 计算平均延迟
        AtomicLong totalLatency = (AtomicLong) latencyMetrics.computeIfAbsent("total_latency", k -> new AtomicLong(0));
        AtomicLong count = (AtomicLong) latencyMetrics.computeIfAbsent("count", k -> new AtomicLong(0));
        
        totalLatency.addAndGet(latencyMs);
        count.incrementAndGet();
        
        double avgLatency = totalLatency.get() / (double) count.get();
        latencyMetrics.put("avg_latency", avgLatency);
        
        if (latencyMs > 300000) {  // 5分钟
            log.warn("处理延迟过高: {} - {}ms", domainName, latencyMs);
        }
    }

    /**
     * 记录数据质量指标
     */
    public void recordDataQuality(String domainName, String qualityType, double value) {
        String key = domainName + "_data_quality_" + qualityType;
        Map<String, Object> qualityMetrics = customMetrics.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        
        qualityMetrics.put("value", value);
        qualityMetrics.put("timestamp", System.currentTimeMillis());
        
        // 检查质量阈值
        if ("completeness".equals(qualityType) && value < 0.95) {
            log.warn("数据完整性过低: {} - {}", domainName, value);
        } else if ("accuracy".equals(qualityType) && value < 0.99) {
            log.warn("数据准确性过低: {} - {}", domainName, value);
        }
    }

    /**
     * 记录JOIN成功率
     */
    public void recordJoinSuccessRate(String domainName, String joinType, double successRate) {
        String key = domainName + "_join_" + joinType;
        Map<String, Object> joinMetrics = customMetrics.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        
        joinMetrics.put("success_rate", successRate);
        joinMetrics.put("timestamp", System.currentTimeMillis());
        
        if (successRate < 0.95) {
            log.warn("JOIN成功率过低: {} {} - {}", domainName, joinType, successRate);
        }
    }

    /**
     * 获取域执行统计
     */
    public DomainExecutionStats getDomainExecutionStats(String domainName) {
        return new DomainExecutionStats(
            domainName,
            domainExecutionCount.getOrDefault(domainName, new AtomicLong(0)).get(),
            domainErrorCount.getOrDefault(domainName, new AtomicLong(0)).get(),
            domainLastExecutionTime.getOrDefault(domainName, 0L)
        );
    }

    /**
     * 获取所有域的统计
     */
    public Map<String, DomainExecutionStats> getAllDomainStats() {
        Map<String, DomainExecutionStats> stats = new ConcurrentHashMap<>();
        
        for (String domainName : domainExecutionCount.keySet()) {
            stats.put(domainName, getDomainExecutionStats(domainName));
        }
        
        return stats;
    }

    /**
     * 获取自定义指标
     */
    public Map<String, Object> getCustomMetrics(String domainName) {
        Map<String, Object> result = new ConcurrentHashMap<>();
        
        // 查找所有以domainName开头的指标
        for (Map.Entry<String, Map<String, Object>> entry : customMetrics.entrySet()) {
            if (entry.getKey().startsWith(domainName + "_")) {
                String metricName = entry.getKey().substring((domainName + "_").length());
                result.put(metricName, entry.getValue());
            }
        }
        
        return result;
    }

    /**
     * 获取指标摘要
     */
    public String getMetricsSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== 多源业务驱动架构指标摘要 ===\n");
        
        for (Map.Entry<String, AtomicLong> entry : domainExecutionCount.entrySet()) {
            String domainName = entry.getKey();
            long execCount = entry.getValue().get();
            long errorCount = domainErrorCount.getOrDefault(domainName, new AtomicLong(0)).get();
            long lastExecTime = domainLastExecutionTime.getOrDefault(domainName, 0L);
            
            double errorRate = execCount > 0 ? (double) errorCount / execCount : 0.0;
            
            summary.append(String.format(
                "域: %s - 执行: %d次, 错误: %d次 (%.2f%%), 最后执行: %s\n",
                domainName, execCount, errorCount, errorRate * 100,
                lastExecTime > 0 ? new java.util.Date(lastExecTime).toString() : "从未执行"
            ));
        }
        
        return summary.toString();
    }

    /**
     * 重置指标
     */
    public void resetMetrics() {
        domainExecutionCount.clear();
        domainErrorCount.clear();
        domainLastExecutionTime.clear();
        customMetrics.clear();
        registeredProcessors.clear();
        
        log.info("所有指标已重置");
    }

    /**
     * 重置特定域的指标
     */
    public void resetMetrics(String domainName) {
        domainExecutionCount.remove(domainName);
        domainErrorCount.remove(domainName);
        domainLastExecutionTime.remove(domainName);
        registeredProcessors.remove(domainName);
        
        // 清除自定义指标
        customMetrics.entrySet().removeIf(entry -> entry.getKey().startsWith(domainName + "_"));
        
        log.info("域 {} 的指标已重置", domainName);
    }

    /**
     * 域执行统计数据类
     */
    public static class DomainExecutionStats {
        public final String domainName;
        public final long executionCount;
        public final long errorCount;
        public final long lastExecutionTime;
        
        public DomainExecutionStats(String domainName, long executionCount, long errorCount, long lastExecutionTime) {
            this.domainName = domainName;
            this.executionCount = executionCount;
            this.errorCount = errorCount;
            this.lastExecutionTime = lastExecutionTime;
        }
        
        public double getErrorRate() {
            return executionCount > 0 ? (double) errorCount / executionCount : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "DomainStats{domain='%s', executions=%d, errors=%d, errorRate=%.2f%%, lastExecution=%s}",
                domainName, executionCount, errorCount, getErrorRate() * 100,
                lastExecutionTime > 0 ? new java.util.Date(lastExecutionTime) : "Never"
            );
        }
    }
}
