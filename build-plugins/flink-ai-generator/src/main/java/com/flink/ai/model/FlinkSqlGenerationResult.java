package com.flink.ai.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FlinkSqlGenerationResult {
    private File sqlFile;
    private File deploymentScript;
    private File jobConfig;
    private int sourceTableCount;
    private int dimensionTableCount;
    private int resultTableCount;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    
    public File getSqlFile() { return sqlFile; }
    public void setSqlFile(File sqlFile) { this.sqlFile = sqlFile; }
    
    public File getDeploymentScript() { return deploymentScript; }
    public void setDeploymentScript(File deploymentScript) { this.deploymentScript = deploymentScript; }
    
    public File getJobConfig() { return jobConfig; }
    public void setJobConfig(File jobConfig) { this.jobConfig = jobConfig; }
    
    public int getSourceTableCount() { return sourceTableCount; }
    public void setSourceTableCount(int sourceTableCount) { this.sourceTableCount = sourceTableCount; }
    
    public int getDimensionTableCount() { return dimensionTableCount; }
    public void setDimensionTableCount(int dimensionTableCount) { this.dimensionTableCount = dimensionTableCount; }
    
    public int getResultTableCount() { return resultTableCount; }
    public void setResultTableCount(int resultTableCount) { this.resultTableCount = resultTableCount; }
    
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    public void addWarning(String warning) { this.warnings.add(warning); }
    public void addError(String error) { this.errors.add(error); }
}
