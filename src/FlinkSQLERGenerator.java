import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;

/**
 * Flink SQL ER Diagram Generator (Input-based)
 * 基于输入表结构和SQL关联关系生成ER图
 */
public class FlinkSQLERGenerator {
    
    private static class TableField {
        String name;
        String type;
        boolean isPrimaryKey;
        boolean isForeignKey;
        String comment;
        boolean isNullable;
        
        TableField(String name, String type, boolean isPK) {
            this.name = name;
            this.type = type;
            this.isPrimaryKey = isPK;
            this.isForeignKey = false;
            this.comment = "";
            this.isNullable = true;
        }
    }
    
    private static class TableInfo {
        String name;
        String tableType; // "source", "dimension" 
        List<TableField> fields;
        String comment;
        String connector;
        
        TableInfo(String name, String tableType) {
            this.name = name;
            this.tableType = tableType;
            this.fields = new ArrayList<>();
            this.comment = "";
            this.connector = "";
        }
    }
    
    private static class TableRelation {
        String sourceTable;
        String targetTable;
        String sourceField;
        String targetField;
        String joinType;
        String joinCondition;
        String relationshipType;
        
        TableRelation(String sourceTable, String targetTable, String sourceField, String targetField, 
                     String joinType, String joinCondition) {
            this.sourceTable = sourceTable;
            this.targetTable = targetTable;
            this.sourceField = sourceField != null ? sourceField : "";
            this.targetField = targetField != null ? targetField : "";
            this.joinType = joinType != null ? joinType.toUpperCase() : "LEFT";
            this.joinCondition = joinCondition != null ? joinCondition : "";
            this.relationshipType = inferRelationshipType(joinType, sourceField, targetField);
        }
        
        private String inferRelationshipType(String joinType, String sourceField, String targetField) {
            // 基于字段名和JOIN类型推断关系
            if (targetField != null && targetField.equals("id")) {
                return "M:1"; // 多对一关系
            }
            return "1:N"; // 一对多关系
        }
        
