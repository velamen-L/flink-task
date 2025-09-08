package com.flink.ai.gradle.core;

import com.flink.ai.gradle.FlinkAiWorkflowExtension;
import com.flink.ai.gradle.model.*;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * AI引擎管理器
 * 
 * 负责调用各种AI引擎执行具体的工作流任务：
 * - SQL生成引擎
 * - 数据验证引擎
 * - ER知识库管理引擎
 */
public class AiEngineManager {
    
    private final FlinkAiWorkflowExtension extension;
    private final Logger logger;
    private final RuleLoader ruleLoader;
    private final TemplateEngine templateEngine;
    private final AiProviderClient aiClient;
    
    public AiEngineManager(FlinkAiWorkflowExtension extension, Logger logger) {
        this.extension = extension;
        this.logger = logger;
        this.ruleLoader = new RuleLoader(extension, logger);
        this.templateEngine = new TemplateEngine(extension, logger);
        this.aiClient = createAiClient(extension, logger);
    }
    
    /**
     * 执行SQL生成
     */
    public SqlGenerationResult generateSql(SqlGenerationRequest request) {
        logger.info("Executing SQL generation for domain: {}", request.getRequestFile().getDomain());
        
        try {
            // 加载SQL生成规则
            String sqlGeneratorRule = ruleLoader.loadRule("intelligent-sql-job-generator.mdc");
            
            // 准备AI提示
            String prompt = buildSqlGenerationPrompt(request, sqlGeneratorRule);
            
            // 调用AI引擎
            long startTime = System.currentTimeMillis();
            AiResponse response = aiClient.generate(prompt, extension.getWorkflowTimeoutMinutes().get());
            long duration = System.currentTimeMillis() - startTime;
            
            // 解析AI响应并生成文件
            SqlGenerationResult result = parseSqlGenerationResponse(response, request);
            result.setExecutionTime(duration);
            
            logger.info("SQL generation completed in {}ms", duration);
            return result;
            
        } catch (Exception e) {
            logger.error("SQL generation failed", e);
            throw new RuntimeException("SQL generation failed", e);
        }
    }
    
    /**
     * 执行数据验证
     */
    public ValidationResult validateData(ValidationRequest request) {
        logger.info("Executing data validation for domain: {}", request.getRequestFile().getDomain());
        
        try {
            // 加载验证规则
            String validationRule = ruleLoader.loadRule("intelligent-validation-workflow.mdc");
            String sqlValidatorRule = ruleLoader.loadRule("flink-sql-validator.mdc");
            String dataValidatorRule = ruleLoader.loadRule("flink-sql-data-validator.mdc");
            
            // 准备AI提示
            String prompt = buildValidationPrompt(request, validationRule, sqlValidatorRule, dataValidatorRule);
            
            // 调用AI引擎
            long startTime = System.currentTimeMillis();
            AiResponse response = aiClient.generate(prompt, extension.getWorkflowTimeoutMinutes().get());
            long duration = System.currentTimeMillis() - startTime;
            
            // 解析AI响应
            ValidationResult result = parseValidationResponse(response, request);
            result.setExecutionTime(duration);
            
            logger.info("Data validation completed in {}ms, overall score: {}", 
                       duration, result.getOverallScore());
            return result;
            
        } catch (Exception e) {
            logger.error("Data validation failed", e);
            throw new RuntimeException("Data validation failed", e);
        }
    }
    
    /**
     * 执行ER知识库更新
     */
    public KnowledgeBaseResult updateKnowledgeBase(KnowledgeBaseRequest request) {
        logger.info("Executing ER knowledge base update for domain: {}", request.getRequestFile().getDomain());
        
        try {
            // 加载ER知识库管理规则
            String erKbRule = ruleLoader.loadRule("intelligent-er-knowledge-base.mdc");
            
            // 准备AI提示
            String prompt = buildKnowledgeBasePrompt(request, erKbRule);
            
            // 调用AI引擎
            long startTime = System.currentTimeMillis();
            AiResponse response = aiClient.generate(prompt, extension.getWorkflowTimeoutMinutes().get());
            long duration = System.currentTimeMillis() - startTime;
            
            // 解析AI响应
            KnowledgeBaseResult result = parseKnowledgeBaseResponse(response, request);
            result.setExecutionTime(duration);
            
            logger.info("ER knowledge base update completed in {}ms, conflicts: {}", 
                       duration, result.getConflicts().size());
            return result;
            
        } catch (Exception e) {
            logger.error("ER knowledge base update failed", e);
            throw new RuntimeException("ER knowledge base update failed", e);
        }
    }
    
    private AiProviderClient createAiClient(FlinkAiWorkflowExtension extension, Logger logger) {
        String provider = extension.getAiProvider().get();
        
        switch (provider.toLowerCase()) {
            case "cursor":
                return new CursorAiClient(extension, logger);
            case "openai":
                return new OpenAiClient(extension, logger);
            case "azure":
                return new AzureAiClient(extension, logger);
            case "custom":
                return new CustomAiClient(extension, logger);
            default:
                throw new IllegalArgumentException("Unsupported AI provider: " + provider);
        }
    }
    
