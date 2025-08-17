package com.flink.realtime.processor.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.processor.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 错题本添加事件处理器
 * 处理用户做错题并加入错题本的事件
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class WrongbookAddProcessor implements EventProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(WrongbookAddProcessor.class);
    
    @Override
    public Object process(BusinessEvent event) throws Exception {
        JsonNode payload = event.getPayload();
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 提取错题信息
            String userId = payload.get("userId").asText();
            String questionId = payload.get("questionId").asText();
            int wrongTimes = payload.get("wrongTimes").asInt(1);
            long wrongTime = payload.get("wrongTime").asLong();
            String sessionId = payload.has("sessionId") ? payload.get("sessionId").asText() : null;
            
            // 处理业务逻辑
            result.put("user_id", userId);
            result.put("question_id", questionId);
            result.put("wrong_times", wrongTimes);
            result.put("wrong_time", wrongTime);
            result.put("session_id", sessionId);
            result.put("action", "add_to_wrongbook");
            
            // 计算错题难度评估
            String difficultyAssessment = assessDifficulty(wrongTimes);
            result.put("difficulty_assessment", difficultyAssessment);
            
            // 是否需要推荐复习
            boolean needReview = wrongTimes >= 3;
            result.put("need_review", needReview);
            
            // 添加处理时间
            result.put("processed_time", System.currentTimeMillis());
            result.put("processor", this.getClass().getSimpleName());
            
            logger.debug("错题添加事件处理完成: 用户={}, 题目={}, 错误次数={}", userId, questionId, wrongTimes);
            
        } catch (Exception e) {
            logger.error("错题添加事件处理失败: {}", event, e);
            throw e;
        }
        
        return result;
    }
    
    @Override
    public String getSupportedEventType() {
        return "wrongbook_add";
    }
    
    /**
     * 评估题目难度
     */
    private String assessDifficulty(int wrongTimes) {
        if (wrongTimes >= 5) {
            return "very_hard";
        } else if (wrongTimes >= 3) {
            return "hard";
        } else if (wrongTimes >= 2) {
            return "medium";
        } else {
            return "easy";
        }
    }
}