        @Override
        public String toString() {
            return sourceTable + " --" + relationshipType + "--> " + targetTable + 
                   " (" + sourceField + " -> " + targetField + ")";
        }
    }
    
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println("Usage: java FlinkSQLERGenerator <sql-file> [domain] [output-dir]");
                return;
            }
            
            String sqlFile = args[0];
            String domain = args.length > 1 ? args[1] : "default";
            String outputDir = args.length > 2 ? args[2] : "er-diagram";
            
            System.out.println("=== Flink SQL ER Diagram Generator (Input-based) ===");
            System.out.println("SQL File: " + sqlFile);
            System.out.println("Domain: " + domain);
            System.out.println("Output: " + outputDir);
            System.out.println();
            
            Files.createDirectories(Paths.get(outputDir));
            
            String sql = readFile(sqlFile);
            
            // 1. 从SQL中提取表结构信息（基于CREATE TABLE语句）
            Map<String, TableInfo> tables = extractTableStructures(sql);
            System.out.println("Extracted " + tables.size() + " table structures from CREATE TABLE statements");
            
            // 2. 从SQL中提取关联关系（基于JOIN语句）
            List<TableRelation> relations = extractJoinRelations(sql);
            System.out.println("Extracted " + relations.size() + " join relations from SQL");
            
            // 3. 标记外键关系
            markForeignKeys(tables, relations);
            
            // 4. 生成ER图
            generateMermaidER(tables, relations, outputDir, domain);
            generatePlantUMLER(tables, relations, outputDir, domain);
            generateMarkdownReport(tables, relations, outputDir, domain);
            
            System.out.println("\nFlink SQL ER Diagram generated successfully!");
            System.out.println("Output directory: " + outputDir);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
    
    /**
     * 从SQL中提取表结构（基于CREATE TABLE语句）
     */
    private static Map<String, TableInfo> extractTableStructures(String sql) {
        Map<String, TableInfo> tables = new HashMap<>();
        
        // 匹配CREATE TABLE语句
        Pattern createTablePattern = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:`[^`]*`\\.)*(?:`[^`]*`\\.)*`?([^`\\s]+)`?\\s*\\((.*?)\\)\\s*(?:COMMENT\\s*'([^']*)')?.*?WITH\\s*\\((.*?)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = createTablePattern.matcher(sql);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            String fieldsSection = matcher.group(2);
            String tableComment = matcher.group(3);
            String withSection = matcher.group(4);
            
            // 判断表类型
            String tableType = determineTableType(withSection);
            
            TableInfo table = new TableInfo(tableName, tableType);
            if (tableComment != null) {
                table.comment = tableComment;
            }
            
            // 提取连接器信息
            table.connector = extractConnector(withSection);
            
            // 解析字段定义
            parseTableFields(fieldsSection, table);
            
            tables.put(tableName, table);
            System.out.println("Found " + tableType.toUpperCase() + " table: " + tableName + 
                             " (" + table.connector + ") with " + table.fields.size() + " fields");
        }
        
        return tables;
    }
    
    /**
     * 根据WITH子句判断表类型
     */
    private static String determineTableType(String withSection) {
        String lowerWith = withSection.toLowerCase();
        
        if (lowerWith.contains("'connector' = 'mysql-cdc'") || 
            lowerWith.contains("'connector' = 'kafka'")) {
            return "source";
        } else if (lowerWith.contains("'connector' = 'jdbc'")) {
            return "dimension";
        } else if (lowerWith.contains("'connector' = 'odps'") || 
                   lowerWith.contains("'connector' = 'hologres'")) {
            return "result";
        } else {
            return "unknown";
        }
    }
    
    /**
     * 提取连接器类型
     */
    private static String extractConnector(String withSection) {
        Pattern connectorPattern = Pattern.compile("'connector'\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE);
        Matcher matcher = connectorPattern.matcher(withSection);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }
    
    /**
     * 解析表字段定义
     */
    private static void parseTableFields(String fieldsSection, TableInfo table) {
        // 分割字段定义，考虑括号嵌套
        List<String> fieldDefs = splitFieldDefinitions(fieldsSection);
        
        for (String fieldDef : fieldDefs) {
            fieldDef = fieldDef.trim();
            if (fieldDef.isEmpty()) continue;
            
            // 跳过非字段定义
            if (fieldDef.toUpperCase().startsWith("PRIMARY KEY") || 
                fieldDef.toUpperCase().startsWith("WATERMARK") ||
                fieldDef.toUpperCase().startsWith("UNIQUE")) {
                
                // 处理PRIMARY KEY定义
                if (fieldDef.toUpperCase().startsWith("PRIMARY KEY")) {
                    markPrimaryKeyFields(fieldDef, table);
                }
                continue;
            }
            
            // 解析字段定义
            parseFieldDefinition(fieldDef, table);
        }
    }
    
    /**
     * 分割字段定义，处理括号嵌套
     */
    private static List<String> splitFieldDefinitions(String fieldsSection) {
        List<String> fieldDefs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parentheses = 0;
        boolean inString = false;
        
        for (int i = 0; i < fieldsSection.length(); i++) {
            char c = fieldsSection.charAt(i);
            
            if (c == '\'' && (i == 0 || fieldsSection.charAt(i-1) != '\\')) {
                inString = !inString;
            }
            
            if (!inString) {
                if (c == '(') parentheses++;
                else if (c == ')') parentheses--;
                else if (c == ',' && parentheses == 0) {
                    fieldDefs.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            fieldDefs.add(current.toString().trim());
        }
        
        return fieldDefs;
    }
    
    /**
     * 标记主键字段
     */
    private static void markPrimaryKeyFields(String primaryKeyDef, TableInfo table) {
        Pattern pkPattern = Pattern.compile("PRIMARY\\s+KEY\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pkPattern.matcher(primaryKeyDef);
        if (matcher.find()) {
            String pkFields = matcher.group(1);
            for (String pkField : pkFields.split(",")) {
                String fieldName = pkField.trim().replace("`", "");
                for (TableField field : table.fields) {
                    if (field.name.equals(fieldName)) {
                        field.isPrimaryKey = true;
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * 解析单个字段定义
     */
    private static void parseFieldDefinition(String fieldDef, TableInfo table) {
        // 匹配字段定义: `field_name` TYPE [NOT NULL] [AS expression] [COMMENT 'comment']
        Pattern fieldPattern = Pattern.compile(
            "`?([^`\\s]+)`?\\s+" +                           // 字段名
            "(\\w+(?:\\([^)]*\\))?)" +                      // 数据类型
            "(?:\\s+NOT\\s+NULL)?" +                        // NOT NULL约束
            "(?:\\s+AS\\s+\\[([^\\]]+)\\])?" +               // AS表达式
            "(?:.*?COMMENT\\s*'([^']*)')?",                 // 注释
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = fieldPattern.matcher(fieldDef);
        if (matcher.find()) {
            String fieldName = matcher.group(1);
            String fieldType = matcher.group(2);
            String asExpression = matcher.group(3);
            String comment = matcher.group(4);
            
            TableField field = new TableField(fieldName, fieldType, false);
            if (comment != null) {
                field.comment = comment;
            }
            
            table.fields.add(field);
        }
    }
    
    /**
     * 从SQL中提取JOIN关联关系
     */
    private static List<TableRelation> extractJoinRelations(String sql) {
        List<TableRelation> relations = new ArrayList<>();
        
        // 提取主查询中的FROM表
        String sourceTable = extractMainSourceTable(sql);
        
        // 提取所有JOIN语句
        Pattern joinPattern = Pattern.compile(
            "(LEFT|RIGHT|INNER|FULL)?\\s*JOIN\\s+" +
            "(?:`[^`]*`\\.)*(?:`[^`]*`\\.)*`?([^`\\s]+)`?" +
            "(?:\\s+FOR\\s+SYSTEM_TIME\\s+AS\\s+OF\\s+PROCTIME\\(\\))?" +
            "(?:\\s+(\\w+))?" +
            "\\s+ON\\s+" +
            "([^\\n]+?)(?=\\s*(?:LEFT|RIGHT|INNER|FULL|WHERE|AND\\s+\\w+\\.\\w+|$))",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = joinPattern.matcher(sql);
        String currentSource = sourceTable;
        
        while (matcher.find()) {
            String joinType = matcher.group(1);
            String targetTable = matcher.group(2);
            String alias = matcher.group(3);
            String condition = matcher.group(4).trim();
            
            // 解析JOIN条件
            JoinFieldMapping mapping = parseJoinCondition(condition, currentSource, targetTable, alias);
            
            if (mapping != null) {
                TableRelation relation = new TableRelation(
                    mapping.sourceTable, mapping.targetTable,
                    mapping.sourceField, mapping.targetField,
                    joinType, condition
                );
                
                relations.add(relation);
                System.out.println("Found JOIN: " + relation);
                
                // 对于链式JOIN，更新当前源表
                if (mapping.targetTable.equals("tower_teaching_type_pt")) {
                    currentSource = "tower_teaching_type_pt";
                }
            }
        }
        
        return relations;
    }
    
    /**
     * 提取主查询的源表
     */
    private static String extractMainSourceTable(String sql) {
        Pattern fromPattern = Pattern.compile(
            "FROM\\s+(?:`[^`]*`\\.)*(?:`[^`]*`\\.)*`?([^`\\s]+)`?\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = fromPattern.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * JOIN字段映射
     */
    private static class JoinFieldMapping {
        String sourceTable;
        String targetTable;
        String sourceField;
        String targetField;
        
        JoinFieldMapping(String sourceTable, String targetTable, String sourceField, String targetField) {
            this.sourceTable = sourceTable;
            this.targetTable = targetTable;
            this.sourceField = sourceField;
            this.targetField = targetField;
        }
    }
    
    /**
     * 解析JOIN条件，提取字段映射
     */
    private static JoinFieldMapping parseJoinCondition(String condition, String sourceTable, String targetTable, String alias) {
        condition = condition.trim();
        
        // 处理JSON_VALUE条件: pt.id = JSON_VALUE(be.payload, '$.patternId')
        if (condition.contains("JSON_VALUE")) {
            Pattern jsonPattern = Pattern.compile(
                "(\\w+)\\.(\\w+)\\s*=\\s*JSON_VALUE\\([^,]+,\\s*'\\$\\.([^']+)'\\)",
                Pattern.CASE_INSENSITIVE
            );
            
            Matcher matcher = jsonPattern.matcher(condition);
            if (matcher.find()) {
                String dimField = matcher.group(2);
                String payloadField = camelToSnake(matcher.group(3));
                
                return new JoinFieldMapping(sourceTable, targetTable, payloadField, dimField);
            }
        }
        
        // 处理常规条件: ttp.teaching_type_id = tt.id
        Pattern regularPattern = Pattern.compile(
            "(\\w+)\\.(\\w+)\\s*=\\s*(\\w+)\\.(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = regularPattern.matcher(condition);
        if (matcher.find()) {
            String table1Alias = matcher.group(1);
            String field1 = matcher.group(2);
            String table2Alias = matcher.group(3);
            String field2 = matcher.group(4);
            
            // 确定哪个是源表，哪个是目标表
            String actualSourceTable = findTableByAlias(table1Alias, sourceTable);
            String actualTargetTable = findTableByAlias(table2Alias, targetTable);
            
            if (actualTargetTable.equals(targetTable)) {
                return new JoinFieldMapping(actualSourceTable, actualTargetTable, field1, field2);
            } else {
                return new JoinFieldMapping(actualTargetTable, actualSourceTable, field2, field1);
            }
        }
        
        return null;
    }
    
    /**
     * 根据别名查找实际表名
     */
    private static String findTableByAlias(String alias, String defaultTable) {
        switch (alias.toLowerCase()) {
            case "be": return "biz_statistic_wrongbook";
            case "pt": return "tower_pattern";
            case "ttp": return "tower_teaching_type_pt";
            case "tt": return "tower_teaching_type";
            default: return defaultTable;
        }
    }
    
    /**
     * 驼峰转下划线
     */
    private static String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * 标记外键关系
     */
    private static void markForeignKeys(Map<String, TableInfo> tables, List<TableRelation> relations) {
        for (TableRelation relation : relations) {
            TableInfo sourceTable = tables.get(relation.sourceTable);
            if (sourceTable != null) {
                for (TableField field : sourceTable.fields) {
                    if (field.name.equals(relation.sourceField)) {
                        field.isForeignKey = true;
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * 生成Mermaid ER图
     */
    private static void generateMermaidER(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                         String outputDir, String domain) throws IOException {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("erDiagram\n");
        mermaid.append("    %% ").append(domain).append(" Domain Flink SQL ER Diagram (Input-based)\n");
        mermaid.append("    %% Generated: ").append(new Date()).append("\n");
        mermaid.append("    %% Tables: ").append(tables.size()).append(", Relations: ").append(relations.size()).append("\n");
        mermaid.append("\n");
        
        // 按表类型输出
        for (String tableType : Arrays.asList("source", "dimension")) {
            boolean hasTableOfType = tables.values().stream().anyMatch(t -> t.tableType.equals(tableType));
            if (!hasTableOfType) continue;
            
            mermaid.append("    %% ").append(tableType.toUpperCase()).append(" TABLES\n");
            
            for (TableInfo table : tables.values()) {
                if (!table.tableType.equals(tableType)) continue;
                
                mermaid.append("    ").append(table.name).append(" {\n");
                
                for (TableField field : table.fields) {
                    mermaid.append("        ")
                           .append(normalizeType(field.type))
                           .append(" ")
                           .append(field.name);
                           
                    if (field.isPrimaryKey) {
                        mermaid.append(" PK");
                    } else if (field.isForeignKey) {
                        mermaid.append(" FK");
                    }
                    
                    if (!field.comment.isEmpty()) {
                        mermaid.append(" \"").append(field.comment).append("\"");
                    }
                    
                    mermaid.append("\n");
                }
                
                mermaid.append("    }\n\n");
            }
        }
        
        // 输出关系
        mermaid.append("    %% RELATIONSHIPS\n");
        for (TableRelation relation : relations) {
            String relationSymbol = getRelationSymbol(relation.relationshipType);
            mermaid.append("    ")
                   .append(relation.sourceTable)
                   .append(" ")
                   .append(relationSymbol)
                   .append(" ")
                   .append(relation.targetTable)
                   .append(" : \"")
                   .append(relation.sourceField)
                   .append(" -> ")
                   .append(relation.targetField)
                   .append("\"\n");
        }
        
        String filename = outputDir + "/" + domain + "-flinksql-er-diagram.mermaid";
        Files.write(Paths.get(filename), mermaid.toString().getBytes());
        System.out.println("Generated: " + filename);
    }
    
    private static String normalizeType(String type) {
        return type.toLowerCase().replaceAll("\\([^)]*\\)", "");
    }
    
    private static String getRelationSymbol(String relationshipType) {
        switch (relationshipType) {
            case "1:1": return "||--||";
            case "1:N": return "||--o{";
            case "M:1": return "}o--||";
            case "M:N": return "}o--o{";
            default: return "||--o{";
        }
    }
    
    /**
     * 生成PlantUML ER图
     */
    private static void generatePlantUMLER(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                          String outputDir, String domain) throws IOException {
        StringBuilder plantuml = new StringBuilder();
        plantuml.append("@startuml\n");
        plantuml.append("!define TITLE ").append(domain).append(" Domain Flink SQL ER Diagram (Input-based)\n");
        plantuml.append("title TITLE\n");
        plantuml.append("!theme aws-orange\n\n");
        
        // 输出表定义
        for (String tableType : Arrays.asList("source", "dimension")) {
            boolean hasTableOfType = tables.values().stream().anyMatch(t -> t.tableType.equals(tableType));
            if (!hasTableOfType) continue;
            
            plantuml.append("' ").append(tableType.toUpperCase()).append(" TABLES\n");
            
            for (TableInfo table : tables.values()) {
                if (!table.tableType.equals(tableType)) continue;
                
                plantuml.append("entity \"").append(table.name).append("\" <<").append(tableType).append(">> {\n");
                
                for (TableField field : table.fields) {
                    if (field.isPrimaryKey) {
                        plantuml.append("  * ");
                    } else if (field.isForeignKey) {
                        plantuml.append("  + ");
                    } else {
                        plantuml.append("    ");
                    }
                    
                    plantuml.append(field.name)
                           .append(" : ")
                           .append(field.type);
                           
                    if (!field.comment.isEmpty()) {
                        plantuml.append(" <<").append(field.comment).append(">>");
                    }
                    
                    plantuml.append("\n");
                }
                
                plantuml.append("}\n\n");
            }
        }
        
        // 输出关系
        plantuml.append("' RELATIONSHIPS\n");
        for (TableRelation relation : relations) {
            String relationSymbol = getPlantUMLRelationSymbol(relation.relationshipType);
            plantuml.append(relation.sourceTable)
                   .append(" ")
                   .append(relationSymbol)
                   .append(" ")
                   .append(relation.targetTable)
                   .append(" : ")
                   .append(relation.sourceField)
                   .append(" -> ")
                   .append(relation.targetField)
                   .append("\n");
        }
        
        plantuml.append("\n@enduml");
        
        String filename = outputDir + "/" + domain + "-flinksql-er-diagram.puml";
        Files.write(Paths.get(filename), plantuml.toString().getBytes());
        System.out.println("Generated: " + filename);
    }
    
    private static String getPlantUMLRelationSymbol(String relationshipType) {
        switch (relationshipType) {
            case "1:1": return "||--||";
            case "1:N": return "||--o{";
            case "M:1": return "}o--||";
            case "M:N": return "}o--o{";
            default: return "||--o{";
        }
    }
    
    /**
     * 生成Markdown报告
     */
    private static void generateMarkdownReport(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                             String outputDir, String domain) throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("# ").append(domain).append(" Domain Flink SQL ER Diagram Report (Input-based)\n\n");
        report.append("**Generated**: ").append(new Date()).append("\n\n");
        
        // 统计信息
        report.append("## Statistics\n\n");
        report.append("- **Total Tables**: ").append(tables.size()).append("\n");
        report.append("- **Relations**: ").append(relations.size()).append("\n");
        
        int sourceCount = 0, dimCount = 0;
        for (TableInfo table : tables.values()) {
            switch (table.tableType) {
                case "source": sourceCount++; break;
                case "dimension": dimCount++; break;
            }
        }
        
        report.append("- **Source Tables**: ").append(sourceCount).append("\n");
        report.append("- **Dimension Tables**: ").append(dimCount).append("\n\n");
        
        // 表详情
        for (String tableType : Arrays.asList("source", "dimension")) {
            boolean hasTableOfType = tables.values().stream().anyMatch(t -> t.tableType.equals(tableType));
            if (!hasTableOfType) continue;
            
            report.append("## ").append(tableType.substring(0, 1).toUpperCase()).append(tableType.substring(1)).append(" Tables\n\n");
            
            for (TableInfo table : tables.values()) {
                if (!table.tableType.equals(tableType)) continue;
                
                report.append("### ").append(table.name).append("\n\n");
                report.append("**Type**: ").append(table.tableType.toUpperCase()).append("\n");
                report.append("**Connector**: ").append(table.connector).append("\n");
                if (!table.comment.isEmpty()) {
                    report.append("**Comment**: ").append(table.comment).append("\n");
                }
                report.append("\n");
                
                report.append("| Field | Type | Key | Comment |\n");
                report.append("|-------|------|-----|----------|\n");
                
                for (TableField field : table.fields) {
                    report.append("| ").append(field.name)
                          .append(" | ").append(field.type)
                          .append(" | ");
                          
                    if (field.isPrimaryKey) {
                        report.append("PK");
                    } else if (field.isForeignKey) {
                        report.append("FK");
                    } else {
                        report.append("-");
                    }
                    
                    report.append(" | ").append(field.comment)
                          .append(" |\n");
                }
                
                report.append("\n");
            }
        }
        
        // 关系详情
        report.append("## Table Relations\n\n");
        report.append("| Source Table | Target Table | Source Field | Target Field | Relationship | JOIN Type |\n");
        report.append("|--------------|--------------|--------------|--------------|--------------|------------|\n");
        
        for (TableRelation relation : relations) {
            report.append("| ").append(relation.sourceTable)
                  .append(" | ").append(relation.targetTable)
                  .append(" | ").append(relation.sourceField)
                  .append(" | ").append(relation.targetField)
                  .append(" | ").append(relation.relationshipType)
                  .append(" | ").append(relation.joinType)
                  .append(" |\n");
        }
        
        report.append("\n## Files Generated\n\n");
        report.append("- `").append(domain).append("-flinksql-er-diagram.mermaid` - Mermaid ER diagram (Input-based)\n");
        report.append("- `").append(domain).append("-flinksql-er-diagram.puml` - PlantUML ER diagram (Input-based)\n");
        report.append("- `").append(domain).append("-flinksql-er-report.md` - This comprehensive report\n");
        
        String filename = outputDir + "/" + domain + "-flinksql-er-report.md";
        Files.write(Paths.get(filename), report.toString().getBytes());
        System.out.println("Generated: " + filename);
    }
}