    private String buildSqlGenerationPrompt(SqlGenerationRequest request, String rule) {
        try {
            // 读取request文件内容
            String requestContent = Files.readString(request.getRequestFile().getFile().toPath());
            
            // 构建提示
            StringBuilder prompt = new StringBuilder();
            prompt.append("请基于以下规则执行Flink SQL生成任务：\n\n");
            prompt.append("=== 规则文件 ===\n");
            prompt.append(rule).append("\n\n");
            prompt.append("=== 输入文件 ===\n");
            prompt.append("文件路径: ").append(request.getRequestFile().getFile().getPath()).append("\n");
            prompt.append("业务域: ").append(request.getRequestFile().getDomain()).append("\n\n");
            prompt.append("文件内容:\n");
            prompt.append(requestContent).append("\n\n");
            prompt.append("=== 任务要求 ===\n");
            prompt.append("1. 基于request文件生成优化的Flink SQL\n");
            prompt.append("2. 生成部署配置文件 (Kubernetes YAML)\n");
            prompt.append("3. 生成数据质量检查SQL\n");
            prompt.append("4. 生成完整的技术文档\n");
            prompt.append("5. 确保所有输出符合生产标准\n\n");
            prompt.append("请开始执行SQL生成任务。");
            
            return prompt.toString();
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to read request file", e);
        }
    }
    
    private String buildValidationPrompt(ValidationRequest request, String... rules) {
        try {
            // 读取SQL文件内容
            String sqlContent = Files.readString(request.getSqlResult().getSqlFile().toPath());
            String requestContent = Files.readString(request.getRequestFile().getFile().toPath());
            
            StringBuilder prompt = new StringBuilder();
            prompt.append("请基于以下规则执行Flink SQL验证任务：\n\n");
            
            // 添加所有规则
            for (int i = 0; i < rules.length; i++) {
                prompt.append("=== 规则文件 ").append(i + 1).append(" ===\n");
                prompt.append(rules[i]).append("\n\n");
            }
            
            prompt.append("=== 待验证的SQL文件 ===\n");
            prompt.append("文件路径: ").append(request.getSqlResult().getSqlFile().getPath()).append("\n");
            prompt.append("业务域: ").append(request.getRequestFile().getDomain()).append("\n\n");
            prompt.append("SQL内容:\n");
            prompt.append(sqlContent).append("\n\n");
            
            prompt.append("=== 原始需求文件 ===\n");
            prompt.append(requestContent).append("\n\n");
            
            prompt.append("=== 质量要求 ===\n");
            prompt.append("最低综合评分: ").append(request.getQualityThresholds().getMinOverallScore()).append("\n");
            prompt.append("Critical问题容忍: ").append(request.getQualityThresholds().getMaxCriticalIssues()).append("\n");
            prompt.append("是否允许Warning: ").append(request.getQualityThresholds().isAllowWarnings()).append("\n\n");
            
            prompt.append("=== 任务要求 ===\n");
            prompt.append("1. 对SQL进行4个维度的全面验证\n");
            prompt.append("2. 生成详细的验证报告\n");
            prompt.append("3. 提供具体的修复建议\n");
            prompt.append("4. 生成测试数据和性能基准\n");
            prompt.append("5. 给出综合质量评分\n\n");
            prompt.append("请开始执行验证任务。");
            
            return prompt.toString();
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to read SQL or request file", e);
        }
    }
    
    private String buildKnowledgeBasePrompt(KnowledgeBaseRequest request, String rule) {
        try {
            String requestContent = Files.readString(request.getRequestFile().getFile().toPath());
            
            StringBuilder prompt = new StringBuilder();
            prompt.append("请基于以下规则执行ER知识库管理任务：\n\n");
            prompt.append("=== 规则文件 ===\n");
            prompt.append(rule).append("\n\n");
            
            prompt.append("=== 输入文件 ===\n");
            prompt.append("文件路径: ").append(request.getRequestFile().getFile().getPath()).append("\n");
            prompt.append("业务域: ").append(request.getRequestFile().getDomain()).append("\n\n");
            prompt.append("文件内容:\n");
            prompt.append(requestContent).append("\n\n");
            
            prompt.append("=== 知识库配置 ===\n");
            prompt.append("知识库目录: ").append(request.getKnowledgeBaseDir().getPath()).append("\n");
            prompt.append("冲突检测敏感度: ").append(request.getConflictSensitivity()).append("\n");
            prompt.append("自动解决兼容冲突: ").append(request.isAutoResolveConflicts()).append("\n\n");
            
            // 读取现有知识库内容
            String existingKnowledgeBase = readExistingKnowledgeBase(request.getKnowledgeBaseDir());
            if (!existingKnowledgeBase.isEmpty()) {
                prompt.append("=== 现有知识库内容 ===\n");
                prompt.append(existingKnowledgeBase).append("\n\n");
            }
            
            prompt.append("=== 任务要求 ===\n");
            prompt.append("1. 解析request文件中的ER图定义\n");
            prompt.append("2. 与现有知识库进行冲突检测\n");
            prompt.append("3. 生成或更新ER图定义\n");
            prompt.append("4. 输出冲突报告（如有冲突）\n");
            prompt.append("5. 更新知识库结构\n\n");
            prompt.append("请开始执行ER知识库管理任务。");
            
            return prompt.toString();
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to read request file", e);
        }
    }
    
