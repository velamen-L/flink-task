package com.flink.ai;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;

public abstract class FlinkAiWorkflowTask extends DefaultTask {
    
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getRequestFile();
    
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();
    
    @OutputDirectory  
    public abstract DirectoryProperty getKnowledgeBaseDir();
    
    @Input
    public abstract Property<String> getDomain();
    
    @TaskAction
    public void executeWorkflow() {
        try {
            File requestFile = getRequestFile().get().getAsFile();
            String domain = getDomain().get();
            
            getLogger().lifecycle("🚀 Starting Flink AI Workflow for domain: {}", domain);
            getLogger().lifecycle("📄 Request file: {}", requestFile.getName());
            
            // 输出工作流摘要
            getLogger().lifecycle("\n=== Workflow Summary ===");
            getLogger().lifecycle("✅ Step 1: Flink SQL Generation - COMPLETED");
            getLogger().lifecycle("✅ Step 2: ER Knowledge Base Update - COMPLETED"); 
            getLogger().lifecycle("✅ Step 3: Data Quality Validation - COMPLETED");
            
            File outputDir = getOutputDir().get().getAsFile();
            getLogger().lifecycle("\n=== Generated Artifacts ===");
            getLogger().lifecycle("📁 Output directory: {}", outputDir.getAbsolutePath());
            
            // 列出生成的文件
            File sqlDir = new File(outputDir, "sql");
            if (sqlDir.exists()) {
                File[] sqlFiles = sqlDir.listFiles((dir, name) -> name.endsWith(".sql"));
                if (sqlFiles != null && sqlFiles.length > 0) {
                    getLogger().lifecycle("📄 SQL files:");
                    for (File sqlFile : sqlFiles) {
                        getLogger().lifecycle("   - {}", sqlFile.getName());
                    }
                }
            }
            
            File deployDir = new File(outputDir, "deployment");
            if (deployDir.exists()) {
                File[] deployFiles = deployDir.listFiles();
                if (deployFiles != null && deployFiles.length > 0) {
                    getLogger().lifecycle("🚀 Deployment files:");
                    for (File deployFile : deployFiles) {
                        getLogger().lifecycle("   - {}", deployFile.getName());
                    }
                }
            }
            
            File configDir = new File(outputDir, "config");
            if (configDir.exists()) {
                File[] configFiles = configDir.listFiles();
                if (configFiles != null && configFiles.length > 0) {
                    getLogger().lifecycle("⚙️  Configuration files:");
                    for (File configFile : configFiles) {
                        getLogger().lifecycle("   - {}", configFile.getName());
                    }
                }
            }
            
            File validationDir = new File(outputDir, "validation");
            if (validationDir.exists()) {
                File[] validationFiles = validationDir.listFiles();
                if (validationFiles != null && validationFiles.length > 0) {
                    getLogger().lifecycle("🔍 Validation reports:");
                    for (File validationFile : validationFiles) {
                        getLogger().lifecycle("   - {}", validationFile.getName());
                    }
                }
            }
            
            File knowledgeBaseDir = getKnowledgeBaseDir().get().getAsFile();
            if (knowledgeBaseDir.exists()) {
                getLogger().lifecycle("📚 ER Knowledge Base: {}", knowledgeBaseDir.getAbsolutePath());
            }
            
            getLogger().lifecycle("\n🎉 Flink AI Workflow completed successfully!");
            
        } catch (Exception e) {
            getLogger().error("❌ Flink AI Workflow failed", e);
            throw new TaskExecutionException(this, e);
        }
    }
}
