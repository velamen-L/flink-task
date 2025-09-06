package com.flink.business.core.processor;

import com.flink.business.core.config.JobDomainConfig;
import com.flink.business.core.service.EventSourceManager;
import com.flink.business.core.service.MetricsCollector;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * 业务处理器抽象基类
 * 
 * 提供所有业务处理器的通用能力：
 * 1. 事件源注册和管理
 * 2. 维表注册和关联
 * 3. 多源处理模式支持
 * 4. 统一的监控和指标收集
 * 5. 配置驱动的处理流程
 * 
 * 子类只需要实现具体的业务逻辑：
 * - applyBusinessRules() 业务规则应用
 * - customizeFieldMapping() 字段映射定制
 * - handleSpecialCases() 特殊情况处理
 */
@Slf4j
public abstract class AbstractBusinessProcessor {

    @Getter
    @Autowired
    protected StreamTableEnvironment tableEnvironment;

    @Autowired
    protected EventSourceManager eventSourceManager;

    @Autowired
    protected MetricsCollector metricsCollector;

    protected JobDomainConfig.DomainConfig domainConfig;
    protected String domainName;

    /**
     * 初始化处理器
     */
    public void initialize(String domainName, JobDomainConfig.DomainConfig domainConfig) {
        this.domainName = domainName;
        this.domainConfig = domainConfig;
        log.info("初始化业务处理器: {}", domainName);
    }

    /**
     * 执行处理器主流程
     */
    public final void execute() {
        log.info("开始执行业务处理器: {}", domainName);
        
        try {
            // 1. 前置检查
            preExecuteCheck();
            
            // 2. 注册数据源
            registerDataSources();
            
            // 3. 执行业务逻辑
            executeBusinessLogic();
            
            // 4. 配置输出
            configureOutputs();
            
            // 5. 后置处理
            postExecuteProcess();
            
            log.info("业务处理器执行完成: {}", domainName);
            
        } catch (Exception e) {
            log.error("业务处理器执行失败: {}", domainName, e);
            handleExecutionError(e);
            throw e;
        }
    }

    /**
     * 前置检查
     */
    protected void preExecuteCheck() {
        if (domainConfig == null) {
            throw new IllegalStateException("域配置未初始化");
        }
        
        if (domainConfig.getEventSources() == null || domainConfig.getEventSources().isEmpty()) {
            throw new IllegalStateException("事件源配置为空");
        }
        
        log.debug("前置检查通过: {}", domainName);
    }

    /**
     * 注册数据源
     */
    protected void registerDataSources() {
        // 注册事件源
        registerEventSources();
        
        // 注册维表
        registerDimTables();
        
        log.info("数据源注册完成: {}", domainName);
    }

    /**
     * 注册事件源
     */
    protected void registerEventSources() {
        for (JobDomainConfig.EventSourceConfig sourceConfig : domainConfig.getEventSources()) {
            eventSourceManager.registerEventSource(tableEnvironment, sourceConfig);
            log.debug("注册事件源: {}", sourceConfig.getSourceName());
        }
    }

    /**
     * 注册维表
     */
    protected void registerDimTables() {
        if (domainConfig.getDimTables() != null) {
            for (JobDomainConfig.DimTableConfig dimConfig : domainConfig.getDimTables()) {
                eventSourceManager.registerDimTable(tableEnvironment, dimConfig);
                log.debug("注册维表: {}", dimConfig.getTableName());
            }
        }
    }

    /**
     * 执行业务逻辑
     */
    protected void executeBusinessLogic() {
        String processingMode = domainConfig.getProcessingStrategy().getProcessingMode();
        
        switch (processingMode) {
            case "UNION" -> executeUnionProcessing();
            case "JOIN" -> executeJoinProcessing();
            case "AGGREGATE" -> executeAggregateProcessing();
            default -> throw new IllegalArgumentException("不支持的处理模式: " + processingMode);
        }
    }

