package com.flink.realtime.topics.wrongbook.sink;

import com.flink.realtime.bean.ProcessedEvent;
import com.flink.realtime.common.UnifiedSinkService;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

/**
 * 错题本MySQL SQL构建器
 * 
 * 专门处理错题本业务的MySQL建表和参数设置逻辑
 * 
 * @author AI代码生成器
 * @date 2024-12-27
 */
public class WrongbookMySQLBuilder implements UnifiedSinkService.MySQLSqlBuilder {
    
    @Override
    public String buildInsertSQL(String tableName) {
        return "INSERT INTO " + tableName + " (" +
                "id, wrong_id, user_id, subject, subject_name, question_id, question, " +
                "pattern_id, pattern_name, teach_type_id, teach_type_name, " +
                "collect_time, fix_id, fix_time, fix_result, fix_result_desc, " +
                "event_id, event_type, process_time" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "subject = VALUES(subject), subject_name = VALUES(subject_name), " +
                "question = VALUES(question), pattern_name = VALUES(pattern_name), " +
                "teach_type_id = VALUES(teach_type_id), teach_type_name = VALUES(teach_type_name), " +
                "fix_id = VALUES(fix_id), fix_time = VALUES(fix_time), " +
                "fix_result = VALUES(fix_result), fix_result_desc = VALUES(fix_result_desc), " +
                "event_id = VALUES(event_id), event_type = VALUES(event_type), " +
                "process_time = VALUES(process_time)";
    }
    
    @Override
    public void setParameters(PreparedStatement ps, ProcessedEvent event) throws SQLException {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) event.getProcessedData();
        
        int index = 1;
        ps.setLong(index++, (Long) data.get("id"));
        ps.setString(index++, (String) data.get("wrong_id"));
        ps.setString(index++, (String) data.get("user_id"));
        ps.setString(index++, (String) data.get("subject"));
        ps.setString(index++, (String) data.get("subject_name"));
        ps.setString(index++, (String) data.get("question_id"));
        ps.setString(index++, (String) data.get("question"));
        ps.setString(index++, (String) data.get("pattern_id"));
        ps.setString(index++, (String) data.get("pattern_name"));
        ps.setString(index++, (String) data.get("teach_type_id"));
        ps.setString(index++, (String) data.get("teach_type_name"));
        
        // 时间字段处理
        setTimestampParameter(ps, index++, data.get("collect_time"));
        ps.setString(index++, (String) data.get("fix_id"));
        setTimestampParameter(ps, index++, data.get("fix_time"));
        
        Object fixResult = data.get("fix_result");
        if (fixResult instanceof Number) {
            ps.setLong(index++, ((Number) fixResult).longValue());
        } else {
            ps.setLong(index++, 0L);
        }
        
        ps.setString(index++, (String) data.get("fix_result_desc"));
        ps.setString(index++, (String) data.get("event_id"));
        ps.setString(index++, (String) data.get("event_type"));
        setTimestampParameter(ps, index++, data.get("process_time"));
    }
    
    private void setTimestampParameter(PreparedStatement ps, int index, Object value) throws SQLException {
        if (value instanceof Timestamp) {
            ps.setTimestamp(index, (Timestamp) value);
        } else if (value instanceof Long) {
            ps.setTimestamp(index, new Timestamp((Long) value));
        } else {
            ps.setTimestamp(index, new Timestamp(System.currentTimeMillis()));
        }
    }
}
