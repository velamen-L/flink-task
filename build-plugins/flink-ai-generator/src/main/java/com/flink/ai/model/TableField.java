package com.flink.ai.model;

public class TableField {
    private String name;
    private String type;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
    private String references;
    private String comment;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public boolean isPrimaryKey() { return isPrimaryKey; }
    public void setPrimaryKey(boolean primaryKey) { isPrimaryKey = primaryKey; }
    
    public boolean isForeignKey() { return isForeignKey; }
    public void setForeignKey(boolean foreignKey) { isForeignKey = foreignKey; }
    
    public String getReferences() { return references; }
    public void setReferences(String references) { this.references = references; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
