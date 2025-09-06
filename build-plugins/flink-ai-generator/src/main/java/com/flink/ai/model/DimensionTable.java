package com.flink.ai.model;

public class DimensionTable extends BaseTable {
    private String filterCondition;
    
    public String getFilterCondition() { return filterCondition; }
    public void setFilterCondition(String filterCondition) { this.filterCondition = filterCondition; }
}
