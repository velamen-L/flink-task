package com.flink.ai.model;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean syntaxChecksPassed;
    private boolean schemaValidationPassed;
    private int dataQualityChecksPassed;
    private int totalDataQualityChecks;
    private int performanceScore;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    
    public boolean getSyntaxChecksPassed() { return syntaxChecksPassed; }
    public void setSyntaxChecksPassed(boolean syntaxChecksPassed) { this.syntaxChecksPassed = syntaxChecksPassed; }
    
    public boolean getSchemaValidationPassed() { return schemaValidationPassed; }
    public void setSchemaValidationPassed(boolean schemaValidationPassed) { this.schemaValidationPassed = schemaValidationPassed; }
    
    public int getDataQualityChecksPassed() { return dataQualityChecksPassed; }
    public void setDataQualityChecksPassed(int dataQualityChecksPassed) { this.dataQualityChecksPassed = dataQualityChecksPassed; }
    
    public int getTotalDataQualityChecks() { return totalDataQualityChecks; }
    public void setTotalDataQualityChecks(int totalDataQualityChecks) { this.totalDataQualityChecks = totalDataQualityChecks; }
    
    public int getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(int performanceScore) { this.performanceScore = performanceScore; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
