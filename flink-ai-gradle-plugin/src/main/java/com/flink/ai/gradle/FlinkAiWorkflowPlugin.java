package com.flink.ai.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

/**
 * Flink AI工作流Gradle插件
 * 
 * 提供基于AI的端到端Flink SQL开发工作流，包括：
 * - 智能SQL生成
 * - 全面数据验证
 * - ER知识库管理
 * - 部署配置生成
 */
public class FlinkAiWorkflowPlugin implements Plugin<Project> {
    
    private static final String PLUGIN_ID = "com.flink.ai.workflow";
    private static final String TASK_GROUP = "flink-ai";
    
    @Override
    public void apply(Project project) {
        Logger logger = project.getLogger();
        logger.info("Applying Flink AI Workflow Plugin v1.0.0");
        
        // 创建插件扩展配置
        FlinkAiWorkflowExtension extension = project.getExtensions()
            .create("flinkAiWorkflow", FlinkAiWorkflowExtension.class);
        
        // 注册主要任务
        registerTasks(project, extension, logger);
        
        // 配置任务依赖关系
        configureDependencies(project, logger);
        
        logger.info("Flink AI Workflow Plugin applied successfully");
    }
    
    private void registerTasks(Project project, FlinkAiWorkflowExtension extension, Logger logger) {
        // 1. 端到端工作流任务
        project.getTasks().register("runAiWorkflow", AiWorkflowTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription("执行完整的AI驱动端到端工作流 (SQL生成 → 验证 → ER知识库更新)");
            task.getExtension().set(extension);
        });
        
        // 2. SQL生成任务
        project.getTasks().register("generateFlinkSql", SqlGenerationTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription("基于request文件智能生成Flink SQL");
            task.getExtension().set(extension);
        });
        
        // 3. 数据验证任务
        project.getTasks().register("validateFlinkSql", ValidationTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription("对生成的Flink SQL进行多维度质量验证");
            task.getExtension().set(extension);
        });
        
        // 4. ER知识库管理任务
        project.getTasks().register("updateErKnowledgeBase", ErKnowledgeBaseTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription("更新ER图知识库并检测冲突");
            task.getExtension().set(extension);
        });
        
        // 5. 监控任务（文件变更自动触发）
        project.getTasks().register("watchRequestFiles", WatchTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription("监控request文件变更，自动触发工作流");
            task.getExtension().set(extension);
        });
        
        // 6. 初始化任务
        project.getTasks().register("initAiWorkflow", InitTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription("初始化AI工作流环境和配置");
            task.getExtension().set(extension);
        });
        
        // 7. 生成项目脚手架
        project.getTasks().register("createFlinkDomain", DomainScaffoldTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription("为新业务域创建标准化的项目结构");
            task.getExtension().set(extension);
        });
        
        // 8. 质量报告任务
        project.getTasks().register("generateQualityReport", QualityReportTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription("生成项目整体质量报告");
            task.getExtension().set(extension);
        });
        
        // 9. 知识库一致性检查
        project.getTasks().register("checkKnowledgeBaseConsistency", ConsistencyCheckTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription("检查ER知识库的一致性和完整性");
            task.getExtension().set(extension);
        });
        
        // 10. 部署配置生成
        project.getTasks().register("generateDeploymentConfig", DeploymentConfigTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription("生成Kubernetes部署配置和监控设置");
            task.getExtension().set(extension);
        });
        
        logger.info("Registered {} AI workflow tasks", 10);
    }
    
    private void configureDependencies(Project project, Logger logger) {
        // 配置任务依赖关系
        project.afterEvaluate(proj -> {
            // runAiWorkflow 依赖于其他三个核心任务
            var runAiWorkflow = proj.getTasks().getByName("runAiWorkflow");
            var generateFlinkSql = proj.getTasks().getByName("generateFlinkSql");
            var validateFlinkSql = proj.getTasks().getByName("validateFlinkSql");
            var updateErKnowledgeBase = proj.getTasks().getByName("updateErKnowledgeBase");
            
            // 严格的顺序依赖
            validateFlinkSql.dependsOn(generateFlinkSql);
            updateErKnowledgeBase.dependsOn(validateFlinkSql);
            
            // generateDeploymentConfig 依赖验证通过
            var generateDeploymentConfig = proj.getTasks().getByName("generateDeploymentConfig");
            generateDeploymentConfig.dependsOn(validateFlinkSql);
            
            // 质量报告依赖所有核心任务
            var generateQualityReport = proj.getTasks().getByName("generateQualityReport");
            generateQualityReport.dependsOn(generateFlinkSql, validateFlinkSql, updateErKnowledgeBase);
            
            logger.info("Configured task dependencies for AI workflow");
        });
    }
}
