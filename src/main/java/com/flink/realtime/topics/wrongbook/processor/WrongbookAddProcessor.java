package com.flink.realtime.topics.wrongbook.processor;

import com.alibaba.fastjson.JSON;
import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.common.DimTableQueryService;
import com.flink.realtime.processor.EventProcessor;
import com.flink.realtime.processor.ProcessorConfig;
import com.flink.realtime.topics.wrongbook.bean.WrongbookPayload;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 错题添加事件处理器
 * 支持维表查询和双输出（MySQL + Kafka）
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
    
    @Override
    public Object process(BusinessEvent event) throws Exception {
        logger.info("处理错题添加事件: {}", event.getEventId());
        
        // 解析payload
        WrongbookPayload.WrongbookAddPayload payload = JSON.parseObject(
            event.getPayload().toString(), 
            WrongbookPayload.WrongbookAddPayload.class
        );
        
        // 构建宽表数据（包含维表查询）
        Map<String, Object> wideTableData = buildWideTableDataWithDims(event, payload);
        
        return wideTableData;
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
