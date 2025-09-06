package com.flink.ai.model;

public class SourceTable extends BaseTable {
    private String payloadClass;
    private String eventFilter;
    
    public String getPayloadClass() { return payloadClass; }
    public void setPayloadClass(String payloadClass) { this.payloadClass = payloadClass; }
    
    public String getEventFilter() { return eventFilter; }
    public void setEventFilter(String eventFilter) { this.eventFilter = eventFilter; }
}
