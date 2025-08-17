package com.flink.realtime.function;

import com.flink.realtime.bean.ProcessedEvent;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 输出路由处理函数
 * 根据事件的输出配置将数据路由到不同的输出流
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class OutputRoutingProcessFunction extends ProcessFunction<ProcessedEvent, ProcessedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(OutputRoutingProcessFunction.class);
    
    private final OutputTag<ProcessedEvent> alertTag;
    private final OutputTag<ProcessedEvent> metricsTag;
    private final OutputTag<ProcessedEvent> auditTag;
    
    public OutputRoutingProcessFunction(OutputTag<ProcessedEvent> alertTag,
                                      OutputTag<ProcessedEvent> metricsTag,
                                      OutputTag<ProcessedEvent> auditTag) {
        this.alertTag = alertTag;
        this.metricsTag = metricsTag;
        this.auditTag = auditTag;
    }
    
    @Override
    public void processElement(ProcessedEvent event, Context ctx, Collector<ProcessedEvent> out) 
            throws Exception {
        
        // 主流：默认输出到宽表
        boolean hasMainOutput = false;
        
        if (event.getOutputTargets() != null) {
            for (String target : event.getOutputTargets()) {
                switch (target) {
                    case "alert_topic":
                        ctx.output(alertTag, event);
                        logger.debug("事件路由到告警流: {}", event.getOriginalEvent().getEventId());
                        break;
                        
                    case "metrics_topic":
                        ctx.output(metricsTag, event);
                        logger.debug("事件路由到指标流: {}", event.getOriginalEvent().getEventId());
                        break;
                        
                    case "audit_topic":
                        ctx.output(auditTag, event);
                        logger.debug("事件路由到审计流: {}", event.getOriginalEvent().getEventId());
                        break;
                        
                    default:
                        // 默认输出到主流（宽表）
                        if (!hasMainOutput) {
                            out.collect(event);
                            hasMainOutput = true;
                            logger.debug("事件路由到主流: {}", event.getOriginalEvent().getEventId());
                        }
                        break;
                }
            }
        }
        
        // 如果没有指定输出目标，默认输出到主流
        if (!hasMainOutput && (event.getOutputTargets() == null || event.getOutputTargets().isEmpty())) {
            out.collect(event);
        }
    }
}
