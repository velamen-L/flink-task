package com.flink.realtime.processor;

import com.flink.realtime.bean.BusinessEvent;

/**
 * 事件处理器接口
 * 根据不同的事件类型，实现不同的处理逻辑
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public interface EventProcessor {
    
    /**
     * 处理业务事件
     * @param event 业务事件
     * @return 处理结果
     */
    Object process(BusinessEvent event) throws Exception;
    
    /**
     * 获取支持的事件类型
     * @return 事件类型
     */
    String getSupportedEventType();
}
