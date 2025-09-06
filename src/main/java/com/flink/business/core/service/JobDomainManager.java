package com.flink.business.core.service;

import com.flink.business.core.config.JobDomainConfig;
import com.flink.business.core.processor.ProcessorFactory;
import com.flink.business.core.processor.AbstractBusinessProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 作业域管理器
 * 
 * 核心职责：
 * 1. 管理所有作业域的配置和生命周期
 * 2. 根据作业域创建对应的处理器
 * 3. 协调作业域之间的资源和依赖
 * 4. 提供作业域的运行时管理
 * 
 * 设计特点：
 * - 配置驱动的作业域管理
 * - 支持动态加载和卸载作业域
 * - 统一的作业域生命周期管理
 * - 完整的监控和告警集成
 */
@Slf4j
@Service
public class JobDomainManager {

    @Autowired
    private JobDomainConfig jobDomainConfig;

    @Autowired
    private ProcessorFactory processorFactory;

    @Autowired
    private ConfigLoader configLoader;

    @Autowired
    private MetricsCollector metricsCollector;

    /**
     * 执行指定的作业域
     */
    public void executeJobDomain(String domainName) {
        log.info("开始执行作业域: {}", domainName);
        
        try {
            // 1. 验证作业域配置
            validateDomainConfig(domainName);
            
            // 2. 加载作业域配置
            JobDomainConfig.DomainConfig domainConfig = loadDomainConfig(domainName);
            
            // 3. 创建作业域处理器
            AbstractBusinessProcessor processor = processorFactory.createProcessor(domainName, domainConfig);
            
            // 4. 执行作业域处理
            processor.execute();
            
            // 5. 注册监控指标
            metricsCollector.registerDomainMetrics(domainName, processor);
            
            log.info("作业域 {} 启动完成", domainName);
            
        } catch (Exception e) {
            log.error("执行作业域 {} 失败", domainName, e);
            throw new RuntimeException("作业域执行失败: " + domainName, e);
        }
    }

    /**
     * 获取所有可用的作业域
     */
    public java.util.List<String> getAvailableDomains() {
        return jobDomainConfig.getEnabledDomains();
    }

    /**
     * 检查作业域是否可用
     */
    public boolean isDomainAvailable(String domainName) {
        return jobDomainConfig.hasDomain(domainName);
    }

    /**
     * 获取作业域状态
     */
    public DomainStatus getDomainStatus(String domainName) {
        if (!isDomainAvailable(domainName)) {
            return DomainStatus.NOT_FOUND;
        }
        
        // 这里可以实现更复杂的状态检查逻辑
        return DomainStatus.READY;
    }

    /**
     * 验证作业域配置
     */
    private void validateDomainConfig(String domainName) {
        if (domainName == null || domainName.trim().isEmpty()) {
            throw new IllegalArgumentException("作业域名称不能为空");
        }
        
        if (!jobDomainConfig.hasDomain(domainName)) {
            throw new IllegalArgumentException("作业域不存在: " + domainName);
        }
        
        JobDomainConfig.DomainConfig config = jobDomainConfig.getDomainConfig(domainName);
        
        // 验证基础配置
        if (config.getMetadata() == null) {
            throw new IllegalArgumentException("作业域元数据配置缺失: " + domainName);
        }
        
        if (config.getEventSources() == null || config.getEventSources().isEmpty()) {
            throw new IllegalArgumentException("作业域事件源配置缺失: " + domainName);
        }
        
        if (config.getOutputs() == null || config.getOutputs().isEmpty()) {
            throw new IllegalArgumentException("作业域输出配置缺失: " + domainName);
        }
        
        // 验证事件源配置
        for (JobDomainConfig.EventSourceConfig sourceConfig : config.getEventSources()) {
            if (sourceConfig.getSourceName() == null || sourceConfig.getTopicName() == null) {
                throw new IllegalArgumentException("事件源配置不完整: " + domainName);
            }
        }
        
        log.info("作业域配置验证通过: {}", domainName);
    }

    /**
     * 加载作业域配置
     */
    private JobDomainConfig.DomainConfig loadDomainConfig(String domainName) {
        // 先从Spring配置加载
        JobDomainConfig.DomainConfig config = jobDomainConfig.getDomainConfig(domainName);
        
        // 如果配置不完整，尝试从外部文件加载
        if (needsExternalConfig(config)) {
            JobDomainConfig.DomainConfig externalConfig = configLoader.loadDomainConfig(domainName);
            if (externalConfig != null) {
                config = mergeDomainConfig(config, externalConfig);
            }
        }
        
        return config;
    }

    /**
     * 检查是否需要加载外部配置
     */
    private boolean needsExternalConfig(JobDomainConfig.DomainConfig config) {
        // 简单判断：如果某些关键配置为空，则需要外部配置
        return config.getEventSources() == null || config.getEventSources().isEmpty() ||
               config.getOutputs() == null || config.getOutputs().isEmpty();
    }

    /**
     * 合并域配置
     */
    private JobDomainConfig.DomainConfig mergeDomainConfig(
            JobDomainConfig.DomainConfig base, 
            JobDomainConfig.DomainConfig external) {
        
        // 简单合并策略：外部配置优先
        if (external.getEventSources() != null) {
            base.setEventSources(external.getEventSources());
        }
        if (external.getDimTables() != null) {
            base.setDimTables(external.getDimTables());
        }
        if (external.getOutputs() != null) {
            base.setOutputs(external.getOutputs());
        }
        
        return base;
    }

    /**
     * 作业域状态枚举
     */
    public enum DomainStatus {
        NOT_FOUND,      // 未找到
        READY,          // 就绪
        RUNNING,        // 运行中
        FAILED,         // 失败
        STOPPED         // 已停止
    }
}
