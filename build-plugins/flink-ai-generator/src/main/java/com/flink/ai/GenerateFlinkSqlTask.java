package com.flink.ai;

import com.flink.ai.service.FlinkSqlGeneratorService;
import com.flink.ai.model.FlinkSqlGenerationResult;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.io.File;

@CacheableTask
public abstract class GenerateFlinkSqlTask extends DefaultTask {
    
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getRequestFile();
    
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();
    
    @OutputDirectory
    public abstract DirectoryProperty getKnowledgeBaseDir();
    
    @TaskAction
    public void generateFlinkSql() {
        try {
            File requestFile = getRequestFile().get().getAsFile();
            File outputDir = getOutputDir().get().getAsFile();
            
            getLogger().lifecycle("Generating Flink SQL from: {}", requestFile.getAbsolutePath());
            
            // 创建输出目录
            outputDir.mkdirs();
            
            // 执行Flink SQL生成
            FlinkSqlGeneratorService service = new FlinkSqlGeneratorService();
            FlinkSqlGenerationResult result = service.generateFlinkSql(requestFile, outputDir);
            
            // 输出结果
            getLogger().lifecycle("✅ Flink SQL generated successfully!");
            getLogger().lifecycle("📄 SQL file: {}", result.getSqlFile().getAbsolutePath());
            getLogger().lifecycle("📊 Tables processed: {} source, {} dimension, {} result", 
                    result.getSourceTableCount(), 
                    result.getDimensionTableCount(), 
                    result.getResultTableCount());
            
            if (!result.getWarnings().isEmpty()) {
                getLogger().warn("⚠️  Warnings:");
                for (String warning : result.getWarnings()) {
                    getLogger().warn("   - {}", warning);
                }
            }
            
        } catch (Exception e) {
            getLogger().error("❌ Failed to generate Flink SQL", e);
            throw new TaskExecutionException(this, e);
        }
    }
}
