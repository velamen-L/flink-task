package com.flink.business.ai.service;

import com.flink.business.ai.model.RequirementInput;
import com.flink.business.ai.model.ParsedRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 需求解析服务
 * 
 * 核心功能：
 * 1. 自然语言需求理解
 * 2. 业务意图识别
 * 3. 表关系推断
 * 4. 计算逻辑提取
 */
@Slf4j
@Service
public class RequirementParsingService {

    @Autowired
    private LLMService llmService;

    @Autowired
    private TableSchemaService tableSchemaService;

    @Autowired
    private BusinessContextService businessContextService;

    /**
     * 解析用户需求
     */
    public ParsedRequirement parseRequirement(RequirementInput requirement) {
        log.info("开始解析用户需求: {}", requirement.getJobName());

        try {
            // 1. 预处理需求文本
            String preprocessedText = preprocessRequirement(requirement);
            
            // 2. 提取关键信息
            ExtractedInfo extractedInfo = extractKeyInformation(preprocessedText);
            
            // 3. 识别业务意图
            BusinessIntent intent = identifyBusinessIntent(extractedInfo);
            
            // 4. 分析表关系
            TableRelationship tableRel = analyzeTableRelationships(extractedInfo, requirement);
            
            // 5. 解析计算逻辑
            ComputationLogic computation = parseComputationLogic(extractedInfo, intent);
            
            // 6. 确定时间策略
            TimeStrategy timeStrategy = determineTimeStrategy(extractedInfo, intent);
            
            // 7. 构建解析结果
            return ParsedRequirement.builder()
                .originalRequirement(requirement)
                .businessIntent(intent)
                .tableRelationship(tableRel)
                .computationLogic(computation)
                .timeStrategy(timeStrategy)
                .confidence(calculateParsingConfidence(extractedInfo, intent, tableRel))
                .parsingNotes(generateParsingNotes(extractedInfo))
                .build();

        } catch (Exception e) {
            log.error("需求解析失败", e);
            throw new RequirementParsingException("需求解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 预处理需求文本
     */
    private String preprocessRequirement(RequirementInput requirement) {
        String text = requirement.getNaturalLanguageDescription();
        
        // 1. 统一术语
        text = normalizeTerminology(text);
        
        // 2. 提取关键词
        text = highlightKeywords(text);
        
        // 3. 补充上下文
        text = enrichWithContext(text, requirement);
        
        return text;
    }

    /**
     * 提取关键信息
     */
    private ExtractedInfo extractKeyInformation(String text) {
        ExtractedInfo info = new ExtractedInfo();
        
        // 1. 提取表名
        info.setTableNames(extractTableNames(text));
        
        // 2. 提取字段名
        info.setFieldNames(extractFieldNames(text));
        
        // 3. 提取业务动词
        info.setBusinessActions(extractBusinessActions(text));
        
        // 4. 提取时间相关词汇
        info.setTimeExpressions(extractTimeExpressions(text));
        
        // 5. 提取数量和统计词汇
        info.setStatisticalTerms(extractStatisticalTerms(text));
        
        return info;
    }

    /**
     * 识别业务意图
     */
    private BusinessIntent identifyBusinessIntent(ExtractedInfo extractedInfo) {
        // 使用LLM进行意图识别
        String prompt = buildIntentRecognitionPrompt(extractedInfo);
        String aiResponse = llmService.chat(prompt);
        
        return parseBusinessIntentFromResponse(aiResponse);
    }

    /**
     * 分析表关系
     */
    private TableRelationship analyzeTableRelationships(ExtractedInfo extractedInfo, RequirementInput requirement) {
        TableRelationship relationship = new TableRelationship();
        
        // 1. 识别主表
        String mainTable = identifyMainTable(extractedInfo, requirement);
        relationship.setMainTable(mainTable);
        
        // 2. 识别维表
        List<String> dimTables = identifyDimTables(extractedInfo, requirement);
        relationship.setDimTables(dimTables);
        
        // 3. 推断JOIN条件
        Map<String, String> joinConditions = inferJoinConditions(mainTable, dimTables);
        relationship.setJoinConditions(joinConditions);
        
        // 4. 分析JOIN类型
        Map<String, String> joinTypes = determineJoinTypes(mainTable, dimTables, extractedInfo);
        relationship.setJoinTypes(joinTypes);
        
        return relationship;
    }

    /**
     * 解析计算逻辑
     */
    private ComputationLogic parseComputationLogic(ExtractedInfo extractedInfo, BusinessIntent intent) {
        ComputationLogic logic = new ComputationLogic();
        
        // 1. 识别聚合类型
        List<String> aggregations = identifyAggregations(extractedInfo);
        logic.setAggregations(aggregations);
        
        // 2. 识别分组字段
        List<String> groupByFields = identifyGroupByFields(extractedInfo, intent);
        logic.setGroupByFields(groupByFields);
        
        // 3. 识别过滤条件
        List<String> filters = identifyFilterConditions(extractedInfo);
        logic.setFilterConditions(filters);
        
        // 4. 识别计算公式
        List<String> calculations = identifyCalculations(extractedInfo);
        logic.setCalculations(calculations);
        
        return logic;
    }

    /**
     * 确定时间策略
     */
    private TimeStrategy determineTimeStrategy(ExtractedInfo extractedInfo, BusinessIntent intent) {
        TimeStrategy strategy = new TimeStrategy();
        
        // 1. 分析时间粒度
        String timeGranularity = analyzeTimeGranularity(extractedInfo);
        strategy.setTimeGranularity(timeGranularity);
        
        // 2. 确定窗口类型
        String windowType = determineWindowType(extractedInfo, intent);
        strategy.setWindowType(windowType);
        
        // 3. 设置窗口大小
        Long windowSize = calculateWindowSize(timeGranularity);
        strategy.setWindowSize(windowSize);
        
        // 4. 处理迟到数据策略
        String lateDataStrategy = determineLateDataStrategy(intent);
        strategy.setLateDataStrategy(lateDataStrategy);
        
        return strategy;
    }

    /**
     * 构建意图识别提示词
     */
    private String buildIntentRecognitionPrompt(ExtractedInfo extractedInfo) {
        return String.format("""
            请分析以下FlinkSQL需求的业务意图：
            
            提取的表名: %s
            提取的字段: %s
            业务动作: %s
            时间表达: %s
            统计术语: %s
            
            请从以下维度分析：
            1. 主要业务目标 (实时统计/数据宽表/告警监控/数据清洗)
            2. 计算复杂度 (简单/中等/复杂)
            3. 实时性要求 (秒级/分钟级/小时级)
            4. 数据规模 (小/中/大)
            5. 核心业务场景
            
            请以JSON格式返回分析结果。
            """,
            extractedInfo.getTableNames(),
            extractedInfo.getFieldNames(),
            extractedInfo.getBusinessActions(),
            extractedInfo.getTimeExpressions(),
            extractedInfo.getStatisticalTerms()
        );
    }

    // 辅助方法实现
    private String normalizeTerminology(String text) {
        // 统一业务术语，如"用户"/"使用者" -> "用户"
        return text.replaceAll("使用者|客户", "用户")
                  .replaceAll("点击|访问", "操作")
                  .replaceAll("每日|每天", "日")
                  .replaceAll("实时|即时", "实时");
    }

    private String highlightKeywords(String text) {
        // 标记关键词以便后续处理
        Pattern tablePattern = Pattern.compile("\\b\\w+表\\b");
        return tablePattern.matcher(text).replaceAll("<TABLE>$0</TABLE>");
    }

    private String enrichWithContext(String text, RequirementInput requirement) {
        StringBuilder enriched = new StringBuilder(text);
        
        // 添加业务域上下文
        if (requirement.getBusinessDomain() != null) {
            enriched.append(" [业务域: ").append(requirement.getBusinessDomain()).append("]");
        }
        
        // 添加性能上下文
        if (requirement.getPerformance() != null) {
            enriched.append(" [性能要求: QPS=").append(requirement.getPerformance().getExpectedQps()).append("]");
        }
        
        return enriched.toString();
    }

    private List<String> extractTableNames(String text) {
        // 使用正则表达式和NLP技术提取表名
        Pattern pattern = Pattern.compile("(\\w+)[表|table]");
        Matcher matcher = pattern.matcher(text);
        
        List<String> tableNames = new ArrayList<>();
        while (matcher.find()) {
            tableNames.add(matcher.group(1));
        }
        
        return tableNames;
    }

    private List<String> extractFieldNames(String text) {
        // 提取字段名，如"用户ID"、"创建时间"等
        Pattern pattern = Pattern.compile("(用户|时间|数量|金额|状态)\\w*");
        Matcher matcher = pattern.matcher(text);
        
        List<String> fieldNames = new ArrayList<>();
        while (matcher.find()) {
            fieldNames.add(matcher.group());
        }
        
        return fieldNames;
    }

    private List<String> extractBusinessActions(String text) {
        // 提取业务动作词汇
        List<String> actions = new ArrayList<>();
        String[] actionKeywords = {"统计", "计算", "分析", "汇总", "筛选", "关联", "聚合"};
        
        for (String keyword : actionKeywords) {
            if (text.contains(keyword)) {
                actions.add(keyword);
            }
        }
        
        return actions;
    }

    private List<String> extractTimeExpressions(String text) {
        // 提取时间相关表达
        Pattern pattern = Pattern.compile("(每日|每周|每月|实时|分钟|小时|天|周|月)");
        Matcher matcher = pattern.matcher(text);
        
        List<String> timeExpressions = new ArrayList<>();
        while (matcher.find()) {
            timeExpressions.add(matcher.group());
        }
        
        return timeExpressions;
    }

    private List<String> extractStatisticalTerms(String text) {
        // 提取统计术语
        List<String> terms = new ArrayList<>();
        String[] statKeywords = {"总数", "平均", "最大", "最小", "求和", "计数", "比率", "占比"};
        
        for (String keyword : statKeywords) {
            if (text.contains(keyword)) {
                terms.add(keyword);
            }
        }
        
        return terms;
    }

    // 其他方法的基础实现...
    private BusinessIntent parseBusinessIntentFromResponse(String aiResponse) {
        // 解析AI响应，构建BusinessIntent对象
        return new BusinessIntent(); // 实际实现需要JSON解析
    }

    private String identifyMainTable(ExtractedInfo extractedInfo, RequirementInput requirement) {
        // 识别主表逻辑
        return requirement.getSourceTableNames().get(0);
    }

    private List<String> identifyDimTables(ExtractedInfo extractedInfo, RequirementInput requirement) {
        // 识别维表逻辑
        return requirement.getDimTableNames();
    }

    private Map<String, String> inferJoinConditions(String mainTable, List<String> dimTables) {
        // 推断JOIN条件
        return Map.of();
    }

    private Map<String, String> determineJoinTypes(String mainTable, List<String> dimTables, ExtractedInfo extractedInfo) {
        // 确定JOIN类型
        return Map.of();
    }

    private List<String> identifyAggregations(ExtractedInfo extractedInfo) {
        return extractedInfo.getStatisticalTerms();
    }

    private List<String> identifyGroupByFields(ExtractedInfo extractedInfo, BusinessIntent intent) {
        return List.of();
    }

    private List<String> identifyFilterConditions(ExtractedInfo extractedInfo) {
        return List.of();
    }

    private List<String> identifyCalculations(ExtractedInfo extractedInfo) {
        return List.of();
    }

    private String analyzeTimeGranularity(ExtractedInfo extractedInfo) {
        return "daily";
    }

    private String determineWindowType(ExtractedInfo extractedInfo, BusinessIntent intent) {
        return "TUMBLING";
    }

    private Long calculateWindowSize(String timeGranularity) {
        return 86400000L; // 1天
    }

    private String determineLateDataStrategy(BusinessIntent intent) {
        return "IGNORE";
    }

    private Double calculateParsingConfidence(ExtractedInfo extractedInfo, BusinessIntent intent, TableRelationship tableRel) {
        return 0.85;
    }

    private List<String> generateParsingNotes(ExtractedInfo extractedInfo) {
        return List.of();
    }

    // 内部数据类
    private static class ExtractedInfo {
        private List<String> tableNames;
        private List<String> fieldNames;
        private List<String> businessActions;
        private List<String> timeExpressions;
        private List<String> statisticalTerms;
        
        // getters and setters...
    }
}
