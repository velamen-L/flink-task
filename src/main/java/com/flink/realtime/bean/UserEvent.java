package com.flink.realtime.bean;

/**
 * 用户事件数据对象（示例）
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class UserEvent extends BaseBean {
    
    private String userId;
    private String eventType;
    private Long timestamp;
    private String data;
    
    public UserEvent() {}
    
    public UserEvent(String userId, String eventType, Long timestamp, String data) {
        this.userId = userId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.data = data;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
}
