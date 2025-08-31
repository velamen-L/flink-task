package com.flink.realtime.topics.wrongbook.processor;

import com.alibaba.fastjson.JSON;
import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.topics.wrongbook.bean.WrongbookPayload;
import com.flink.realtime.processor.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 错题订正事件处理器
 * 
 * @author AI代码生成器
 * @date 2025-08-29
 */
public class WrongbookFixProcessor implements EventProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(WrongbookFixProcessor.class);
    
    @Override
    public Object process(BusinessEvent event) throws Exception {
        logger.info("处理错题订正事件: {}", event.getEventId());
        
        // 解析payload
        WrongbookPayload.WrongbookFixPayload payload = JSON.parseObject(
            event.getPayload().toString(), 
            WrongbookPayload.WrongbookFixPayload.class
        );
        
        // 构建宽表数据
        Map<String, Object> wideTableData = buildWideTableData(event, payload);
        
        return wideTableData;
    }
    
    @Override
    public String getSupportedEventType() {
        return "wrongbook_fix";
    }
    
    /**
     * 构建宽表数据
     */
    private Map<String, Object> buildWideTableData(
            BusinessEvent event,
            WrongbookPayload.WrongbookFixPayload payload) {
        
        Map<String, Object> wideTableData = new HashMap<>();
        
        // 基础字段
        wideTableData.put("id", payload.getWrongId().hashCode());
        wideTableData.put("wrong_id", payload.getWrongId());
        wideTableData.put("user_id", payload.getUserId());
        wideTableData.put("subject", null); // 将通过维表查询获取
        wideTableData.put("subject_name", null); // 将通过维表查询获取
        wideTableData.put("question_id", payload.getQuestionId());
        wideTableData.put("question", null); // 将通过维表查询获取
        wideTableData.put("pattern_id", payload.getPatternId());
        wideTableData.put("pattern_name", null); // 将通过维表查询获取
        wideTableData.put("teach_type_id", null); // 将通过维表查询获取
        wideTableData.put("teach_type_name", null); // 将通过维表查询获取
        wideTableData.put("collect_time", null); // 将通过维表查询获取
        
        // 错题订正事件特有字段
        wideTableData.put("fix_id", payload.getFixId());
        wideTableData.put("fix_time", new java.sql.Timestamp(payload.getFixTime()));
        wideTableData.put("fix_result", payload.getFixResult());
        wideTableData.put("fix_result_desc", payload.getFixResultDesc());
        wideTableData.put("event_id", event.getEventId());
        wideTableData.put("event_type", event.getType());
        wideTableData.put("process_time", new java.sql.Timestamp(System.currentTimeMillis()));
        
        return wideTableData;
    }
}
