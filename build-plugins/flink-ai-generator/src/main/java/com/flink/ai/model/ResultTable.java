package com.flink.ai.model;

public class ResultTable extends BaseTable {
    private String operation = "INSERT";
    private String primaryKey;
    
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    
    public String getPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(String primaryKey) { this.primaryKey = primaryKey; }
}