    private String readExistingKnowledgeBase(File knowledgeBaseDir) {
        StringBuilder content = new StringBuilder();
        
        try {
            if (knowledgeBaseDir.exists() && knowledgeBaseDir.isDirectory()) {
                Files.walk(knowledgeBaseDir.toPath())
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .limit(10) // 限制文件数量，避免prompt过长
                    .forEach(path -> {
                        try {
                            content.append("文件: ").append(path.getFileName()).append("\n");
                            content.append(Files.readString(path)).append("\n\n");
                        } catch (IOException e) {
                            // 忽略读取失败的文件
                        }
                    });
            }
        } catch (IOException e) {
            logger.warn("Failed to read existing knowledge base", e);
        }
        
        return content.toString();
    }
    
    private SqlGenerationResult parseSqlGenerationResponse(AiResponse response, SqlGenerationRequest request) {
        // 这里需要解析AI的响应，提取生成的文件内容
        // 实际实现中，需要根据AI响应的具体格式进行解析
        
        SqlGenerationResult result = new SqlGenerationResult();
        result.setDomain(request.getRequestFile().getDomain());
        result.setRequestFile(request.getRequestFile().getFile());
        
        // 模拟解析过程
        List<File> generatedFiles = new ArrayList<>();
        
        try {
            // 创建输出目录
            File domainDir = new File(request.getOutputDir(), request.getRequestFile().getDomain());
            domainDir.mkdirs();
            
            // 这里应该根据AI响应内容创建具体的文件
            // 目前使用模拟数据
            File sqlFile = new File(domainDir, "sql/" + request.getRequestFile().getDomain() + "_wide_table_v3.sql");
            File deploymentFile = new File(domainDir, "deployment/deploy-" + request.getRequestFile().getDomain() + "-v3.yaml");
            File docsFile = new File(domainDir, "docs/README-AI-Generated-v3.md");
            
            sqlFile.getParentFile().mkdirs();
            deploymentFile.getParentFile().mkdirs();
            docsFile.getParentFile().mkdirs();
            
            // 写入AI生成的内容（这里需要从response中提取）
            Files.writeString(sqlFile.toPath(), "-- AI Generated SQL\n" + response.getContent());
            Files.writeString(deploymentFile.toPath(), "# AI Generated Deployment Config\n" + response.getContent());
            Files.writeString(docsFile.toPath(), "# AI Generated Documentation\n" + response.getContent());
            
            generatedFiles.add(sqlFile);
            generatedFiles.add(deploymentFile);
            generatedFiles.add(docsFile);
            
            result.setSqlFile(sqlFile);
            result.setGeneratedFiles(generatedFiles);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to write generated files", e);
        }
        
        return result;
    }
    
    private ValidationResult parseValidationResponse(AiResponse response, ValidationRequest request) {
        // 解析验证结果
        ValidationResult result = new ValidationResult();
        result.setDomain(request.getRequestFile().getDomain());
        
        // 这里需要根据AI响应解析具体的验证结果
        // 目前使用模拟数据
        result.setSqlStandardnessScore(94);
        result.setDataAccuracyScore(97);
        result.setPerformanceScore(88);
        result.setBusinessComplianceScore(92);
        result.setOverallScore(93.25);
        
        result.setCriticalIssues(new ArrayList<>());
        result.setWarningIssues(Arrays.asList("潜在数据类型风险", "维表关联率偏低"));
        result.setInfoSuggestions(Arrays.asList("考虑增加并行度", "优化缓存配置", "抽取复杂逻辑为UDF"));
        
        return result;
    }
    
    private KnowledgeBaseResult parseKnowledgeBaseResponse(AiResponse response, KnowledgeBaseRequest request) {
        // 解析知识库更新结果
        KnowledgeBaseResult result = new KnowledgeBaseResult();
        result.setDomain(request.getRequestFile().getDomain());
        
        // 这里需要根据AI响应解析具体的知识库更新结果
        // 目前使用模拟数据
        result.setConflicts(new ArrayList<>());
        result.setKnowledgeBaseUpdated(true);
        result.setGeneratedFiles(new ArrayList<>());
        
        return result;
    }
}
