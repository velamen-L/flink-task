package com.flink.ai.gradle;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;

/**
 * Flink AI工作流插件配置扩展
 * 
 * 允许用户在build.gradle中配置插件行为：
 * flinkAiWorkflow {
 *     workspaceDir = 'job'
 *     aiProvider = 'cursor' 
 *     enableWatch = true
 *     // ... 其他配置
 * }
 */
public abstract class FlinkAiWorkflowExtension {
    
    // =============================================================================
    // 基础配置
    // =============================================================================
    
    /**
     * 工作空间目录，默认为 'job'
     */
    public abstract Property<String> getWorkspaceDir();
    
    /**
     * AI提供者，默认为 'cursor'
     * 可选: cursor, openai, azure, custom
     */
    public abstract Property<String> getAiProvider();
    
    /**
     * 规则文件目录，默认为 '.cursor/rules'
     */
    public abstract Property<String> getRulesDir();
    
    /**
     * 配置文件目录，默认为 'job/ai-config'
     */
    public abstract Property<String> getConfigDir();
    
    /**
     * 是否启用文件监控，默认为 false
     */
    public abstract Property<Boolean> getEnableWatch();
    
    /**
     * 是否启用自动部署配置生成，默认为 true
     */
    public abstract Property<Boolean> getEnableDeploymentGeneration();
    
    // =============================================================================
    // AI引擎配置
    // =============================================================================
    
    /**
     * AI模型配置
     */
    public abstract Property<String> getModel();
    
    /**
     * AI API配置
     */
    public abstract MapProperty<String, String> getAiConfig();
    
    /**
     * 工作流超时时间（分钟），默认为 10
     */
    public abstract Property<Integer> getWorkflowTimeoutMinutes();
    
    /**
     * 重试次数，默认为 3
     */
    public abstract Property<Integer> getMaxRetries();
    
    // =============================================================================
    // 质量控制配置
    // =============================================================================
    
    /**
     * 质量门控严格程度: strict, permissive, advisory
     * 默认为 strict
     */
    public abstract Property<String> getQualityGateMode();
    
    /**
     * 最低质量评分阈值，默认为 85
     */
    public abstract Property<Integer> getMinQualityScore();
    
    /**
     * 是否允许Warning级别问题通过，默认为 true
     */
    public abstract Property<Boolean> getAllowWarnings();
    
    /**
     * Critical问题容忍数量，默认为 0
     */
    public abstract Property<Integer> getCriticalIssuesThreshold();
    
    // =============================================================================
    // 业务域配置
    // =============================================================================
    
    /**
     * 默认业务域列表
     */
    public abstract ListProperty<String> getDomains();
    
    /**
     * 排除的业务域
     */
    public abstract ListProperty<String> getExcludedDomains();
    
    /**
     * 业务域特定配置
     */
    public abstract MapProperty<String, String> getDomainConfigs();
    
    // =============================================================================
    // 输出配置
    // =============================================================================
    
    /**
     * 输出目录，默认为 build/ai-workflow
     */
    public abstract Property<String> getOutputDir();
    
    /**
     * 是否生成详细报告，默认为 true
     */
    public abstract Property<Boolean> getGenerateDetailedReports();
    
    /**
     * 报告格式: markdown, html, json
     * 默认为 markdown
     */
    public abstract Property<String> getReportFormat();
    
    /**
     * 是否备份生成的文件，默认为 true
     */
    public abstract Property<Boolean> getEnableBackup();
    
    // =============================================================================
    // ER知识库配置
    // =============================================================================
    
    /**
     * 知识库目录，默认为 'job/knowledge-base'
     */
    public abstract Property<String> getKnowledgeBaseDir();
    
    /**
     * 冲突检测敏感度: low, medium, high
     * 默认为 medium
     */
    public abstract Property<String> getConflictDetectionSensitivity();
    
    /**
     * 是否自动解决兼容性冲突，默认为 true
     */
    public abstract Property<Boolean> getAutoResolveCompatibleConflicts();
    
    /**
     * ER图输出格式: mermaid, plantuml, both
     * 默认为 mermaid
     */
    public abstract Property<String> getErDiagramFormat();
    
