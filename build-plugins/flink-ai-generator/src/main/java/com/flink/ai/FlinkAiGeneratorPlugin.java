package com.flink.ai;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class FlinkAiGeneratorPlugin implements Plugin<Project> {
    
    @Override
    public void apply(Project project) {
        // 创建扩展配置
        FlinkAiGeneratorExtension extension = project.getExtensions()
                .create("flinkAiGenerator", FlinkAiGeneratorExtension.class);
        
        // 注册任务
        project.getTasks().register("generateFlinkSql", GenerateFlinkSqlTask.class, task -> {
            task.setGroup("flink-ai");
            task.setDescription("Generate Flink SQL job from request file");
            task.getRequestFile().convention(extension.getRequestFile());
            task.getOutputDir().convention(extension.getOutputDir());
            task.getKnowledgeBaseDir().convention(extension.getKnowledgeBaseDir());
        });
        
        project.getTasks().register("updateERKnowledgeBase", UpdateERKnowledgeBaseTask.class, task -> {
            task.setGroup("flink-ai");
            task.setDescription("Update ER diagram knowledge base");
            task.getRequestFile().convention(extension.getRequestFile());
            task.getKnowledgeBaseDir().convention(extension.getKnowledgeBaseDir());
        });
        
        project.getTasks().register("validateFlinkSqlData", ValidateFlinkSqlDataTask.class, task -> {
            task.setGroup("flink-ai");
            task.setDescription("Validate generated Flink SQL data quality");
            task.getSqlFile().convention(extension.getOutputDir().map(dir -> 
                dir.file("sql/" + extension.getDomain().get() + "_wide_table.sql")));
            task.getValidationOutputDir().convention(extension.getOutputDir().map(dir ->
                dir.dir("validation")));
        });
        
        project.getTasks().register("flinkAiWorkflow", FlinkAiWorkflowTask.class, task -> {
            task.setGroup("flink-ai");
            task.setDescription("Execute complete Flink AI workflow: SQL generation + ER update + validation");
            task.getRequestFile().convention(extension.getRequestFile());
            task.getOutputDir().convention(extension.getOutputDir());
            task.getKnowledgeBaseDir().convention(extension.getKnowledgeBaseDir());
            task.getDomain().convention(extension.getDomain());
            
            // 设置任务依赖
            task.dependsOn("generateFlinkSql", "updateERKnowledgeBase", "validateFlinkSqlData");
        });
        
        // 设置默认值
        extension.getRequestFile().convention(
            project.getLayout().getProjectDirectory().file("job/request.md"));
        extension.getOutputDir().convention(
            project.getLayout().getBuildDirectory().dir("flink-ai-output"));
        extension.getKnowledgeBaseDir().convention(
            project.getLayout().getProjectDirectory().dir("er-knowledge-base"));
        extension.getDomain().convention("default");
    }
}
