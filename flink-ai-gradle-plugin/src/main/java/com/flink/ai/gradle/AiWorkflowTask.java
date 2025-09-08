package com.flink.ai.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;

import com.flink.ai.gradle.core.*;
import com.flink.ai.gradle.model.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 端到端AI工作流任务
 * 
 * 执行完整的三阶段工作流：
 * 1. 阶段1: SQL生成 (45秒)
 * 2. 阶段2: 数据验证 (2.5分钟)  
 * 3. 阶段3: ER知识库更新 (1.3分钟)
 */
public abstract class AiWorkflowTask extends DefaultTask {
    
    @Input
    public abstract Property<FlinkAiWorkflowExtension> getExtension();
    
    @InputDirectory
    @Optional
    public abstract DirectoryProperty getWorkspaceDir();
    
    @InputFile
    @Optional
    public abstract RegularFileProperty getRequestFile();
    
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();
    
    private AiEngineManager aiEngine;
    private WorkflowStateManager stateManager;
    private QualityGateManager qualityGateManager;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    
    @TaskAction
    public void executeWorkflow() {
        Logger logger = getLogger();
        FlinkAiWorkflowExtension ext = getExtension().get();
        
        try {
            logger.info("🚀 Starting AI-driven end-to-end workflow");
            
            // 初始化组件
            initializeComponents(ext, logger);
            
            // 验证配置
            validateConfiguration(ext, logger);
            
            // 发现和处理request文件
            List<RequestFile> requestFiles = discoverRequestFiles(ext, logger);
            
            if (requestFiles.isEmpty()) {
                logger.warn("No request files found in workspace: {}", ext.getWorkspaceDir().get());
                return;
            }
            
            // 执行工作流
            for (RequestFile requestFile : requestFiles) {
                executeWorkflowForDomain(requestFile, ext, logger);
            }
            
            // 生成整体报告
            generateOverallReport(requestFiles, ext, logger);
            
            logger.info("✅ AI workflow completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ AI workflow failed", e);
            throw new RuntimeException("AI workflow execution failed", e);
        } finally {
            cleanup();
        }
    }
    
    private void initializeComponents(FlinkAiWorkflowExtension ext, Logger logger) {
        logger.info("Initializing AI workflow components...");
        
        // 初始化AI引擎
        this.aiEngine = new AiEngineManager(ext, logger);
        
        // 初始化状态管理器
        this.stateManager = new WorkflowStateManager(ext, logger);
        
        // 初始化质量门控管理器
        this.qualityGateManager = new QualityGateManager(ext, logger);
        
        logger.info("Components initialized successfully");
    }
    
    private void validateConfiguration(FlinkAiWorkflowExtension ext, Logger logger) {
        logger.info("Validating workflow configuration...");
        
        try {
            ext.validate();
            
            // 检查必需的目录和文件
            File workspaceDir = new File(ext.getWorkspaceDir().get());
            if (!workspaceDir.exists()) {
                throw new IllegalStateException("Workspace directory does not exist: " + workspaceDir);
            }
            
            File rulesDir = new File(ext.getRulesDir().get());
            if (!rulesDir.exists()) {
                throw new IllegalStateException("Rules directory does not exist: " + rulesDir);
            }
            
            logger.info("Configuration validated successfully");
            
        } catch (Exception e) {
            logger.error("Configuration validation failed", e);
            throw e;
        }
    }
    
