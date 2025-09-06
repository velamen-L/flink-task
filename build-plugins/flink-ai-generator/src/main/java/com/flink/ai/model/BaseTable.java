package com.flink.ai.model;

import java.util.List;

public abstract class BaseTable {
    protected String name;
    protected String fullName;
    protected List<TableField> fields;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public List<TableField> getFields() { return fields; }
    public void setFields(List<TableField> fields) { this.fields = fields; }
}
