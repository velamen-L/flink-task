package com.flink.business.domain.wrongbook;

import com.flink.business.core.config.JobDomainConfig;
import com.flink.business.core.processor.AbstractBusinessProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.table.api.Table;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 错题本作业处理器
 * 
 * 业务场景：
 * - 处理错题本域的订正和收集事件
 * - 关联知识点、教学类型等维表
 * - 生成错题记录宽表数据
 * - 支持复杂的科目转换和业务规则
 * 
 * 处理模式：UNION
 * - 合并wrongbook_fix和wrongbook_collect事件
 * - 统一处理业务逻辑和数据转换
 * - 输出到ODPS宽表
 */
@Slf4j
@Service("wrongbookJobProcessor")
public class WrongbookJobProcessor extends AbstractBusinessProcessor {

    @Override
    protected Table extractAndFilterEvents(JobDomainConfig.EventSourceConfig sourceConfig) {
        String sourceName = sourceConfig.getSourceName();
        List<String> interestedTypes = sourceConfig.getInterestedEventTypes();
        
        // 构建事件过滤条件
        String eventTypeFilter = interestedTypes.stream()
            .map(type -> "'" + type + "'")
            .collect(Collectors.joining(", "));
            
        return tableEnvironment.sqlQuery(String.format("""
            SELECT 
                domain,
                type,
                payload,
                event_time,
                source,
                -- 提取错题本相关字段
                JSON_VALUE(payload, '$.fixId') as fix_id,
                JSON_VALUE(payload, '$.wrongId') as wrong_id,
                JSON_VALUE(payload, '$.userId') as user_id,
                JSON_VALUE(payload, '$.subject') as subject,
                JSON_VALUE(payload, '$.questionId') as question_id,
                JSON_VALUE(payload, '$.patternId') as pattern_id,
                CAST(JSON_VALUE(payload, '$.createTime') AS BIGINT) as create_time,
                CAST(JSON_VALUE(payload, '$.submitTime') AS BIGINT) as submit_time,
                CAST(JSON_VALUE(payload, '$.fixResult') AS INT) as fix_result,
                JSON_VALUE(payload, '$.chapterId') as chapter_id
            FROM %s
            WHERE domain = 'wrongbook'
              AND type IN (%s)
              AND JSON_VALUE(payload, '$.userId') IS NOT NULL
              AND JSON_VALUE(payload, '$.questionId') IS NOT NULL
        """, sourceName, eventTypeFilter));
    }

    @Override
    protected Table unionEventTables(List<Table> eventTables) {
        if (eventTables.size() == 1) {
            return eventTables.get(0);
        }
        
        // 使用UNION ALL合并多个事件表
        Table result = eventTables.get(0);
        for (int i = 1; i < eventTables.size(); i++) {
            result = result.unionAll(eventTables.get(i));
        }
        
        log.info("合并了 {} 个事件表", eventTables.size());
        return result;
    }

    @Override
    protected Table executeCrossDomainJoin(Map<String, Table> domainEvents) {
        // 错题本域是单域处理，不需要跨域JOIN
        throw new UnsupportedOperationException("错题本作业不支持跨域JOIN");
    }

    @Override
    protected Table aggregateByDomain(Table events, String domain) {
        // 错题本域使用UNION模式，不需要聚合
        return events;
    }

    @Override
    protected Table executeCrossDomainAggregation(Map<String, Table> domainAggregations) {
        // 错题本域是单域处理，不需要跨域聚合
        throw new UnsupportedOperationException("错题本作业不支持跨域聚合");
    }

    @Override
    protected Table enrichWithDimTables(Table baseData) {
        // 创建临时视图
        tableEnvironment.createTemporaryView("wrongbook_events", baseData);
        
        return tableEnvironment.sqlQuery("""
            SELECT 
                -- 主键和基础信息
                CAST(w.fix_id AS BIGINT) as id,
                w.wrong_id,
                w.user_id,
                w.subject,
                w.question_id,
                w.pattern_id,
                w.fix_id,
                w.fix_result,
                w.chapter_id,
                w.type as event_type,
                
                -- 维表关联字段
                pt.name as pattern_name,
                CAST(tt.id AS STRING) as teach_type_id,
                tt.teaching_type_name as teach_type_name,
                
                -- 时间字段转换
                TO_TIMESTAMP_LTZ(w.create_time, 3) as collect_time,
                TO_TIMESTAMP_LTZ(w.submit_time, 3) as fix_time,
                w.event_time
                
            FROM wrongbook_events w
            
            -- 关联知识点维表
            LEFT JOIN tower_pattern FOR SYSTEM_TIME AS OF PROCTIME() pt 
                ON pt.id = w.pattern_id
                
            -- 关联教学类型映射表
            LEFT JOIN tower_teaching_type_pt FOR SYSTEM_TIME AS OF PROCTIME() ttp 
                ON ttp.pt_id = w.pattern_id
                AND ttp.is_delete = 0
                
            -- 关联教学类型表
            LEFT JOIN tower_teaching_type FOR SYSTEM_TIME AS OF PROCTIME() tt 
                ON ttp.teaching_type_id = tt.id
                AND tt.is_delete = 0
        """);
    }