    private List<RequestFile> discoverRequestFiles(FlinkAiWorkflowExtension ext, Logger logger) {
        logger.info("Discovering request files in workspace...");
        
        List<RequestFile> requestFiles = new ArrayList<>();
        File workspaceDir = new File(ext.getWorkspaceDir().get());
        
        try {
            Files.walk(workspaceDir.toPath())
                .filter(path -> path.toString().endsWith("-request-v3.md"))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        RequestFile requestFile = RequestFileParser.parse(path.toFile());
                        
                        // 检查是否在排除列表中
                        if (!ext.getExcludedDomains().get().contains(requestFile.getDomain())) {
                            requestFiles.add(requestFile);
                            logger.info("Found request file: {} (domain: {})", 
                                       path.getFileName(), requestFile.getDomain());
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to parse request file: {}, error: {}", 
                                   path, e.getMessage());
                    }
                });
                
        } catch (Exception e) {
            logger.error("Failed to discover request files", e);
            throw new RuntimeException("Request file discovery failed", e);
        }
        
        logger.info("Discovered {} request files", requestFiles.size());
        return requestFiles;
    }
    
    private void executeWorkflowForDomain(RequestFile requestFile, FlinkAiWorkflowExtension ext, Logger logger) {
        String domain = requestFile.getDomain();
        String workflowId = generateWorkflowId(domain);
        
        logger.info("🔄 Executing workflow for domain: {} (workflowId: {})", domain, workflowId);
        
        try {
            // 初始化工作流状态
            WorkflowState state = stateManager.initializeWorkflow(workflowId, domain, requestFile);
            
            // 阶段1: SQL生成
            logger.info("📝 Phase 1: SQL Generation for domain {}", domain);
            long phase1Start = System.currentTimeMillis();
            
            SqlGenerationResult sqlResult = executePhase1(requestFile, ext, logger);
            state.setPhase1Result(sqlResult);
            
            long phase1Duration = System.currentTimeMillis() - phase1Start;
            logger.info("✅ Phase 1 completed in {}ms", phase1Duration);
            
            // 质量门控1
            if (!qualityGateManager.checkPhase1QualityGate(sqlResult, ext)) {
                throw new RuntimeException("Phase 1 quality gate failed for domain: " + domain);
            }
            
            // 阶段2: 数据验证
            logger.info("🔍 Phase 2: Data Validation for domain {}", domain);
            long phase2Start = System.currentTimeMillis();
            
            ValidationResult validationResult = executePhase2(requestFile, sqlResult, ext, logger);
            state.setPhase2Result(validationResult);
            
            long phase2Duration = System.currentTimeMillis() - phase2Start;
            logger.info("✅ Phase 2 completed in {}ms", phase2Duration);
            
            // 质量门控2
            if (!qualityGateManager.checkPhase2QualityGate(validationResult, ext)) {
                throw new RuntimeException("Phase 2 quality gate failed for domain: " + domain);
            }
            
            // 阶段3: ER知识库更新
            logger.info("🗄️ Phase 3: ER Knowledge Base Update for domain {}", domain);
            long phase3Start = System.currentTimeMillis();
            
            KnowledgeBaseResult kbResult = executePhase3(requestFile, validationResult, ext, logger);
            state.setPhase3Result(kbResult);
            
            long phase3Duration = System.currentTimeMillis() - phase3Start;
            logger.info("✅ Phase 3 completed in {}ms", phase3Duration);
            
            // 质量门控3
            if (!qualityGateManager.checkPhase3QualityGate(kbResult, ext)) {
                throw new RuntimeException("Phase 3 quality gate failed for domain: " + domain);
            }
            
            // 完成工作流
            state.markCompleted();
            stateManager.saveState(state);
            
            // 生成执行报告
            generateExecutionReport(state, ext, logger);
            
            logger.info("🎉 Workflow completed successfully for domain: {}", domain);
            
        } catch (Exception e) {
            logger.error("❌ Workflow failed for domain: " + domain, e);
            
            // 保存失败状态
            WorkflowState failedState = stateManager.getState(workflowId);
            if (failedState != null) {
                failedState.markFailed(e.getMessage());
                stateManager.saveState(failedState);
            }
            
            throw new RuntimeException("Workflow failed for domain: " + domain, e);
        }
    }
    
    private SqlGenerationResult executePhase1(RequestFile requestFile, FlinkAiWorkflowExtension ext, Logger logger) {
        try {
            logger.debug("Executing SQL generation for domain: {}", requestFile.getDomain());
            
            // 调用AI引擎进行SQL生成
            SqlGenerationRequest request = SqlGenerationRequest.builder()
                .requestFile(requestFile)
                .outputDir(getOutputDir().get().getAsFile())
                .templateDir(new File(ext.getConfigDir().get() + "/templates"))
                .build();
                
            SqlGenerationResult result = aiEngine.generateSql(request);
            
            logger.debug("SQL generation completed. Generated {} files", 
                        result.getGeneratedFiles().size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Phase 1 (SQL Generation) failed", e);
            throw new RuntimeException("SQL generation failed", e);
        }
    }
    
    private ValidationResult executePhase2(RequestFile requestFile, SqlGenerationResult sqlResult, 
                                         FlinkAiWorkflowExtension ext, Logger logger) {
        try {
            logger.debug("Executing data validation for domain: {}", requestFile.getDomain());
            
            ValidationRequest request = ValidationRequest.builder()
                .requestFile(requestFile)
                .sqlResult(sqlResult)
                .qualityThresholds(createQualityThresholds(ext))
                .outputDir(getOutputDir().get().getAsFile())
                .build();
                
            ValidationResult result = aiEngine.validateData(request);
            
            logger.debug("Data validation completed. Overall score: {}", 
                        result.getOverallScore());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Phase 2 (Data Validation) failed", e);
            throw new RuntimeException("Data validation failed", e);
        }
    }
    
    private KnowledgeBaseResult executePhase3(RequestFile requestFile, ValidationResult validationResult,
                                            FlinkAiWorkflowExtension ext, Logger logger) {
        try {
            logger.debug("Executing ER knowledge base update for domain: {}", requestFile.getDomain());
            
            KnowledgeBaseRequest request = KnowledgeBaseRequest.builder()
                .requestFile(requestFile)
                .validationResult(validationResult)
                .knowledgeBaseDir(new File(ext.getKnowledgeBaseDir().get()))
                .conflictSensitivity(ext.getConflictDetectionSensitivity().get())
                .autoResolveConflicts(ext.getAutoResolveCompatibleConflicts().get())
                .build();
                
            KnowledgeBaseResult result = aiEngine.updateKnowledgeBase(request);
            
            logger.debug("ER knowledge base update completed. Conflicts detected: {}", 
                        result.getConflicts().size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Phase 3 (ER Knowledge Base Update) failed", e);
            throw new RuntimeException("ER knowledge base update failed", e);
        }
    }
    
    private QualityThresholds createQualityThresholds(FlinkAiWorkflowExtension ext) {
        return QualityThresholds.builder()
            .minOverallScore(ext.getMinQualityScore().get())
            .allowWarnings(ext.getAllowWarnings().get())
            .maxCriticalIssues(ext.getCriticalIssuesThreshold().get())
            .sqlStandardnessThreshold(90)
            .dataAccuracyThreshold(95)
            .performanceThreshold(80)
            .businessComplianceThreshold(85)
            .build();
    }
    
    private void generateExecutionReport(WorkflowState state, FlinkAiWorkflowExtension ext, Logger logger) {
        try {
            logger.debug("Generating execution report for workflow: {}", state.getWorkflowId());
            
            ExecutionReportGenerator reportGenerator = new ExecutionReportGenerator(ext, logger);
            File reportFile = reportGenerator.generateReport(state);
            
            logger.info("Execution report generated: {}", reportFile.getAbsolutePath());
            
        } catch (Exception e) {
            logger.warn("Failed to generate execution report", e);
        }
    }
    
    private void generateOverallReport(List<RequestFile> requestFiles, FlinkAiWorkflowExtension ext, Logger logger) {
        try {
            logger.info("Generating overall workflow report...");
            
            OverallReportGenerator reportGenerator = new OverallReportGenerator(ext, logger);
            File reportFile = reportGenerator.generateReport(requestFiles, stateManager.getAllStates());
            
            logger.info("Overall report generated: {}", reportFile.getAbsolutePath());
            
        } catch (Exception e) {
            logger.warn("Failed to generate overall report", e);
        }
    }
    
    private String generateWorkflowId(String domain) {
        return String.format("%s_workflow_%s", 
                           domain, 
                           LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
    }
    
    private void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
