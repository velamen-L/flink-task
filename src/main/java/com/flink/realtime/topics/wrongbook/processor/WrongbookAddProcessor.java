package com.flink.realtime.topics.wrongbook.processor;

import com.alibaba.fastjson.JSON;
import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.bean.ProcessedEvent;
import com.flink.realtime.topics.wrongbook.bean.WrongbookPayload;
import com.flink.realtime.processor.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashMap;
import java.util.Map;

/**
 * 错题添加事件处理器
 * 
 * @author AI代码生成器
 * @date 2025-08-29
 */
public class WrongbookAddProcessor implements EventProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(WrongbookAddProcessor.class);
    
    @Override
    public Object process(BusinessEvent event) throws Exception {
        logger.info("处理错题添加事件: {}", event.getEventId());
        
        // 解析payload
        WrongbookPayload.WrongbookAddPayload payload = JSON.parseObject(
            event.getPayload().toString(), 
            WrongbookPayload.WrongbookAddPayload.class
        );
        
        // 构建宽表数据
        Map<String, Object> wideTableData = buildWideTableData(event, payload);
        
        return wideTableData;
    }
    
    @Override
    public String getSupportedEventType() {
        return "wrongbook_add";
    }
    

    
    /**
     * 构建宽表数据
     */
    private Map<String, Object> buildWideTableData(
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
        wideTableData.put("pattern_name", null); // 将通过维表查询获取
        wideTableData.put("teach_type_id", null); // 将通过维表查询获取
        wideTableData.put("teach_type_name", null); // 将通过维表查询获取
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
