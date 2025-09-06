package com.flink.business.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * SQL生成结果模型
 * 
 * 包含完整的生成过程和结果信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SQLGenerationResult {
    
    /**
     * 基础信息
     */
    private String id;
    private String jobId;
    private GenerationStatus status;
    private LocalDateTime generationTime;
    private Long durationMs;
    
    /**
     * 输入和解析
     */
    private RequirementInput originalRequirement;
    private ParsedRequirement parsedRequirement;
    
    /**
     * 生成结果
     */
    private String generatedSQL;
    private String generatedConfig;
    private Map<String, String> additionalFiles;
    
    /**
     * 质量分析
     */
    private QualityCheckResult qualityResult;
    private List<OptimizationSuggestion> optimizations;
    
    /**
     * AI分析信息
     */
    private AIAnalysisInfo aiAnalysis;
    
    /**
     * 部署信息
     */
    private DeploymentInfo deploymentInfo;
    
    public enum GenerationStatus {
        SUCCESS,               // 生成成功
        PARTIAL_SUCCESS,       // 部分成功（有警告）
        FAILED,               // 生成失败
        TIMEOUT,              // 超时
        CANCELLED             // 用户取消
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIAnalysisInfo {
        private Double confidence;             // 整体置信度
        private String aiModel;               // 使用的AI模型
        private List<String> usedTemplates;   // 使用的模板
        private List<String> appliedRules;    // 应用的规则
        private Map<String, Double> decisionConfidence; // 各决策点置信度
        private List<String> uncertainties;   // 不确定的地方
        private String reasoningProcess;      // 推理过程
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeploymentInfo {
        private Boolean deployable;           // 是否可直接部署
        private List<String> prerequisites;   // 部署前置条件
        private Map<String, String> environmentConfig; // 环境配置
        private String deploymentScript;      // 部署脚本
        private List<String> verificationSteps; // 验证步骤
    }
}
