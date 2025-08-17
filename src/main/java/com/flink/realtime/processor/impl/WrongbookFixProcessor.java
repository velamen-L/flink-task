package com.flink.realtime.processor.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.processor.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 错题订正事件处理器
 * 处理用户订正错题的事件
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class WrongbookFixProcessor implements EventProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(WrongbookFixProcessor.class);
    
    @Override
    public Object process(BusinessEvent event) throws Exception {
        JsonNode payload = event.getPayload();
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 提取订正信息
            String userId = payload.get("userId").asText();
            String questionId = payload.get("questionId").asText();
            long fixTime = payload.get("fixTime").asLong();
            String fixResult = payload.get("fixResult").asText(); // correct/incorrect
            int attempts = payload.has("attempts") ? payload.get("attempts").asInt() : 1;
            long timeCost = payload.has("timeCost") ? payload.get("timeCost").asLong() : 0;
            
            // 处理业务逻辑
            result.put("user_id", userId);
            result.put("question_id", questionId);
            result.put("fix_time", fixTime);
            result.put("fix_result", fixResult);
            result.put("attempts", attempts);
            result.put("time_cost", timeCost);
            result.put("action", "fix_wrongbook");
            
            // 计算订正效果评估
            String fixEffectiveness = assessFixEffectiveness(fixResult, attempts, timeCost);
            result.put("fix_effectiveness", fixEffectiveness);
            
            // 判断是否掌握
            boolean isMastered = "correct".equals(fixResult) && attempts <= 2;
            result.put("is_mastered", isMastered);
            
            // 推荐下次复习时间（小时）
            int nextReviewHours = calculateNextReviewTime(fixResult, attempts);
            result.put("next_review_hours", nextReviewHours);
            
            // 学习效果分析
            Map<String, Object> learningAnalysis = analyzeLearningEffect(fixResult, attempts, timeCost);
            result.put("learning_analysis", learningAnalysis);
            
            // 添加处理时间
            result.put("processed_time", System.currentTimeMillis());
            result.put("processor", this.getClass().getSimpleName());
            
            logger.debug("错题订正事件处理完成: 用户={}, 题目={}, 结果={}, 尝试次数={}", 
                    userId, questionId, fixResult, attempts);
            
        } catch (Exception e) {
            logger.error("错题订正事件处理失败: {}", event, e);
            throw e;
        }
        
        return result;
    }
    
    @Override
    public String getSupportedEventType() {
        return "wrongbook_fix";
    }
    
    /**
     * 评估订正效果
     */
    private String assessFixEffectiveness(String fixResult, int attempts, long timeCost) {
        if ("correct".equals(fixResult)) {
            if (attempts == 1 && timeCost < 30000) { // 30秒内一次正确
                return "excellent";
            } else if (attempts <= 2 && timeCost < 60000) { // 1分钟内2次内正确
                return "good";
            } else {
                return "fair";
            }
        } else {
            return "poor";
        }
    }
    
    /**
     * 计算下次复习时间
     */
    private int calculateNextReviewTime(String fixResult, int attempts) {
        if ("correct".equals(fixResult)) {
            switch (attempts) {
                case 1: return 24;    // 1天后
                case 2: return 12;    // 12小时后
                default: return 6;    // 6小时后
            }
        } else {
            return 2; // 2小时后重新复习
        }
    }
    
    /**
     * 分析学习效果
     */
    private Map<String, Object> analyzeLearningEffect(String fixResult, int attempts, long timeCost) {
        Map<String, Object> analysis = new HashMap<>();
        
        // 正确率
        analysis.put("success_rate", "correct".equals(fixResult) ? 1.0 : 0.0);
        
        // 效率评分 (0-100)
        int efficiencyScore;
        if ("correct".equals(fixResult)) {
            if (attempts == 1 && timeCost < 30000) {
                efficiencyScore = 100;
            } else if (attempts <= 2 && timeCost < 60000) {
                efficiencyScore = 80;
            } else {
                efficiencyScore = 60;
            }
        } else {
            efficiencyScore = Math.max(20, 60 - attempts * 10);
        }
        analysis.put("efficiency_score", efficiencyScore);
        
        // 学习建议
        String suggestion;
        if ("correct".equals(fixResult) && attempts == 1) {
            suggestion = "掌握良好，继续保持";
        } else if ("correct".equals(fixResult)) {
            suggestion = "基本掌握，建议多练习类似题目";
        } else {
            suggestion = "需要加强练习，建议寻求帮助";
        }
        analysis.put("suggestion", suggestion);
        
        return analysis;
    }
}