    @Override
    protected Table applyBusinessRules(Table enrichedData) {
        // 创建临时视图
        tableEnvironment.createTemporaryView("enriched_wrongbook_data", enrichedData);
        
        return tableEnvironment.sqlQuery("""
            SELECT 
                id,
                wrong_id,
                user_id,
                subject,
                
                -- 科目名称转换业务规则
                CASE subject
                    WHEN 'ENGLISH' THEN '英语'
                    WHEN 'BIOLOGY' THEN '生物'
                    WHEN 'math' THEN '数学'
                    WHEN 'MATH' THEN '数学'
                    WHEN 'PHYSICS' THEN '物理'
                    WHEN 'CHEMISTRY' THEN '化学'
                    WHEN 'AOSHU' THEN '数学思维'
                    WHEN 'SCIENCE' THEN '科学'
                    WHEN 'CHINESE' THEN '语文'
                    ELSE ''
                END as subject_name,
                
                question_id,
                CAST(NULL AS STRING) as question,  -- 题目内容暂时设为空
                pattern_id,
                pattern_name,
                teach_type_id,
                teach_type_name,
                collect_time,
                fix_id,
                fix_time,
                CAST(fix_result AS BIGINT) as fix_result,
                
                -- 订正结果描述转换
                CASE fix_result
                    WHEN 1 THEN '订正'
                    WHEN 0 THEN '未订正'
                    ELSE ''
                END as fix_result_desc,
                
                -- 事件类型标识
                event_type,
                
                -- 处理时间戳（用于监控）
                CURRENT_TIMESTAMP as process_time
                
            FROM enriched_wrongbook_data
            
            -- 应用复杂业务规则：语文英语科目特殊处理
            WHERE (
                subject NOT IN ('CHINESE', 'ENGLISH')
                OR (
                    subject IN ('CHINESE', 'ENGLISH') 
                    AND (
                        teach_type_id IS NOT NULL  -- 确保有教学类型关联
                        OR event_type = 'wrongbook_collect'  -- 收集事件可以没有教学类型
                    )
                )
            )
            -- 数据质量过滤
            AND user_id IS NOT NULL
            AND question_id IS NOT NULL
        """);
    }

    @Override
    protected Table customizeOutputData(Table outputData, JobDomainConfig.OutputConfig outputConfig) {
        // 根据输出配置定制数据
        String targetName = outputConfig.getTargetName();
        
        if ("dwd_wrong_record_wide_delta".equals(targetName)) {
            // 为宽表输出添加额外处理
            tableEnvironment.createTemporaryView("output_data", outputData);
            
            return tableEnvironment.sqlQuery("""
                SELECT 
                    id,
                    wrong_id,
                    user_id,
                    subject,
                    subject_name,
                    question_id,
                    question,
                    pattern_id,
                    pattern_name,
                    teach_type_id,
                    teach_type_name,
                    collect_time,
                    fix_id,
                    fix_time,
                    fix_result,
                    fix_result_desc,
                    
                    -- 添加数据版本和处理时间
                    '2.0' as data_version,
                    process_time,
                    DATE(process_time) as partition_date
                    
                FROM output_data
                WHERE id IS NOT NULL  -- 确保主键不为空
            """);
        }
        
        return outputData;
    }

    /**
     * 错题本域特有的业务指标收集
     */
    @Override
    protected void postExecuteProcess() {
        super.postExecuteProcess();
        
        // 收集错题本特有的业务指标
        collectWrongbookMetrics();
        
        log.info("错题本作业处理器后置处理完成");
    }

    /**
     * 收集错题本业务指标
     */
    private void collectWrongbookMetrics() {
        // 这里可以添加错题本特有的指标收集逻辑
        // 例如：订正成功率、知识点覆盖度、用户活跃度等
        
        try {
            // 统计处理的事件数量
            Table eventStats = tableEnvironment.sqlQuery("""
                SELECT 
                    COUNT(*) as total_events,
                    COUNT(CASE WHEN event_type = 'wrongbook_fix' THEN 1 END) as fix_events,
                    COUNT(CASE WHEN event_type = 'wrongbook_collect' THEN 1 END) as collect_events,
                    COUNT(CASE WHEN fix_result = 1 THEN 1 END) as fix_success_events
                FROM processed_business_data
                WHERE process_time >= CURRENT_TIMESTAMP - INTERVAL '1' HOUR
            """);
            
            // 将统计结果记录到指标系统
            metricsCollector.recordCustomMetrics(domainName, "event_stats", eventStats);
            
        } catch (Exception e) {
            log.warn("收集错题本业务指标失败", e);
        }
    }
}
