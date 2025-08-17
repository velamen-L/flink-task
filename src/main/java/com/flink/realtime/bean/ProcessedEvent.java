package com.flink.realtime.bean;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 处理后的事件数据模型
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class ProcessedEvent implements Serializable {
    
    private BusinessEvent originalEvent;
    private Object processedData;
    private Map<String, Object> outputConfig;
    private List<String> outputTargets;
    private long processTime;
    private String processorClass;
    
    public ProcessedEvent() {}
    
    public ProcessedEvent(BusinessEvent originalEvent, Object processedData, 
                         Map<String, Object> outputConfig) {
        this.originalEvent = originalEvent;
        this.processedData = processedData;
        this.outputConfig = outputConfig;
        this.processTime = System.currentTimeMillis();
        
        // 解析输出目标
        if (outputConfig != null && outputConfig.containsKey("sinks")) {
            this.outputTargets = (List<String>) outputConfig.get("sinks");
        }
    }
    
    // getters and setters
    public BusinessEvent getOriginalEvent() { return originalEvent; }
    public void setOriginalEvent(BusinessEvent originalEvent) { this.originalEvent = originalEvent; }
    
    public Object getProcessedData() { return processedData; }
    public void setProcessedData(Object processedData) { this.processedData = processedData; }
    
    public Map<String, Object> getOutputConfig() { return outputConfig; }
    public void setOutputConfig(Map<String, Object> outputConfig) { this.outputConfig = outputConfig; }
    
    public List<String> getOutputTargets() { return outputTargets; }
    public void setOutputTargets(List<String> outputTargets) { this.outputTargets = outputTargets; }
    
    public long getProcessTime() { return processTime; }
    public void setProcessTime(long processTime) { this.processTime = processTime; }
    
    public String getProcessorClass() { return processorClass; }
    public void setProcessorClass(String processorClass) { this.processorClass = processorClass; }
    
    public boolean shouldOutputTo(String target) {
        return outputTargets != null && outputTargets.contains(target);
    }
    
    @Override
    public String toString() {
        return "ProcessedEvent{" +
                "originalEvent=" + (originalEvent != null ? originalEvent.getEventId() : "null") +
                ", processorClass='" + processorClass + '\'' +
                ", outputTargets=" + outputTargets +
                ", processTime=" + processTime +
                '}';
    }
}
