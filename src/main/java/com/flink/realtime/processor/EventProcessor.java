package com.flink.realtime.processor;

import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.bean.ProcessedEvent;
import org.apache.flink.util.Collector;

/**
 * 事件处理器接口
 * 根据不同的事件类型，实现不同的处理逻辑和输出决策
 * 
 * 职责：
 * 1. 业务逻辑处理
 * 2. 输出目标决策（MySQL/Kafka/其他）
 * 3. 数据输出执行
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public interface EventProcessor {
    
    /**
     * 处理业务事件并决定输出
     * 
     * @param event 业务事件
     * @param collector 数据收集器（用于输出到下游）
     * @throws Exception 处理异常
     */
    void process(BusinessEvent event, Collector<ProcessedEvent> collector) throws Exception;
    
    /**
     * 获取支持的事件类型
     * @return 事件类型
     */
    String getSupportedEventType();
}