    /**
     * UNION模式处理
     */
    protected void executeUnionProcessing() {
        log.info("执行UNION模式处理: {}", domainName);
        
        // 提取和过滤事件
        List<Table> eventTables = domainConfig.getEventSources().stream()
            .map(this::extractAndFilterEvents)
            .toList();
            
        // 合并事件数据
        Table unionedEvents = unionEventTables(eventTables);
        
        // 关联维表
        Table enrichedData = enrichWithDimTables(unionedEvents);
        
        // 应用业务规则
        Table processedData = applyBusinessRules(enrichedData);
        
        // 创建临时视图供输出使用
        tableEnvironment.createTemporaryView("processed_business_data", processedData);
    }

    /**
     * JOIN模式处理
     */
    protected void executeJoinProcessing() {
        log.info("执行JOIN模式处理: {}", domainName);
        
        // 提取各域事件数据
        Map<String, Table> domainEvents = domainConfig.getEventSources().stream()
            .collect(java.util.stream.Collectors.toMap(
                JobDomainConfig.EventSourceConfig::getEventDomain,
                this::extractAndFilterEvents
            ));
            
        // 执行跨域JOIN
        Table joinedEvents = executeCrossDomainJoin(domainEvents);
        
        // 关联维表
        Table enrichedData = enrichWithDimTables(joinedEvents);
        
        // 应用业务规则
        Table processedData = applyBusinessRules(enrichedData);
        
        tableEnvironment.createTemporaryView("processed_business_data", processedData);
    }

    /**
     * AGGREGATE模式处理
     */
    protected void executeAggregateProcessing() {
        log.info("执行AGGREGATE模式处理: {}", domainName);
        
        // 分域聚合
        Map<String, Table> domainAggregations = domainConfig.getEventSources().stream()
            .collect(java.util.stream.Collectors.toMap(
                JobDomainConfig.EventSourceConfig::getEventDomain,
                sourceConfig -> {
                    Table events = extractAndFilterEvents(sourceConfig);
                    return aggregateByDomain(events, sourceConfig.getEventDomain());
                }
            ));
            
        // 跨域聚合
        Table crossDomainAggregation = executeCrossDomainAggregation(domainAggregations);
        
        // 关联维表
        Table enrichedData = enrichWithDimTables(crossDomainAggregation);
        
        tableEnvironment.createTemporaryView("processed_business_data", enrichedData);
    }

    /**
     * 配置输出
     */
    protected void configureOutputs() {
        for (JobDomainConfig.OutputConfig outputConfig : domainConfig.getOutputs()) {
            Table outputData = tableEnvironment.from("processed_business_data");
            
            // 可以在这里添加输出前的数据转换
            outputData = customizeOutputData(outputData, outputConfig);
            
            outputData.executeInsert(outputConfig.getTargetName());
            
            log.info("配置输出: {} -> {}", outputConfig.getOutputName(), outputConfig.getTargetName());
        }
    }

    /**
     * 后置处理
     */
    protected void postExecuteProcess() {
        // 注册监控指标
        metricsCollector.recordDomainExecution(domainName);
        
        log.debug("后置处理完成: {}", domainName);
    }

    /**
     * 处理执行错误
     */
    protected void handleExecutionError(Exception e) {
        metricsCollector.recordDomainError(domainName, e);
        log.error("处理器执行错误: {}", domainName, e);
    }

    // ======= 抽象方法，由子类实现 =======

    /**
     * 提取和过滤事件数据
     */
    protected abstract Table extractAndFilterEvents(JobDomainConfig.EventSourceConfig sourceConfig);

    /**
     * 合并事件表
     */
    protected abstract Table unionEventTables(List<Table> eventTables);

    /**
     * 执行跨域JOIN
     */
    protected abstract Table executeCrossDomainJoin(Map<String, Table> domainEvents);

    /**
     * 按域聚合数据
     */
    protected abstract Table aggregateByDomain(Table events, String domain);

    /**
     * 执行跨域聚合
     */
    protected abstract Table executeCrossDomainAggregation(Map<String, Table> domainAggregations);

    /**
     * 关联维表
     */
    protected abstract Table enrichWithDimTables(Table baseData);

    /**
     * 应用业务规则
     */
    protected abstract Table applyBusinessRules(Table enrichedData);

    /**
     * 定制输出数据
     */
    protected Table customizeOutputData(Table outputData, JobDomainConfig.OutputConfig outputConfig) {
        // 默认实现：直接返回原数据
        return outputData;
    }
}
