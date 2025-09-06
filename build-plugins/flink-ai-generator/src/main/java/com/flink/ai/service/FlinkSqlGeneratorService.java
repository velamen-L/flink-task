package com.flink.ai.service;

import com.flink.ai.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class FlinkSqlGeneratorService {
    
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    public FlinkSqlGenerationResult generateFlinkSql(File requestFile, File outputDir) throws IOException {
        // 解析请求文件
        RequestModel request = parseRequestFile(requestFile);
        
        // 生成Flink SQL
        String sql = generateSql(request);
        
        // 保存SQL文件
        File sqlDir = new File(outputDir, "sql");
        sqlDir.mkdirs();
        File sqlFile = new File(sqlDir, request.getDomain() + "_wide_table.sql");
        Files.write(sqlFile.toPath(), sql.getBytes());
        
        // 生成部署脚本
        generateDeploymentScript(request, outputDir);
        
        // 生成作业配置文件
        generateJobConfig(request, outputDir);
        
        // 收集结果
        FlinkSqlGenerationResult result = new FlinkSqlGenerationResult();
        result.setSqlFile(sqlFile);
        result.setSourceTableCount(1); // BusinessEvent
        result.setDimensionTableCount(request.getDimensionTables().size());
        result.setResultTableCount(1);
        
        return result;
    }
    
    private RequestModel parseRequestFile(File requestFile) throws IOException {
        String content = new String(Files.readAllBytes(requestFile.toPath()));
        RequestModel request = new RequestModel();
        
        // 解析基本信息
        parseJobInfo(content, request);
        
        // 解析源表配置
        parseSourceTable(content, request);
        
        // 解析维表配置
        parseDimensionTables(content, request);
        
        // 解析结果表配置
        parseResultTable(content, request);
        
        // 解析字段映射
        parseFieldMapping(content, request);
        
        // 解析关联关系
        parseJoinRelationships(content, request);
        
        return request;
    }
    
    private void parseJobInfo(String content, RequestModel request) {
        Pattern pattern = Pattern.compile("job_info:(.*?)^##", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            String jobInfoText = matcher.group(1);
            
            request.setName(extractYamlValue(jobInfoText, "name"));
            request.setDescription(extractYamlValue(jobInfoText, "description"));
            request.setDomain(extractYamlValue(jobInfoText, "domain"));
            request.setEventType(extractYamlValue(jobInfoText, "event_type"));
            request.setAuthor(extractYamlValue(jobInfoText, "author"));
            request.setVersion(extractYamlValue(jobInfoText, "version"));
        }
    }
    
    private void parseSourceTable(String content, RequestModel request) {
        // 解析Payload结构
        Pattern payloadPattern = Pattern.compile("public class (\\w+)\\s*\\{([^}]+)\\}", Pattern.DOTALL);
        Matcher matcher = payloadPattern.matcher(content);
        
        if (matcher.find()) {
            String payloadClass = matcher.group(1);
            String payloadFields = matcher.group(2);
            
            SourceTable sourceTable = new SourceTable();
            sourceTable.setPayloadClass(payloadClass);
            sourceTable.setEventFilter("domain = '" + request.getDomain() + "' AND type = '" + request.getDomain() + "_" + request.getEventType() + "'");
            
            // 解析字段
            parsePayloadFields(payloadFields, sourceTable);
            
            request.setSourceTable(sourceTable);
        }
    }
    
    private void parseDimensionTables(String content, RequestModel request) {
        Pattern tablePattern = Pattern.compile("### 维表\\d+: (\\w+).*?CREATE TABLE[^`]*`([^`]+)`[^(]*\\(([^)]+WITH[^)]+)\\)", 
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        
        Matcher matcher = tablePattern.matcher(content);
        List<DimensionTable> dimensionTables = new ArrayList<>();
        
        while (matcher.find()) {
            String tableName = matcher.group(1);
            String fullTableName = matcher.group(2);
            String tableDefinition = matcher.group(3);
            
            DimensionTable dimTable = new DimensionTable();
            dimTable.setName(tableName);
            dimTable.setFullName(fullTableName);
            
            // 解析过滤条件
            String filterCondition = extractFilterCondition(content, tableName);
            dimTable.setFilterCondition(filterCondition);
            
            // 解析表字段
            parseTableFields(tableDefinition, dimTable);
            
            dimensionTables.add(dimTable);
        }
        
        request.setDimensionTables(dimensionTables);
    }
    
    private void parseResultTable(String content, RequestModel request) {
        Pattern resultPattern = Pattern.compile("## 🎯 结果表配置.*?CREATE TABLE[^`]*`([^`]+)`[^(]*\\(([^)]+WITH[^)]+)\\)",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        
        Matcher matcher = resultPattern.matcher(content);
        if (matcher.find()) {
            String fullTableName = matcher.group(1);
            String tableDefinition = matcher.group(2);
            
            ResultTable resultTable = new ResultTable();
            resultTable.setName(fullTableName.substring(fullTableName.lastIndexOf('.') + 1));
            resultTable.setFullName(fullTableName);
            
            parseTableFields(tableDefinition, resultTable);
            
            request.setResultTable(resultTable);
        }
    }
    
    private void parseFieldMapping(String content, RequestModel request) {
        Pattern mappingPattern = Pattern.compile("field_mapping:(.*?)(?=^##|$)", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = mappingPattern.matcher(content);
        
        if (matcher.find()) {
            String mappingText = matcher.group(1);
            Map<String, String> fieldMapping = new HashMap<>();
            
            // 解析字段映射
            Pattern fieldPattern = Pattern.compile("\\s+(\\w+):\\s*(.+?)(?=\\n\\s+\\w+:|$)", Pattern.DOTALL);
            Matcher fieldMatcher = fieldPattern.matcher(mappingText);
            
            while (fieldMatcher.find()) {
                String fieldName = fieldMatcher.group(1);
                String expression = fieldMatcher.group(2).trim();
                
                // 处理多行表达式
                if (expression.startsWith("|")) {
                    expression = expression.substring(1).trim();
                    expression = expression.replaceAll("\\n\\s+", " ");
                }
                
                fieldMapping.put(fieldName, expression);
            }
            
            request.setFieldMapping(fieldMapping);
        }
    }
    
    private void parseJoinRelationships(String content, RequestModel request) {
        Pattern relationPattern = Pattern.compile("join_relationships:(.*?)(?=^##|^#|$)", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = relationPattern.matcher(content);
        
        if (matcher.find()) {
            String relationText = matcher.group(1);
            List<JoinRelationship> relationships = new ArrayList<>();
            
            // 解析每个关联关系
            Pattern joinPattern = Pattern.compile("\\s+(\\w+):(.*?)(?=\\n\\s+\\w+:|$)", Pattern.DOTALL);
            Matcher joinMatcher = joinPattern.matcher(relationText);
            
            while (joinMatcher.find()) {
                String relationName = joinMatcher.group(1);
                String relationDef = joinMatcher.group(2);
                
                JoinRelationship relationship = new JoinRelationship();
                relationship.setName(relationName);
                relationship.setSourceTable(extractYamlValue(relationDef, "source_table"));
                relationship.setSourceField(extractYamlValue(relationDef, "source_field"));
                relationship.setTargetTable(extractYamlValue(relationDef, "target_table"));
                relationship.setTargetField(extractYamlValue(relationDef, "target_field"));
                relationship.setJoinType(extractYamlValue(relationDef, "join_type"));
                relationship.setAdditionalCondition(extractYamlValue(relationDef, "additional_condition"));
                
                relationships.add(relationship);
            }
            
            request.setJoinRelationships(relationships);
        }
    }
    
    private String generateSql(RequestModel request) {
        StringBuilder sql = new StringBuilder();
        
        // 生成INSERT语句头部
        sql.append("-- ").append(request.getName()).append("\n");
        sql.append("-- ").append(request.getDescription()).append("\n");
        sql.append("-- Generated by Flink AI Generator\n\n");
        
        sql.append("INSERT INTO ").append(request.getResultTable().getFullName()).append("\n");
        
        // 生成SELECT子句
        sql.append("SELECT\n");
        generateSelectClause(sql, request);
        
        // 生成FROM子句
        sql.append("FROM biz_statistic_").append(request.getDomain()).append(" be\n");
        
        // 生成JOIN子句
        generateJoinClause(sql, request);
        
        // 生成WHERE子句
        generateWhereClause(sql, request);
        
        sql.append(";");
        
        return sql.toString();
    }
    
    private void generateSelectClause(StringBuilder sql, RequestModel request) {
        Map<String, String> fieldMapping = request.getFieldMapping();
        List<String> selectFields = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            String fieldName = entry.getKey();
            String expression = entry.getValue();
            
            selectFields.add("    " + expression + " AS " + fieldName);
        }
        
        sql.append(String.join(",\n", selectFields)).append("\n");
    }
    
    private void generateJoinClause(StringBuilder sql, RequestModel request) {
        List<JoinRelationship> relationships = request.getJoinRelationships();
        
        for (JoinRelationship rel : relationships) {
            sql.append(rel.getJoinType()).append(" ");
            
            // 查找目标表的别名
            String targetAlias = getTableAlias(rel.getTargetTable(), request);
            sql.append(rel.getTargetTable()).append(" AS ").append(targetAlias);
            sql.append(" ON ").append(rel.getSourceField()).append(" = ").append(rel.getTargetField());
            
            // 添加额外条件
            if (rel.getAdditionalCondition() != null && !rel.getAdditionalCondition().isEmpty()) {
                sql.append(" AND ").append(rel.getAdditionalCondition());
            }
            
            sql.append("\n");
        }
    }
    
    private void generateWhereClause(StringBuilder sql, RequestModel request) {
        List<String> conditions = new ArrayList<>();
        
        // 添加事件过滤条件
        conditions.add(request.getSourceTable().getEventFilter());
        
        // 添加维表过滤条件
        for (DimensionTable dimTable : request.getDimensionTables()) {
            if (dimTable.getFilterCondition() != null && !dimTable.getFilterCondition().isEmpty()) {
                String alias = getTableAlias(dimTable.getName(), request);
                conditions.add(alias + "." + dimTable.getFilterCondition());
            }
        }
        
        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join("\n  AND ", conditions)).append("\n");
        }
    }
    
    private String getTableAlias(String tableName, RequestModel request) {
        // 生成表别名的简单逻辑
        if (tableName.equals("wrong_question_record")) return "wqr";
        if (tableName.equals("tower_pattern")) return "pt";
        if (tableName.equals("tower_teaching_type_pt")) return "ttp";
        if (tableName.equals("tower_teaching_type")) return "tt";
        
        // 默认使用表名的首字母
        String[] parts = tableName.split("_");
        StringBuilder alias = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                alias.append(part.charAt(0));
            }
        }
        return alias.toString();
    }
    
    private void generateDeploymentScript(RequestModel request, File outputDir) throws IOException {
        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n\n");
        script.append("# Deployment script for ").append(request.getName()).append("\n");
        script.append("# Generated by Flink AI Generator\n\n");
        script.append("FLINK_SQL_FILE=\"sql/").append(request.getDomain()).append("_wide_table.sql\"\n");
        script.append("JOB_NAME=\"").append(request.getName()).append("\"\n\n");
        script.append("echo \"Deploying Flink SQL job: $JOB_NAME\"\n");
        script.append("echo \"SQL file: $FLINK_SQL_FILE\"\n\n");
        script.append("# Add your Flink deployment commands here\n");
        script.append("# flink run -d your-deployment-command\n");
        
        File deployDir = new File(outputDir, "deployment");
        deployDir.mkdirs();
        File scriptFile = new File(deployDir, "deploy-" + request.getDomain() + ".sh");
        Files.write(scriptFile.toPath(), script.toString().getBytes());
    }
    
    private void generateJobConfig(RequestModel request, File outputDir) throws IOException {
        Map<String, Object> config = new HashMap<>();
        config.put("jobName", request.getName());
        config.put("description", request.getDescription());
        config.put("domain", request.getDomain());
        config.put("eventType", request.getEventType());
        config.put("author", request.getAuthor());
        config.put("version", request.getVersion());
        
        Map<String, Object> resources = new HashMap<>();
        resources.put("parallelism", 2);
        resources.put("memory", "2048m");
        resources.put("checkpointInterval", "60000");
        config.put("resources", resources);
        
        File configDir = new File(outputDir, "config");
        configDir.mkdirs();
        File configFile = new File(configDir, request.getDomain() + "-job-config.yaml");
        
        yamlMapper.writeValue(configFile, config);
    }
    
    // 辅助方法
    private String extractYamlValue(String text, String key) {
        Pattern pattern = Pattern.compile(key + ":\\s*[\"']?([^\"'\\n]+)[\"']?");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
    
    private String extractFilterCondition(String content, String tableName) {
        Pattern pattern = Pattern.compile("### 维表\\d+: " + tableName + ".*?- \\*\\*过滤条件\\*\\*: ([^\\n]+)", 
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String condition = matcher.group(1).trim();
            return condition.equals("无") || condition.isEmpty() ? null : condition;
        }
        return null;
    }
    
    private void parsePayloadFields(String fieldsText, SourceTable sourceTable) {
        Pattern fieldPattern = Pattern.compile("private\\s+(\\w+)\\s+(\\w+);");
        Matcher matcher = fieldPattern.matcher(fieldsText);
        
        List<TableField> fields = new ArrayList<>();
        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            
            TableField field = new TableField();
            field.setName(name);
            field.setType(mapJavaTypeToSQL(type));
            field.setPrimaryKey(name.equalsIgnoreCase("id"));
            field.setForeignKey(name.toLowerCase().contains("id") && !name.equalsIgnoreCase("id"));
            
            fields.add(field);
        }
        
        sourceTable.setFields(fields);
    }
    
    private void parseTableFields(String tableDefinition, BaseTable table) {
        Pattern fieldPattern = Pattern.compile("`(\\w+)`\\s+(\\w+[^,\\n]*)[,\\n]");
        Matcher matcher = fieldPattern.matcher(tableDefinition);
        
        List<TableField> fields = new ArrayList<>();
        while (matcher.find()) {
            String name = matcher.group(1);
            String type = matcher.group(2).trim();
            
            if (name.equals("PRIMARY") || name.equals("COMMENT") || name.equals("WITH")) {
                continue;
            }
            
            TableField field = new TableField();
            field.setName(name);
            field.setType(type);
            field.setPrimaryKey(tableDefinition.contains("PRIMARY KEY (" + name + ")"));
            field.setForeignKey(name.toLowerCase().contains("id") && !name.equalsIgnoreCase("id"));
            
            fields.add(field);
        }
        
        table.setFields(fields);
    }
    
    private String mapJavaTypeToSQL(String javaType) {
        switch (javaType.toLowerCase()) {
            case "string": return "VARCHAR(255)";
            case "int": case "integer": return "INT";
            case "long": return "BIGINT";
            case "double": return "DOUBLE";
            case "boolean": return "BOOLEAN";
            default: return "VARCHAR(255)";
        }
    }
}
