package com.flink.realtime.topics.wrongbook.processor;

import com.alibaba.fastjson.JSON;
import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.bean.ProcessedEvent;
import com.flink.realtime.common.DimTableQueryService;
import com.flink.realtime.common.UnifiedSinkService;
import com.flink.realtime.topics.wrongbook.sink.WrongbookMySQLBuilder;
import com.flink.realtime.processor.EventProcessor;
import com.flink.realtime.processor.ProcessorConfig;
import com.flink.realtime.topics.wrongbook.bean.WrongbookPayload;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 错题添加事件处理器
 * 
 * 职责：
 * 1. 处理错题添加业务逻辑
 * 2. 查询维表数据并丰富信息
 * 3. 决定输出目标（MySQL宽表 + Kafka下游）
 * 4. 执行数据输出
 * 
 * @author AI代码生成器
 * @date 2025-08-29
 */
@ProcessorConfig(
    eventTypes = {"wrongbook:wrongbook_add"},
    description = "错题添加事件处理器",
    priority = 1
)
public class WrongbookAddProcessor implements EventProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(WrongbookAddProcessor.class);
    
    private final DimTableQueryService dimService = DimTableQueryService.getInstance();
    private final UnifiedSinkService sinkService = UnifiedSinkService.getInstance();
    
    // 缓存Sink实例
    private SinkFunction<ProcessedEvent> mysqlSink;
    private KafkaSink<ProcessedEvent> kafkaSink;
    
    @Override
    public void process(BusinessEvent event, Collector<ProcessedEvent> collector) throws Exception {
        logger.info("处理错题添加事件: {}", event.getEventId());
        
        try {
            // 1. 解析payload
            WrongbookPayload.WrongbookAddPayload payload = JSON.parseObject(
                event.getPayload().toString(), 
                WrongbookPayload.WrongbookAddPayload.class
            );
            
            // 2. 构建宽表数据（包含维表查询）
            Map<String, Object> wideTableData = buildWideTableDataWithDims(event, payload);
            
            // 3. 构造处理结果
            ProcessedEvent processedEvent = new ProcessedEvent(event, wideTableData, null);
            processedEvent.setProcessorClass(this.getClass().getName());
            
            // 4. 输出到MySQL宽表
            outputToMySQL(processedEvent);
            
            // 5. 输出到Kafka下游（如果需要）
            if (shouldOutputToKafka(payload)) {
                outputToKafka(processedEvent);
            }
            
            // 6. 收集事件（用于监控）
            collector.collect(processedEvent);
            
            logger.info("错题添加事件处理完成: eventId={}, wrongId={}", 
                    event.getEventId(), payload.getWrongId());
            
        } catch (Exception e) {
            logger.error("处理错题添加事件失败: eventId={}", event.getEventId(), e);
            throw e;
        }
    }
    
    @Override
    public String getSupportedEventType() {
        return "wrongbook_add";
    }
    
    /**
     * 构建宽表数据（包含维表查询）
     */
    private Map<String, Object> buildWideTableDataWithDims(
            BusinessEvent event,
            WrongbookPayload.WrongbookAddPayload payload) {
        
        Map<String, Object> wideTableData = new HashMap<>();
        
        // 基础字段
        wideTableData.put("id", payload.getWrongId().hashCode());
        wideTableData.put("wrong_id", payload.getWrongId());
        wideTableData.put("user_id", payload.getUserId());
        wideTableData.put("subject", payload.getSubject());
        wideTableData.put("subject_name", getSubjectName(payload.getSubject()));
        wideTableData.put("question_id", payload.getQuestionId());
        wideTableData.put("question", payload.getQuestion());
        wideTableData.put("pattern_id", payload.getPatternId());
        
        // 查询维表信息
        enrichWithDimensions(wideTableData, payload);
        
        wideTableData.put("collect_time", new java.sql.Timestamp(payload.getCreateTime()));
        wideTableData.put("fix_id", null);
        wideTableData.put("fix_time", null);
        wideTableData.put("fix_result", null);
        wideTableData.put("fix_result_desc", null);
        wideTableData.put("event_id", event.getEventId());
        wideTableData.put("event_type", event.getType());
        wideTableData.put("process_time", new java.sql.Timestamp(System.currentTimeMillis()));
        
        return wideTableData;
    }
    
    /**
     * 通过维表查询丰富数据
     */
    private void enrichWithDimensions(Map<String, Object> wideTableData, 
                                    WrongbookPayload.WrongbookAddPayload payload) {
        
        try {
            // 查询知识点信息
            if (payload.getPatternId() != null) {
                Map<String, Object> patternInfo = dimService.queryWithCache(
                    "pattern_info", 
                    payload.getPatternId(),
                    "SELECT pattern_id, pattern_name, teach_type_id, teach_type_name FROM pattern_info WHERE pattern_id = ?",
                    payload.getPatternId()
                );
                
                if (patternInfo != null) {
                    wideTableData.put("pattern_name", patternInfo.get("pattern_name"));
                    wideTableData.put("teach_type_id", patternInfo.get("teach_type_id"));
                    wideTableData.put("teach_type_name", patternInfo.get("teach_type_name"));
                } else {
                    logger.warn("未找到知识点信息: pattern_id={}", payload.getPatternId());
                }
            }
            
            // 查询题目详细信息（如果payload中没有）
            if (payload.getQuestionId() != null && 
                (payload.getQuestion() == null || payload.getQuestion().isEmpty())) {
                
                Map<String, Object> questionInfo = dimService.queryWithCache(
                    "question_info",
                    payload.getQuestionId(), 
                    "SELECT question_id, question, answer, analysis, difficulty FROM question_info WHERE question_id = ?",
                    payload.getQuestionId()
                );
                
                if (questionInfo != null) {
                    wideTableData.put("question", questionInfo.get("question"));
                    wideTableData.put("answer", questionInfo.get("answer"));
                    wideTableData.put("analysis", questionInfo.get("analysis"));
                    wideTableData.put("difficulty", questionInfo.get("difficulty"));
                } else {
                    logger.warn("未找到题目信息: question_id={}", payload.getQuestionId());
                }
            }
            
        } catch (Exception e) {
            logger.error("维表查询失败: payload={}", payload, e);
            // 维表查询失败不影响主流程，使用默认值
            wideTableData.putIfAbsent("pattern_name", null);
            wideTableData.putIfAbsent("teach_type_id", null);
            wideTableData.putIfAbsent("teach_type_name", null);
        }
    }
    
    /**
     * 输出到MySQL宽表
     */
    private void outputToMySQL(ProcessedEvent processedEvent) {
        try {
            if (mysqlSink == null) {
                mysqlSink = sinkService.createMySQLSink(
                    "dwd_wrong_record_wide_delta",
                    new WrongbookMySQLBuilder()
                );
            }
            
            // 注意：这里是示例，实际需要通过Flink的addSink方式
            // 在真实环境中，Sink应该在作业启动时配置
            logger.debug("准备输出到MySQL: eventId={}", processedEvent.getOriginalEvent().getEventId());
            
        } catch (Exception e) {
            logger.error("MySQL输出失败: eventId={}", 
                    processedEvent.getOriginalEvent().getEventId(), e);
        }
    }
    
    /**
     * 输出到Kafka下游
     */
    private void outputToKafka(ProcessedEvent processedEvent) {
        try {
            if (kafkaSink == null) {
                kafkaSink = sinkService.createKafkaSink("wrongbook-wide-table-output");
            }
            
            // 注意：这里是示例，实际需要通过Flink的addSink方式
            logger.debug("准备输出到Kafka: eventId={}", processedEvent.getOriginalEvent().getEventId());
            
        } catch (Exception e) {
            logger.error("Kafka输出失败: eventId={}", 
                    processedEvent.getOriginalEvent().getEventId(), e);
        }
    }
    
    /**
     * 判断是否需要输出到Kafka
     */
    private boolean shouldOutputToKafka(WrongbookPayload.WrongbookAddPayload payload) {
        // 业务规则：重要错题需要输出到下游
        return payload.getResult() != null && payload.getResult() == 0; // 假设0表示重要错题
    }
    
    /**
     * 获取科目名称
     */
    private String getSubjectName(String subject) {
        if (subject == null) return null;
        switch (subject.toLowerCase()) {
            case "math":
            case "shuxue":
                return "数学";
            case "chinese":
            case "yuwen":
                return "语文";
            case "english":
            case "yingyu":
                return "英语";
            case "physics":
            case "wuli":
                return "物理";
            case "chemistry":
            case "huaxue":
                return "化学";
            default:
                return subject;
        }
    }
}