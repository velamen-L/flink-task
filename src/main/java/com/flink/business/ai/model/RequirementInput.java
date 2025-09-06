package com.flink.business.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户需求输入模型
 * 
 * 支持多种输入方式：
 * 1. 自然语言描述
 * 2. 结构化参数配置
 * 3. 模板选择+参数调整
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementInput {
    
    /**
     * 基础信息
     */
    private String id;
    private String userId;
    private String jobName;
    private String description;
    private RequirementType type;
    private Priority priority;
    private LocalDateTime createTime;
    
    /**
     * 自然语言描述
     */
    private String naturalLanguageDescription;
    
    /**
     * 业务上下文
     */
    private String businessDomain;
    private String businessScenario;
    private List<String> businessGoals;
    
    /**
     * 表信息（用户指定或AI识别）
     */
    private List<String> sourceTableNames;
    private List<String> dimTableNames;
    private String outputTableName;
    
    /**
     * 性能要求
     */
    private PerformanceRequirement performance;
    
    /**
     * 高级配置
     */
    private Map<String, Object> advancedConfig;
    
    /**
     * 模板相关
     */
    private String selectedTemplateId;
    private Map<String, Object> templateParameters;
    
    public enum RequirementType {
        NATURAL_LANGUAGE,      // 自然语言输入
        TEMPLATE_BASED,        // 基于模板
        STRUCTURED_CONFIG,     // 结构化配置
        EXAMPLE_DRIVEN         // 示例驱动
    }
    
    public enum Priority {
        LOW,
        MEDIUM, 
        HIGH,
        URGENT
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceRequirement {
        private Long expectedQps;              // 预期QPS
        private Long maxLatencyMs;             // 最大延迟(毫秒)
        private String resourceConstraint;     // 资源约束(low/medium/high)
        private Boolean needRealTime;          // 是否需要实时处理
        private String slaRequirement;         // SLA要求
    }
}