    // =============================================================================
    // 高级配置
    // =============================================================================
    
    /**
     * 并行执行配置
     */
    public abstract Property<Boolean> getEnableParallelExecution();
    
    /**
     * 最大并行任务数，默认为 3
     */
    public abstract Property<Integer> getMaxParallelTasks();
    
    /**
     * 缓存配置
     */
    public abstract Property<Boolean> getEnableCache();
    
    /**
     * 缓存有效期（小时），默认为 24
     */
    public abstract Property<Integer> getCacheExpiryHours();
    
    /**
     * 调试模式，默认为 false
     */
    public abstract Property<Boolean> getDebugMode();
    
    /**
     * 日志级别: DEBUG, INFO, WARN, ERROR
     * 默认为 INFO
     */
    public abstract Property<String> getLogLevel();
    
    // =============================================================================
    // 钩子和扩展点
    // =============================================================================
    
    /**
     * 前置处理器类名列表
     */
    public abstract ListProperty<String> getPreProcessors();
    
    /**
     * 后置处理器类名列表  
     */
    public abstract ListProperty<String> getPostProcessors();
    
    /**
     * 自定义验证器类名列表
     */
    public abstract ListProperty<String> getCustomValidators();
    
    /**
     * 自定义生成器类名列表
     */
    public abstract ListProperty<String> getCustomGenerators();
    
    // =============================================================================
    // 默认值初始化
    // =============================================================================
    
    public FlinkAiWorkflowExtension() {
        // 基础配置默认值
        getWorkspaceDir().convention("job");
        getAiProvider().convention("cursor");
        getRulesDir().convention(".cursor/rules");
        getConfigDir().convention("job/ai-config");
        getEnableWatch().convention(false);
        getEnableDeploymentGeneration().convention(true);
        
        // AI引擎默认值
        getModel().convention("gpt-4");
        getWorkflowTimeoutMinutes().convention(10);
        getMaxRetries().convention(3);
        
        // 质量控制默认值
        getQualityGateMode().convention("strict");
        getMinQualityScore().convention(85);
        getAllowWarnings().convention(true);
        getCriticalIssuesThreshold().convention(0);
        
        // 输出配置默认值
        getOutputDir().convention("build/ai-workflow");
        getGenerateDetailedReports().convention(true);
        getReportFormat().convention("markdown");
        getEnableBackup().convention(true);
        
        // ER知识库默认值
        getKnowledgeBaseDir().convention("job/knowledge-base");
        getConflictDetectionSensitivity().convention("medium");
        getAutoResolveCompatibleConflicts().convention(true);
        getErDiagramFormat().convention("mermaid");
        
        // 高级配置默认值
        getEnableParallelExecution().convention(false);
        getMaxParallelTasks().convention(3);
        getEnableCache().convention(true);
        getCacheExpiryHours().convention(24);
        getDebugMode().convention(false);
        getLogLevel().convention("INFO");
    }
    
    // =============================================================================
    // 配置验证方法
    // =============================================================================
    
    /**
     * 验证配置的有效性
     */
    public void validate() {
        // 验证质量评分阈值
        if (getMinQualityScore().get() < 0 || getMinQualityScore().get() > 100) {
            throw new IllegalArgumentException("minQualityScore must be between 0 and 100");
        }
        
        // 验证超时时间
        if (getWorkflowTimeoutMinutes().get() <= 0) {
            throw new IllegalArgumentException("workflowTimeoutMinutes must be positive");
        }
        
        // 验证重试次数
        if (getMaxRetries().get() < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        
        // 验证并行任务数
        if (getMaxParallelTasks().get() <= 0) {
            throw new IllegalArgumentException("maxParallelTasks must be positive");
        }
        
        // 验证质量门控模式
        String qualityMode = getQualityGateMode().get();
        if (!qualityMode.matches("strict|permissive|advisory")) {
            throw new IllegalArgumentException("qualityGateMode must be one of: strict, permissive, advisory");
        }
        
        // 验证AI提供者
        String provider = getAiProvider().get();
        if (!provider.matches("cursor|openai|azure|custom")) {
            throw new IllegalArgumentException("aiProvider must be one of: cursor, openai, azure, custom");
        }
    }
}
