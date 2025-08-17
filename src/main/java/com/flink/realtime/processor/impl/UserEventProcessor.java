package com.flink.realtime.processor.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.processor.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户事件处理器示例
 * 处理用户相关的业务事件
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class UserEventProcessor implements EventProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(UserEventProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Object process(BusinessEvent event) throws Exception {
        JsonNode payload = event.getPayload();
        Map<String, Object> result = new HashMap<>();
        
        // 根据事件类型进行不同的处理
        switch (event.getType()) {
            case "user_login":
                result = processUserLogin(payload);
                break;
            case "user_purchase":
                result = processUserPurchase(payload);
                break;
            case "user_view":
                result = processUserView(payload);
                break;
            default:
                logger.warn("未支持的用户事件类型: {}", event.getType());
                result.put("raw_data", payload);
        }
        
        // 添加通用字段
        result.put("processed_time", System.currentTimeMillis());
        result.put("processor", this.getClass().getSimpleName());
        
        return result;
    }
    
    @Override
    public String getSupportedEventType() {
        return "user_*"; // 支持所有user_开头的事件
    }
    
    /**
     * 处理用户登录事件
     */
    private Map<String, Object> processUserLogin(JsonNode payload) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("user_id", payload.get("userId").asText());
        result.put("login_time", payload.get("loginTime").asLong());
        result.put("login_device", payload.get("device").asText());
        result.put("login_ip", payload.get("ip").asText());
        
        // 计算登录时长（如果有上次登录时间）
        if (payload.has("lastLoginTime")) {
            long lastLoginTime = payload.get("lastLoginTime").asLong();
            long currentLoginTime = payload.get("loginTime").asLong();
            result.put("time_since_last_login", currentLoginTime - lastLoginTime);
        }
        
        logger.debug("处理用户登录事件: {}", result.get("user_id"));
        return result;
    }
    
    /**
     * 处理用户购买事件
     */
    private Map<String, Object> processUserPurchase(JsonNode payload) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("user_id", payload.get("userId").asText());
        result.put("order_id", payload.get("orderId").asText());
        result.put("product_id", payload.get("productId").asText());
        result.put("amount", payload.get("amount").asDouble());
        result.put("quantity", payload.get("quantity").asInt());
        result.put("purchase_time", payload.get("purchaseTime").asLong());
        
        // 计算总价
        double totalPrice = payload.get("amount").asDouble() * payload.get("quantity").asInt();
        result.put("total_price", totalPrice);
        
        // 根据金额判断购买等级
        String purchaseLevel;
        if (totalPrice >= 1000) {
            purchaseLevel = "HIGH";
        } else if (totalPrice >= 100) {
            purchaseLevel = "MEDIUM";
        } else {
            purchaseLevel = "LOW";
        }
        result.put("purchase_level", purchaseLevel);
        
        logger.debug("处理用户购买事件: {} - {}", result.get("user_id"), totalPrice);
        return result;
    }
    
    /**
     * 处理用户浏览事件
     */
    private Map<String, Object> processUserView(JsonNode payload) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("user_id", payload.get("userId").asText());
        result.put("page_id", payload.get("pageId").asText());
        result.put("view_time", payload.get("viewTime").asLong());
        result.put("duration", payload.get("duration").asLong());
        result.put("referrer", payload.get("referrer").asText(""));
        
        // 根据浏览时长判断用户兴趣度
        long duration = payload.get("duration").asLong();
        String interestLevel;
        if (duration >= 60000) { // 1分钟
            interestLevel = "HIGH";
        } else if (duration >= 10000) { // 10秒
            interestLevel = "MEDIUM";
        } else {
            interestLevel = "LOW";
        }
        result.put("interest_level", interestLevel);
        
        logger.debug("处理用户浏览事件: {} - {}", result.get("user_id"), duration);
        return result;
    }
}
