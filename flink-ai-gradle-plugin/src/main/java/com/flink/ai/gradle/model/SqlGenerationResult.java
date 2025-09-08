package com.flink.ai.gradle.model;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * SQL生成结果模型
 */
public class SqlGenerationResult {
    
    private String domain;
    private File requestFile;
    private File sqlFile;
    private File deploymentFile;
    private File documentationFile;
    private File qualityCheckFile;
    private List<File> generatedFiles;
    private long executionTime;
    private boolean syntaxValid;
    private boolean logicValid;
    private boolean performanceOptimized;
    private List<String> warnings;
    private List<String> suggestions;
    
    public SqlGenerationResult() {
        this.generatedFiles = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.suggestions = new ArrayList<>();
        this.syntaxValid = true;
        this.logicValid = true;
        this.performanceOptimized = true;
    }
    
    // Getters and Setters
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public File getRequestFile() {
        return requestFile;
    }
    
    public void setRequestFile(File requestFile) {
        this.requestFile = requestFile;
    }
    
    public File getSqlFile() {
        return sqlFile;
    }
    
    public void setSqlFile(File sqlFile) {
        this.sqlFile = sqlFile;
    }
    
    public File getDeploymentFile() {
        return deploymentFile;
    }
    
    public void setDeploymentFile(File deploymentFile) {
        this.deploymentFile = deploymentFile;
    }
    
    public File getDocumentationFile() {
        return documentationFile;
    }
    
    public void setDocumentationFile(File documentationFile) {
        this.documentationFile = documentationFile;
    }
    
    public File getQualityCheckFile() {
        return qualityCheckFile;
    }
    
    public void setQualityCheckFile(File qualityCheckFile) {
        this.qualityCheckFile = qualityCheckFile;
    }
    
    public List<File> getGeneratedFiles() {
        return generatedFiles;
    }
    
    public void setGeneratedFiles(List<File> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }
    
    public void addGeneratedFile(File file) {
        if (this.generatedFiles == null) {
            this.generatedFiles = new ArrayList<>();
        }
        this.generatedFiles.add(file);
    }
    
    public long getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }
    
    public boolean isSyntaxValid() {
        return syntaxValid;
    }
    
    public void setSyntaxValid(boolean syntaxValid) {
        this.syntaxValid = syntaxValid;
    }
    
    public boolean isLogicValid() {
        return logicValid;
    }
    
    public void setLogicValid(boolean logicValid) {
        this.logicValid = logicValid;
    }
    
    public boolean isPerformanceOptimized() {
        return performanceOptimized;
    }
    
    public void setPerformanceOptimized(boolean performanceOptimized) {
        this.performanceOptimized = performanceOptimized;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
    
    public List<String> getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
    
    public void addSuggestion(String suggestion) {
        if (this.suggestions == null) {
            this.suggestions = new ArrayList<>();
        }
        this.suggestions.add(suggestion);
    }
    
    /**
     * 检查是否通过基本质量检查
     */
    public boolean passesBasicQualityCheck() {
        return syntaxValid && logicValid && sqlFile != null && sqlFile.exists();
    }
    
    /**
     * 获取生成文件的统计信息
     */
    public String getGenerationSummary() {
        return String.format("Generated %d files in %dms for domain '%s'", 
                           generatedFiles.size(), executionTime, domain);
    }
    
    @Override
    public String toString() {
        return "SqlGenerationResult{" +
                "domain='" + domain + '\'' +
                ", sqlFile=" + sqlFile +
                ", generatedFiles=" + generatedFiles.size() +
                ", executionTime=" + executionTime +
                ", syntaxValid=" + syntaxValid +
                ", logicValid=" + logicValid +
                '}';
    }
}
