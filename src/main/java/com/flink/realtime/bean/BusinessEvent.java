package com.flink.realtime.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 业务事件统一数据模型
 * 格式：业务域统一topic + 内部子type + payload数据
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class BusinessEvent extends BaseBean {
    
    /**
     * 业务域
     */
    @JsonProperty("domain")
    private String domain;
    
    /**
     * 事件类型
     */
    @JsonProperty("type")
    private String type;
    
    /**
     * 事件时间戳
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    /**
     * 事件ID
     */
    @JsonProperty("eventId")
    private String eventId;
    
    /**
     * 载荷数据
     */
    @JsonProperty("payload")
    private JsonNode payload;
    
    /**
     * 数据版本
     */
    @JsonProperty("version")
    private String version;
    
    /**
     * 来源系统
     */
    @JsonProperty("source")
    private String source;
    
    public BusinessEvent() {}
    
    public BusinessEvent(String domain, String type, Long timestamp, String eventId, JsonNode payload) {
        this.domain = domain;
        this.type = type;
        this.timestamp = timestamp;
        this.eventId = eventId;
        this.payload = payload;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public JsonNode getPayload() {
        return payload;
    }
    
    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    @Override
    public String toString() {
        return "BusinessEvent{" +
                "domain='" + domain + '\'' +
                ", type='" + type + '\'' +
                ", timestamp=" + timestamp +
                ", eventId='" + eventId + '\'' +
                ", payload=" + payload +
                ", version='" + version + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
