package com.flink.ai.gradle.model;

import java.io.File;
import java.util.Map;
import java.util.List;

/**
 * Request文件模型
 * 
 * 表示一个业务需求描述文件 (domain-request-v3.md)
 */
public class RequestFile {
    
    private File file;
    private String domain;
    private String name;
    private String eventType;
    private String description;
    private Map<String, Object> jobInfo;
    private Map<String, String> fieldMapping;
    private Map<String, Object> joinRelationships;
    private String erDiagram;
    private Map<String, Object> specialConditions;
    
    // 构造函数
    public RequestFile() {}
    
    public RequestFile(File file, String domain) {
        this.file = file;
        this.domain = domain;
    }
    
    // Getters and Setters
    public File getFile() {
        return file;
    }
    
    public void setFile(File file) {
        this.file = file;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Map<String, Object> getJobInfo() {
        return jobInfo;
    }
    
    public void setJobInfo(Map<String, Object> jobInfo) {
        this.jobInfo = jobInfo;
    }
    
    public Map<String, String> getFieldMapping() {
        return fieldMapping;
    }
    
    public void setFieldMapping(Map<String, String> fieldMapping) {
        this.fieldMapping = fieldMapping;
    }
    
    public Map<String, Object> getJoinRelationships() {
        return joinRelationships;
    }
    
    public void setJoinRelationships(Map<String, Object> joinRelationships) {
        this.joinRelationships = joinRelationships;
    }
    
    public String getErDiagram() {
        return erDiagram;
    }
    
    public void setErDiagram(String erDiagram) {
        this.erDiagram = erDiagram;
    }
    
    public Map<String, Object> getSpecialConditions() {
        return specialConditions;
    }
    
    public void setSpecialConditions(Map<String, Object> specialConditions) {
        this.specialConditions = specialConditions;
    }
    
    @Override
    public String toString() {
        return "RequestFile{" +
                "file=" + file +
                ", domain='" + domain + '\'' +
                ", name='" + name + '\'' +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}
