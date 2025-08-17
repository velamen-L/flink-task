package com.flink.realtime.config;

import java.io.Serializable;
import java.util.Map;

/**
 * 动态路由配置模型
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class RoutingConfig implements Serializable {
    
    private String domain;
    private String eventType;
    private String processorClass;
    private Map<String, Object> outputConfig;
    private boolean enabled;
    private int priority;
    private long updateTime;
    
    public RoutingConfig() {}
    
    public RoutingConfig(String domain, String eventType, String processorClass, 
                        Map<String, Object> outputConfig, boolean enabled) {
        this.domain = domain;
        this.eventType = eventType;
        this.processorClass = processorClass;
        this.outputConfig = outputConfig;
        this.enabled = enabled;
        this.priority = 100;
        this.updateTime = System.currentTimeMillis();
    }
    
    // getters and setters
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getProcessorClass() { return processorClass; }
    public void setProcessorClass(String processorClass) { this.processorClass = processorClass; }
    
    public Map<String, Object> getOutputConfig() { return outputConfig; }
    public void setOutputConfig(Map<String, Object> outputConfig) { this.outputConfig = outputConfig; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
    
    public String getRoutingKey() {
        return domain + ":" + eventType;
    }
    
    @Override
    public String toString() {
        return "RoutingConfig{" +
                "domain='" + domain + '\'' +
                ", eventType='" + eventType + '\'' +
                ", processorClass='" + processorClass + '\'' +
                ", enabled=" + enabled +
                ", priority=" + priority +
                '}';
    }
}
