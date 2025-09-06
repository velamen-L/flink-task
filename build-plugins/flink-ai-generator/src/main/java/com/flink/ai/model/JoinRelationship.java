package com.flink.ai.model;

public class JoinRelationship {
    private String name;
    private String sourceTable;
    private String sourceField;
    private String targetTable;
    private String targetField;
    private String joinType;
    private String additionalCondition;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    
    public String getSourceField() { return sourceField; }
    public void setSourceField(String sourceField) { this.sourceField = sourceField; }
    
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
    
    public String getTargetField() { return targetField; }
    public void setTargetField(String targetField) { this.targetField = targetField; }
    
    public String getJoinType() { return joinType; }
    public void setJoinType(String joinType) { this.joinType = joinType; }
    
    public String getAdditionalCondition() { return additionalCondition; }
    public void setAdditionalCondition(String additionalCondition) { this.additionalCondition = additionalCondition; }
}
