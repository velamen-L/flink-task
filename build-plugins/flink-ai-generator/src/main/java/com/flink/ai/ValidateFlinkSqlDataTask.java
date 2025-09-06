package com.flink.ai;

import com.flink.ai.service.DataValidationService;
import com.flink.ai.model.ValidationResult;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.io.File;

@CacheableTask
public abstract class ValidateFlinkSqlDataTask extends DefaultTask {
    
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSqlFile();
    
    @OutputDirectory
    public abstract DirectoryProperty getValidationOutputDir();
    
    @TaskAction
    public void validateFlinkSqlData() {
        try {
            File sqlFile = getSqlFile().get().getAsFile();
            File validationOutputDir = getValidationOutputDir().get().getAsFile();
            
            getLogger().lifecycle("Validating Flink SQL data quality: {}", sqlFile.getName());
            
            // 创建验证输出目录
            validationOutputDir.mkdirs();
            
            // 执行数据验证
            DataValidationService service = new DataValidationService();
            ValidationResult result = service.validateSql(sqlFile, validationOutputDir);
            
            // 输出结果
            getLogger().lifecycle("✅ Data validation completed!");
            getLogger().lifecycle("📊 Validation Summary:");
            getLogger().lifecycle("   - Syntax checks: {}", result.getSyntaxChecksPassed() ? "PASSED" : "FAILED");
            getLogger().lifecycle("   - Schema validation: {}", result.getSchemaValidationPassed() ? "PASSED" : "FAILED");
            getLogger().lifecycle("   - Data quality checks: {}/{} passed", 
                    result.getDataQualityChecksPassed(), result.getTotalDataQualityChecks());
            getLogger().lifecycle("   - Performance score: {}/100", result.getPerformanceScore());
            
            if (!result.getWarnings().isEmpty()) {
                getLogger().warn("⚠️  Validation warnings:");
                for (String warning : result.getWarnings()) {
                    getLogger().warn("   - {}", warning);
                }
            }
            
            if (!result.getErrors().isEmpty()) {
                getLogger().error("❌ Validation errors:");
                for (String error : result.getErrors()) {
                    getLogger().error("   - {}", error);
                }
            }
            
            getLogger().lifecycle("📋 Validation report: {}", 
                    new File(validationOutputDir, "validation-report.html").getAbsolutePath());
            
        } catch (Exception e) {
            getLogger().error("❌ Failed to validate Flink SQL data", e);
            throw new TaskExecutionException(this, e);
        }
    }
}
