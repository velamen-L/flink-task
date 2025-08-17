package com.flink.realtime.function;

import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.bean.ProcessedEvent;
import com.flink.realtime.config.RoutingConfig;
import com.flink.realtime.processor.EventProcessor;
import com.flink.realtime.metrics.AliyunMetricsReporter;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 动态路由处理函数
 * 根据广播的配置动态路由事件到不同的处理器
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class DynamicRoutingProcessFunction 
    extends BroadcastProcessFunction<BusinessEvent, RoutingConfig, ProcessedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicRoutingProcessFunction.class);
    
    private final MapStateDescriptor<String, RoutingConfig> configDescriptor;
    private transient Map<String, EventProcessor> processorCache;
    private transient AliyunMetricsReporter metricsReporter;
    
    public DynamicRoutingProcessFunction(MapStateDescriptor<String, RoutingConfig> configDescriptor) {
        this.configDescriptor = configDescriptor;
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        
        this.processorCache = new ConcurrentHashMap<>();
        
        // 初始化监控指标
        this.metricsReporter = AliyunMetricsReporter.createForAliyunEnvironment(
                getRuntimeContext().getMetricGroup(), "dynamic-routing");
        
        logger.info("动态路由处理函数已初始化");
    }
    
    @Override
    public void processElement(BusinessEvent event, ReadOnlyContext ctx, Collector<ProcessedEvent> out) 
            throws Exception {
        
        long startTime = System.currentTimeMillis();
        String routingKey = event.getDomain() + ":" + event.getType();
        
        try {
            // 从广播状态获取路由配置
            ReadOnlyBroadcastState<String, RoutingConfig> configState = 
                ctx.getBroadcastState(configDescriptor);
            
            RoutingConfig config = configState.get(routingKey);
            if (config == null || !config.isEnabled()) {
                logger.debug("未找到路由配置或已禁用: {}", routingKey);
                metricsReporter.recordEventFailed();
                return;
            }
            
            // 获取或创建处理器
            EventProcessor processor = getOrCreateProcessor(config.getProcessorClass());
            if (processor == null) {
                logger.error("无法创建处理器: {}", config.getProcessorClass());
                metricsReporter.recordEventFailed();
                return;
            }
            
            // 处理事件
            Object result = processor.process(event);
            
            // 构造输出事件
            ProcessedEvent processedEvent = new ProcessedEvent(event, result, config.getOutputConfig());
            processedEvent.setProcessorClass(config.getProcessorClass());
            
            out.collect(processedEvent);
            
            // 记录指标
            metricsReporter.recordEventProcessed(event.toString().length());
            
            long processingTime = System.currentTimeMillis() - startTime;
            logger.debug("事件处理完成: {}, 耗时: {}ms, 处理器: {}", 
                    event.getEventId(), processingTime, config.getProcessorClass());
            
        } catch (Exception e) {
            logger.error("事件处理失败: {}", event, e);
            metricsReporter.recordEventFailed();
            
            // 可以选择发送到死信队列
            sendToDeadLetterQueue(event, e);
        }
    }
    
    @Override
    public void processBroadcastElement(RoutingConfig config, Context ctx, Collector<ProcessedEvent> out) 
            throws Exception {
        
        String routingKey = config.getRoutingKey();
        
        // 更新广播状态
        BroadcastState<String, RoutingConfig> configState = 
            ctx.getBroadcastState(configDescriptor);
        
        if (config.isEnabled()) {
            configState.put(routingKey, config);
            logger.info("更新路由配置: {} -> {}", routingKey, config.getProcessorClass());
        } else {
            configState.remove(routingKey);
            logger.info("删除路由配置: {}", routingKey);
        }
        
        // 清理处理器缓存（强制重新加载）
        processorCache.remove(config.getProcessorClass());
        
        logger.info("路由配置更新完成: {}", config);
    }
    
    /**
     * 获取或创建事件处理器
     */
    private EventProcessor getOrCreateProcessor(String processorClass) {
        return processorCache.computeIfAbsent(processorClass, className -> {
            try {
                Class<?> clazz = Class.forName(className);
                EventProcessor processor = (EventProcessor) clazz.newInstance();
                logger.info("创建新处理器实例: {}", className);
                return processor;
            } catch (Exception e) {
                logger.error("创建处理器失败: {}", className, e);
                return null;
            }
        });
    }
    
    /**
     * 发送到死信队列
     */
    private void sendToDeadLetterQueue(BusinessEvent event, Exception error) {
        try {
            // 这里可以实现发送到死信队列的逻辑
            // 比如发送到特定的Kafka Topic或数据库表
            logger.info("事件已发送到死信队列: {}", event.getEventId());
        } catch (Exception e) {
            logger.error("发送到死信队列失败", e);
        }
    }
}
