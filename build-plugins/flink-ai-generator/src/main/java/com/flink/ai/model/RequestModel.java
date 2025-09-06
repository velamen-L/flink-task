package com.flink.ai.model;

import java.util.List;
import java.util.Map;

public class RequestModel {
    private String name;
    private String description;
    private String domain;
    private String eventType;
    private String author;
    private String version;
    
    private SourceTable sourceTable;
    private List<DimensionTable> dimensionTables;
    private ResultTable resultTable;
    private Map<String, String> fieldMapping;
    private List<JoinRelationship> joinRelationships;
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public SourceTable getSourceTable() { return sourceTable; }
    public void setSourceTable(SourceTable sourceTable) { this.sourceTable = sourceTable; }
    
    public List<DimensionTable> getDimensionTables() { return dimensionTables; }
    public void setDimensionTables(List<DimensionTable> dimensionTables) { this.dimensionTables = dimensionTables; }
    
    public ResultTable getResultTable() { return resultTable; }
    public void setResultTable(ResultTable resultTable) { this.resultTable = resultTable; }
    
    public Map<String, String> getFieldMapping() { return fieldMapping; }
    public void setFieldMapping(Map<String, String> fieldMapping) { this.fieldMapping = fieldMapping; }
    
    public List<JoinRelationship> getJoinRelationships() { return joinRelationships; }
    public void setJoinRelationships(List<JoinRelationship> joinRelationships) { this.joinRelationships = joinRelationships; }
}
