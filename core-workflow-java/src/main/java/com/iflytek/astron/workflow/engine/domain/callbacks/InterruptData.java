package com.iflytek.astron.workflow.engine.domain.callbacks;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.HashMap;

/**
 * Data structure for interrupt events.
 * 
 * This class contains information about workflow interruption events,
 * including event identification and context data.
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
public class InterruptData {
    /**
     * Unique identifier for the interrupt event.
     */
    @JsonProperty("event_id")
    private String eventId;
    
    /**
     * Type of the event, defaults to 'interrupt'.
     */
    @JsonProperty("event_type")
    private String eventType = "interrupt";
    
    /**
     * Whether a reply is needed for this interrupt event.
     */
    @JsonProperty("need_reply")
    private boolean needReply = true;
    
    /**
     * Event-specific data and context information.
     */
    private Map<String, Object> value = new HashMap<>();
    
    public InterruptData() {
    }
    
    public InterruptData(String eventId, String eventType, boolean needReply, Map<String, Object> value) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.needReply = needReply;
        this.value = value;
    }
    
    // Getters and setters
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public boolean isNeedReply() {
        return needReply;
    }
    
    public void setNeedReply(boolean needReply) {
        this.needReply = needReply;
    }
    
    public Map<String, Object> getValue() {
        return value;
    }
    
    public void setValue(Map<String, Object> value) {
        this.value = value;
    }
}