package com.flink.realtime.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 事件处理器工厂
 * 根据事件类型返回对应的处理器
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class EventProcessorFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(EventProcessorFactory.class);
    private static final Map<String, EventProcessor> processors = new HashMap<>();
    
    /**
     * 注册事件处理器
     * @param eventType 事件类型
     * @param processor 处理器
     */
    public static void registerProcessor(String eventType, EventProcessor processor) {
        processors.put(eventType, processor);
        logger.info("注册事件处理器: {} -> {}", eventType, processor.getClass().getSimpleName());
    }
    
    /**
     * 获取事件处理器
     * @param eventType 事件类型
     * @return 处理器
     */
    public static EventProcessor getProcessor(String eventType) {
        EventProcessor processor = processors.get(eventType);
        if (processor == null) {
            logger.warn("未找到事件类型 {} 对应的处理器", eventType);
        }
        return processor;
    }
    
    /**
     * 获取所有已注册的处理器
     * @return 处理器映射
     */
    public static Map<String, EventProcessor> getAllProcessors() {
        return new HashMap<>(processors);
    }
}
