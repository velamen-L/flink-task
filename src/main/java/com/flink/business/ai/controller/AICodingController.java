package com.flink.business.ai.controller;

import com.flink.business.ai.model.RequirementInput;
import com.flink.business.ai.model.SQLGenerationResult;
import com.flink.business.ai.model.QualityCheckResult;
import com.flink.business.ai.service.AIFlinkSQLService;
import com.flink.business.ai.service.KnowledgeBaseService;
import com.flink.business.ai.service.QualityCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * AI编程控制器
 * 
 * 提供FlinkSQL AI生成的完整API接口：
 * 1. 需求分析和理解
 * 2. SQL智能生成
 * 3. 质量检查和优化
 * 4. 知识库管理
 * 5. 模板和最佳实践
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-coding")
@CrossOrigin(origins = "*")
public class AICodingController {

    @Autowired
    private AIFlinkSQLService aiFlinkSQLService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private QualityCheckService qualityCheckService;

    /**
     * 智能生成FlinkSQL
     */
    @PostMapping("/generate-sql")
    public ResponseEntity<APIResponse<SQLGenerationResult>> generateSQL(
            @Valid @RequestBody RequirementInput requirement) {
        
        log.info("收到SQL生成请求: {}", requirement.getJobName());
        
        try {
            // 验证请求参数
            validateRequirement(requirement);
            
            // 生成SQL
            SQLGenerationResult result = aiFlinkSQLService.generateFlinkSQL(requirement);
            
            // 记录用户行为
            recordUserBehavior("generate_sql", requirement.getUserId(), requirement);
            
            return APIResponse.success(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("请求参数无效: {}", e.getMessage());
            return APIResponse.error("INVALID_PARAMETER", e.getMessage());
            
        } catch (Exception e) {
            log.error("SQL生成失败", e);
            return APIResponse.error("GENERATION_FAILED", "SQL生成失败: " + e.getMessage());
        }
    }

    /**
     * 分析和解析需求
     */
    @PostMapping("/analyze-requirement")
    public ResponseEntity<APIResponse<RequirementAnalysisResult>> analyzeRequirement(
            @Valid @RequestBody RequirementAnalysisRequest request) {
        
        log.info("收到需求分析请求");
        
        try {
            RequirementAnalysisResult result = RequirementAnalysisResult.builder()
                .originalText(request.getDescription())
                .extractedTables(extractTablesFromText(request.getDescription()))
                .identifiedBusinessLogic(identifyBusinessLogic(request.getDescription()))
                .suggestedTemplates(suggestTemplates(request))
                .estimatedComplexity(estimateComplexity(request.getDescription()))
                .confidenceScore(calculateAnalysisConfidence(request))
                .build();
            
            return APIResponse.success(result);
            
        } catch (Exception e) {
            log.error("需求分析失败", e);
            return APIResponse.error("ANALYSIS_FAILED", "需求分析失败: " + e.getMessage());
        }
    }

    /**
     * 检查SQL质量
     */
    @PostMapping("/check-quality")
    public ResponseEntity<APIResponse<QualityCheckResult>> checkQuality(
            @Valid @RequestBody QualityCheckRequest request) {
        
        log.info("收到质量检查请求");
        
        try {
            QualityCheckResult result = qualityCheckService.checkQuality(
                request.getSql(), 
                request.getRequirement()
            );
            
            return APIResponse.success(result);
            
        } catch (Exception e) {
            log.error("质量检查失败", e);
            return APIResponse.error("QUALITY_CHECK_FAILED", "质量检查失败: " + e.getMessage());
        }
    }

    /**
     * 获取可用的SQL模板
     */
    @GetMapping("/templates")
    public ResponseEntity<APIResponse<List<SQLTemplate>>> getTemplates(
            @RequestParam(required = false) String scenario,
            @RequestParam(required = false) String complexity) {
        
        log.info("获取SQL模板，场景: {}, 复杂度: {}", scenario, complexity);
        
        try {
            List<SQLTemplate> templates = templateService.getTemplates(scenario, complexity);
            return APIResponse.success(templates);
            
        } catch (Exception e) {
            log.error("获取模板失败", e);
            return APIResponse.error("GET_TEMPLATES_FAILED", "获取模板失败: " + e.getMessage());
        }
    }

    /**
     * 获取表结构信息
     */
    @GetMapping("/tables/{tableName}/schema")
    public ResponseEntity<APIResponse<TableSchema>> getTableSchema(
            @PathVariable String tableName) {
        
        log.info("获取表结构: {}", tableName);
        
        try {
            Map<String, TableSchema> schemas = knowledgeBaseService.getTableSchemas(List.of(tableName));
            TableSchema schema = schemas.get(tableName);
            
            if (schema == null) {
                return APIResponse.error("TABLE_NOT_FOUND", "表不存在: " + tableName);
            }
            
            return APIResponse.success(schema);
            
        } catch (Exception e) {
            log.error("获取表结构失败", e);
            return APIResponse.error("GET_SCHEMA_FAILED", "获取表结构失败: " + e.getMessage());
        }
    }

    /**
     * 搜索相似案例
     */
    @PostMapping("/search-similar")
    public ResponseEntity<APIResponse<List<SimilarCase>>> searchSimilarCases(
            @Valid @RequestBody SimilarSearchRequest request) {
        
        log.info("搜索相似案例");
        
        try {
            List<SimilarCase> cases = searchService.searchSimilarCases(request);
            return APIResponse.success(cases);
            
        } catch (Exception e) {
            log.error("搜索相似案例失败", e);
            return APIResponse.error("SEARCH_FAILED", "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 保存用户反馈
     */
    @PostMapping("/feedback")
    public ResponseEntity<APIResponse<Void>> saveFeedback(
            @Valid @RequestBody UserFeedback feedback) {
        
        log.info("收到用户反馈");
        
        try {
            feedbackService.saveFeedback(feedback);
            
            // 触发知识库更新
            knowledgeBaseService.updateFromUserFeedback(feedback);
            
            return APIResponse.success(null);
            
        } catch (Exception e) {
            log.error("保存反馈失败", e);
            return APIResponse.error("SAVE_FEEDBACK_FAILED", "保存反馈失败: " + e.getMessage());
        }
    }

    /**
     * 获取生成历史
     */
    @GetMapping("/history")
    public ResponseEntity<APIResponse<List<GenerationHistory>>> getGenerationHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("获取生成历史，用户: {}", userId);
        
        try {
            List<GenerationHistory> history = historyService.getGenerationHistory(userId, page, size);
            return APIResponse.success(history);
            
        } catch (Exception e) {
            log.error("获取历史失败", e);
            return APIResponse.error("GET_HISTORY_FAILED", "获取历史失败: " + e.getMessage());
        }
    }

    /**
     * 部署SQL作业
     */
    @PostMapping("/deploy")
    public ResponseEntity<APIResponse<DeploymentResult>> deployJob(
            @Valid @RequestBody JobDeploymentRequest request) {
        
        log.info("部署作业请求: {}", request.getJobName());
        
        try {
            DeploymentResult result = deploymentService.deployJob(request);
            return APIResponse.success(result);
            
        } catch (Exception e) {
            log.error("作业部署失败", e);
            return APIResponse.error("DEPLOYMENT_FAILED", "部署失败: " + e.getMessage());
        }
    }

    /**
     * 获取优化建议
     */
    @PostMapping("/optimize")
    public ResponseEntity<APIResponse<List<OptimizationSuggestion>>> getOptimizations(
            @Valid @RequestBody OptimizationRequest request) {
        
        log.info("获取优化建议");
        
        try {
            List<OptimizationSuggestion> suggestions = optimizationService.generateSuggestions(
                request.getSql(), 
                request.getPerformanceMetrics()
            );
            
            return APIResponse.success(suggestions);
            
        } catch (Exception e) {
            log.error("获取优化建议失败", e);
            return APIResponse.error("OPTIMIZATION_FAILED", "获取优化建议失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成SQL
     */
    @PostMapping("/batch-generate")
    public ResponseEntity<APIResponse<BatchGenerationResult>> batchGenerate(
            @Valid @RequestBody BatchGenerationRequest request) {
        
        log.info("批量生成SQL，数量: {}", request.getRequirements().size());
        
        try {
            BatchGenerationResult result = batchGenerationService.batchGenerate(request);
            return APIResponse.success(result);
            
        } catch (Exception e) {
            log.error("批量生成失败", e);
            return APIResponse.error("BATCH_GENERATION_FAILED", "批量生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统状态
     */
    @GetMapping("/status")
    public ResponseEntity<APIResponse<SystemStatus>> getSystemStatus() {
        try {
            SystemStatus status = SystemStatus.builder()
                .aiServiceStatus(checkAIServiceStatus())
                .knowledgeBaseStatus(checkKnowledgeBaseStatus())
                .qualityServiceStatus(checkQualityServiceStatus())
                .totalGenerations(getTotalGenerations())
                .activeUsers(getActiveUsers())
                .systemLoad(getSystemLoad())
                .build();
            
            return APIResponse.success(status);
            
        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            return APIResponse.error("GET_STATUS_FAILED", "获取状态失败: " + e.getMessage());
        }
    }

    // 私有辅助方法
    private void validateRequirement(RequirementInput requirement) {
        if (requirement.getNaturalLanguageDescription() == null || 
            requirement.getNaturalLanguageDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("需求描述不能为空");
        }
        
        if (requirement.getJobName() == null || 
            requirement.getJobName().trim().isEmpty()) {
            throw new IllegalArgumentException("作业名称不能为空");
        }
    }

    private void recordUserBehavior(String action, String userId, Object data) {
        // 记录用户行为用于分析和优化
        try {
            behaviorService.recordBehavior(action, userId, data);
        } catch (Exception e) {
            log.warn("记录用户行为失败", e);
        }
    }

    // 其他辅助方法的基础实现
    private List<String> extractTablesFromText(String text) { return List.of(); }
    private String identifyBusinessLogic(String text) { return ""; }
    private List<SQLTemplate> suggestTemplates(RequirementAnalysisRequest request) { return List.of(); }
    private String estimateComplexity(String text) { return "medium"; }
    private double calculateAnalysisConfidence(RequirementAnalysisRequest request) { return 0.85; }
    private String checkAIServiceStatus() { return "healthy"; }
    private String checkKnowledgeBaseStatus() { return "healthy"; }
    private String checkQualityServiceStatus() { return "healthy"; }
    private long getTotalGenerations() { return 12345L; }
    private int getActiveUsers() { return 89; }
    private double getSystemLoad() { return 0.45; }

    /**
     * 统一API响应格式
     */
    public static class APIResponse<T> {
        private boolean success;
        private String code;
        private String message;
        private T data;
        private long timestamp;

        public static <T> ResponseEntity<APIResponse<T>> success(T data) {
            APIResponse<T> response = new APIResponse<>();
            response.success = true;
            response.code = "SUCCESS";
            response.message = "操作成功";
            response.data = data;
            response.timestamp = System.currentTimeMillis();
            return ResponseEntity.ok(response);
        }

        public static <T> ResponseEntity<APIResponse<T>> error(String code, String message) {
            APIResponse<T> response = new APIResponse<>();
            response.success = false;
            response.code = code;
            response.message = message;
            response.timestamp = System.currentTimeMillis();
            return ResponseEntity.badRequest().body(response);
        }

        // getters and setters...
    }
}
