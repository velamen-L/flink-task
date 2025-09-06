import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;

/**
 * Enhanced ER Diagram Generator
 * 增强版ER图生成器 - 支持完整字段信息、关联键、关系类型等
 */
public class EnhancedERGenerator {
    
    // 字段信息类
    private static class TableField {
        String name;
        String type;
        boolean isPrimaryKey;
        boolean isForeignKey;
        String referencedTable;
        String referencedField;
        String comment;
        boolean isNullable;
        
        TableField(String name, String type) {
            this.name = name;
            this.type = type;
            this.isPrimaryKey = false;
            this.isForeignKey = false;
            this.comment = "";
            this.isNullable = true;
            this.referencedTable = "";
            this.referencedField = "";
        }
    }
    
    // 表信息类
    private static class TableInfo {
        String name;
        String schema;
        String catalog;
        List<TableField> fields;
        String comment;
        
        TableInfo(String fullName) {
            this.fields = new ArrayList<>();
            this.comment = "";
            
            // 解析完整表名 catalog.schema.table
            String[] parts = fullName.replace("`", "").split("\\.");
            if (parts.length >= 3) {
                this.catalog = parts[0];
                this.schema = parts[1];
                this.name = parts[2];
            } else if (parts.length == 2) {
                this.catalog = "";
                this.schema = parts[0];
                this.name = parts[1];
            } else {
                this.catalog = "";
                this.schema = "";
                this.name = parts[0];
            }
        }
        
        String getSimpleName() {
            return name;
        }
        
        String getFullName() {
            if (!catalog.isEmpty() && !schema.isEmpty()) {
                return catalog + "." + schema + "." + name;
            } else if (!schema.isEmpty()) {
                return schema + "." + name;
            }
            return name;
        }
    }
    
    // 表关联关系类
    private static class TableRelation {
        String sourceTable;
        String targetTable;
        String sourceField;
        String targetField;
        String joinType;
        String joinCondition;
        String relationshipType; // "1:1", "1:N", "M:N"
        
        TableRelation(String sourceTable, String targetTable, String sourceField, String targetField, 
                     String joinType, String joinCondition) {
            this.sourceTable = sourceTable;
            this.targetTable = targetTable;
            this.sourceField = sourceField != null ? sourceField : "";
            this.targetField = targetField != null ? targetField : "";
            this.joinType = joinType != null ? joinType.toUpperCase() : "LEFT";
            this.joinCondition = joinCondition != null ? joinCondition : "";
            
            // 根据JOIN类型推断关系类型
            this.relationshipType = inferRelationshipType(joinType, sourceField, targetField);
        }
        
        private String inferRelationshipType(String joinType, String sourceField, String targetField) {
            // 如果是主键对主键，通常是1:1
            if ((sourceField.toLowerCase().contains("id") && targetField.toLowerCase().contains("id")) &&
                (sourceField.toLowerCase().equals("id") || targetField.toLowerCase().equals("id"))) {
                return "1:1";
            }
            
            // 如果目标字段是id，通常是1:N
            if (targetField.toLowerCase().equals("id")) {
                return "1:N";
            }
            
            // 如果是外键关联，通常是1:N
            if (sourceField.toLowerCase().contains("_id") || targetField.toLowerCase().contains("_id")) {
                return "1:N";
            }
            
            // 默认1:N
            return "1:N";
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
                System.out.println("Usage: java EnhancedERGenerator <sql-file> [domain] [output-dir]");
                return;
            }
            
            String sqlFile = args[0];
            String domain = args.length > 1 ? args[1] : "default";
            String outputDir = args.length > 2 ? args[2] : "er-diagram";
            
            System.out.println("=== Enhanced ER Diagram Generator ===");
            System.out.println("SQL File: " + sqlFile);
            System.out.println("Domain: " + domain);
            System.out.println("Output: " + outputDir);
            System.out.println();
            
            // Create output directory
            Files.createDirectories(Paths.get(outputDir));
            
            // Read SQL file
            String sql = readFile(sqlFile);
            
            // Extract table information
            Map<String, TableInfo> tables = extractTableInfo(sql);
            System.out.println("Found " + tables.size() + " tables with detailed information");
            
            // Extract table relations
            List<TableRelation> relations = extractTableRelations(sql, tables);
            System.out.println("Found " + relations.size() + " table relations");
            
            // Generate outputs
            generateMermaidER(tables, relations, outputDir, domain);
            generatePlantUMLER(tables, relations, outputDir, domain);
            generateMarkdownReport(tables, relations, outputDir, domain);
            
            System.out.println("\nEnhanced ER Diagram generated successfully!");
            System.out.println("Output directory: " + outputDir);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 读取文件内容
    private static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
    
    // 从SQL中提取表信息（包括字段、类型等）
    private static Map<String, TableInfo> extractTableInfo(String sql) {
        Map<String, TableInfo> tables = new HashMap<>();
        
        // 匹配CREATE TABLE语句
        Pattern createTablePattern = Pattern.compile(
            "CREATE\\s+TABLE\\s+([`\"\\w\\.]+)\\s*\\((.*?)\\)(?:\\s*COMMENT\\s*['\"]([^'\"]*)['\"])?.*?WITH\\s*\\(",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = createTablePattern.matcher(sql);
        while (matcher.find()) {
            String tableName = matcher.group(1).replace("`", "").replace("\"", "");
            String fieldsSection = matcher.group(2);
            String tableComment = matcher.group(3);
            
            TableInfo table = new TableInfo(tableName);
            if (tableComment != null) {
                table.comment = tableComment;
            }
            
            // 解析字段定义
            parseTableFields(fieldsSection, table);
            
            tables.put(table.getSimpleName(), table);
            System.out.println("Extracted table: " + table.getSimpleName() + " with " + table.fields.size() + " fields");
        }
        
        // 如果没有找到CREATE TABLE，从SQL中推断表结构
        if (tables.isEmpty()) {
            tables = inferTablesFromSQL(sql);
        }
        
        return tables;
    }
    
    // 解析表字段定义
    private static void parseTableFields(String fieldsSection, TableInfo table) {
        // 拆分字段定义行
        String[] lines = fieldsSection.split(",(?=\\s*[`\"\\w])");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 跳过非字段定义（如PRIMARY KEY, WATERMARK等）
            if (line.toUpperCase().startsWith("PRIMARY KEY") || 
                line.toUpperCase().startsWith("WATERMARK") ||
                line.toUpperCase().startsWith("UNIQUE")) {
                
                // 处理PRIMARY KEY定义
                if (line.toUpperCase().startsWith("PRIMARY KEY")) {
                    Pattern pkPattern = Pattern.compile("PRIMARY\\s+KEY\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
                    Matcher pkMatcher = pkPattern.matcher(line);
                    if (pkMatcher.find()) {
                        String pkFields = pkMatcher.group(1);
                        for (String pkField : pkFields.split(",")) {
                            String fieldName = pkField.trim().replace("`", "").replace("\"", "");
                            for (TableField field : table.fields) {
                                if (field.name.equals(fieldName)) {
                                    field.isPrimaryKey = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                continue;
            }
            
            // 解析字段定义
            parseFieldDefinition(line, table);
        }
    }
    
    // 解析单个字段定义
    private static void parseFieldDefinition(String fieldDef, TableInfo table) {
        // 正则匹配字段定义: `field_name` TYPE [NOT NULL] [COMMENT 'comment']
        Pattern fieldPattern = Pattern.compile(
            "([`\"\\w]+)\\s+(\\w+(?:\\([^)]*\\))?(?:\\s+(?:AS|GENERATED)\\s+[^,]+)?)" +
            "(?:\\s+(NOT\\s+NULL))?" +
            "(?:.*?COMMENT\\s*['\"]([^'\"]*)['\"])?",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = fieldPattern.matcher(fieldDef);
        if (matcher.find()) {
            String fieldName = matcher.group(1).replace("`", "").replace("\"", "");
            String fieldType = matcher.group(2);
            String notNull = matcher.group(3);
            String comment = matcher.group(4);
            
            TableField field = new TableField(fieldName, fieldType);
            field.isNullable = (notNull == null);
            if (comment != null) {
                field.comment = comment;
            }
            
            table.fields.add(field);
        }
    }
    
    // 从SQL推断表结构（当没有CREATE TABLE时）
    private static Map<String, TableInfo> inferTablesFromSQL(String sql) {
        Map<String, TableInfo> tables = new HashMap<>();
        Set<String> tableNames = extractAllTables(sql);
        
        for (String tableName : tableNames) {
            TableInfo table = new TableInfo(tableName);
            
            // 推断常见字段
            inferCommonFields(table, sql);
            
            tables.put(table.getSimpleName(), table);
        }
        
        return tables;
    }
    
    // 推断常见字段
    private static void inferCommonFields(TableInfo table, String sql) {
        // 基础字段
        table.fields.add(new TableField("id", "BIGINT"));
        table.fields.get(0).isPrimaryKey = true;
        
        // 从SQL中提取提到的字段
        Set<String> mentionedFields = extractFieldsForTable(table.getSimpleName(), sql);
        for (String fieldName : mentionedFields) {
            if (!fieldName.equals("id")) {
                TableField field = new TableField(fieldName, inferFieldType(fieldName));
                if (fieldName.toLowerCase().endsWith("_id")) {
                    field.isForeignKey = true;
                }
                table.fields.add(field);
            }
        }
        
        // 添加常见的时间戳字段
        if (!mentionedFields.contains("create_time")) {
            table.fields.add(new TableField("create_time", "TIMESTAMP(3)"));
        }
        if (!mentionedFields.contains("update_time")) {
            table.fields.add(new TableField("update_time", "TIMESTAMP(3)"));
        }
    }
    
    // 推断字段类型
    private static String inferFieldType(String fieldName) {
        String lowerName = fieldName.toLowerCase();
        
        if (lowerName.endsWith("_id") || lowerName.equals("id")) {
            return "BIGINT";
        } else if (lowerName.contains("time") || lowerName.contains("date")) {
            return "TIMESTAMP(3)";
        } else if (lowerName.contains("amount") || lowerName.contains("price") || lowerName.contains("rate")) {
            return "DECIMAL(10,2)";
        } else if (lowerName.contains("count") || lowerName.contains("num") || lowerName.contains("size")) {
            return "INT";
        } else if (lowerName.contains("status") || lowerName.contains("type") || lowerName.contains("result")) {
            return "TINYINT";
        } else if (lowerName.contains("name") || lowerName.contains("title")) {
            return "VARCHAR(255)";
        } else if (lowerName.contains("content") || lowerName.contains("desc") || lowerName.contains("detail")) {
            return "TEXT";
        } else {
            return "VARCHAR(255)";
        }
    }
    
    // 提取表的字段
    private static Set<String> extractFieldsForTable(String tableName, String sql) {
        Set<String> fields = new HashSet<>();
        
        // 从JSON_VALUE中提取字段
        Pattern jsonPattern = Pattern.compile("JSON_VALUE\\([^,]+,\\s*'\\$\\.([^']+)'\\)", Pattern.CASE_INSENSITIVE);
        Matcher jsonMatcher = jsonPattern.matcher(sql);
        while (jsonMatcher.find()) {
            fields.add(camelToSnake(jsonMatcher.group(1)));
        }
        
        // 从表别名.字段中提取
        Pattern fieldPattern = Pattern.compile("\\b" + tableName + "\\.(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher fieldMatcher = fieldPattern.matcher(sql);
        while (fieldMatcher.find()) {
            fields.add(fieldMatcher.group(1));
        }
        
        return fields;
    }
    
    // 驼峰转下划线
    private static String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    // 提取所有表名
    private static Set<String> extractAllTables(String sql) {
        Set<String> tables = new HashSet<>();
        
        // 匹配FROM和JOIN后的表名
        Pattern tablePattern = Pattern.compile(
            "(?:FROM|JOIN)\\s+(?:`?\"?\\w+`?\"?\\.)?(?:`?\"?\\w+`?\"?\\.)?([`\"\\w]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = tablePattern.matcher(sql);
        while (matcher.find()) {
            String tableName = matcher.group(1).replace("`", "").replace("\"", "");
            tables.add(tableName);
        }
        
        return tables;
    }
    
    // 提取表关联关系
    private static List<TableRelation> extractTableRelations(String sql, Map<String, TableInfo> tables) {
        List<TableRelation> relations = new ArrayList<>();
        
        // 提取JOIN关系
        Pattern joinPattern = Pattern.compile(
            "(LEFT|RIGHT|INNER|FULL)?\\s*JOIN\\s+" +
            "(?:`?\"?\\w+`?\"?\\.)*([`\"\\w]+)" +  // table name
            "(?:\\s+FOR\\s+SYSTEM_TIME\\s+AS\\s+OF\\s+PROCTIME\\(\\))?" +  // Flink temporal join
            "(?:\\s+(\\w+))?" +  // optional alias
            "\\s+ON\\s+" +
            "([^\\n]+)",  // condition
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = joinPattern.matcher(sql);
        String currentSourceTable = null;
        
        while (matcher.find()) {
            String joinType = matcher.group(1);
            String targetTable = matcher.group(2).replace("`", "").replace("\"", "");
            String alias = matcher.group(3);
            String condition = matcher.group(4).trim();
            
            // 查找源表
            if (currentSourceTable == null) {
                currentSourceTable = findSourceTableFromSQL(sql, matcher.start());
            }
            
            if (currentSourceTable != null) {
                // 解析JOIN条件，提取字段关联
                String[] fieldMapping = parseJoinCondition(condition, currentSourceTable, targetTable, alias);
                
                TableRelation relation = new TableRelation(
                    currentSourceTable, targetTable, 
                    fieldMapping[0], fieldMapping[1], 
                    joinType, condition
                );
                
                relations.add(relation);
                System.out.println("Found JOIN: " + relation);
            }
        }
        
        // 提取隐式关联（从JSON_VALUE等）
        relations.addAll(extractImplicitRelations(sql, tables));
        
        return relations;
    }
    
    // 解析JOIN条件获取字段映射
    private static String[] parseJoinCondition(String condition, String sourceTable, String targetTable, String alias) {
        String sourceField = "";
        String targetField = "";
        
        // 移除多余的空格和括号
        condition = condition.trim().replaceAll("\\s+", " ");
        
        // 匹配 table1.field = table2.field 或 alias.field = table.field
        Pattern conditionPattern = Pattern.compile(
            "(\\w+)\\.(\\w+)\\s*=\\s*(\\w+)\\.(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = conditionPattern.matcher(condition);
        if (matcher.find()) {
            String table1 = matcher.group(1);
            String field1 = matcher.group(2);
            String table2 = matcher.group(3);
            String field2 = matcher.group(4);
            
            // 判断哪个是源表，哪个是目标表
            if (table1.equalsIgnoreCase(sourceTable) || table1.equalsIgnoreCase("be")) {
                sourceField = field1;
                targetField = field2;
            } else if (table2.equalsIgnoreCase(sourceTable) || table2.equalsIgnoreCase("be")) {
                sourceField = field2;
                targetField = field1;
            } else if (table1.equalsIgnoreCase(alias)) {
                targetField = field1;
                sourceField = field2;
            } else if (table2.equalsIgnoreCase(alias)) {
                targetField = field2;
                sourceField = field1;
            }
        }
        
        // 处理JSON_VALUE的情况
        if (condition.contains("JSON_VALUE")) {
            Pattern jsonPattern = Pattern.compile("JSON_VALUE\\([^,]+,\\s*'\\$\\.([^']+)'\\)", Pattern.CASE_INSENSITIVE);
            Matcher jsonMatcher = jsonPattern.matcher(condition);
            if (jsonMatcher.find()) {
                sourceField = camelToSnake(jsonMatcher.group(1));
            }
            
            // 提取等号另一边的字段
            Pattern targetPattern = Pattern.compile("=\\s*(\\w+)\\.(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher targetMatcher = targetPattern.matcher(condition);
            if (targetMatcher.find()) {
                targetField = targetMatcher.group(2);
            }
        }
        
        return new String[]{sourceField, targetField};
    }
    
    // 查找源表
    private static String findSourceTableFromSQL(String sql, int position) {
        // 在JOIN位置之前查找FROM子句
        String beforeJoin = sql.substring(0, position);
        
        Pattern fromPattern = Pattern.compile(
            "FROM\\s+(?:`?\"?\\w+`?\"?\\.)*([`\"\\w]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = fromPattern.matcher(beforeJoin);
        String sourceTable = null;
        while (matcher.find()) {
            sourceTable = matcher.group(1).replace("`", "").replace("\"", "");
        }
        
        return sourceTable;
    }
    
    // 提取隐式关联关系
    private static List<TableRelation> extractImplicitRelations(String sql, Map<String, TableInfo> tables) {
        List<TableRelation> relations = new ArrayList<>();
        
        // 从JSON_VALUE中推断关联
        Pattern jsonPattern = Pattern.compile(
            "JSON_VALUE\\([^,]+,\\s*'\\$\\.([^']+)'\\)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = jsonPattern.matcher(sql);
        while (matcher.find()) {
            String jsonField = matcher.group(1);
            String snakeField = camelToSnake(jsonField);
            
            // 如果是ID字段，尝试推断关联的表
            if (jsonField.toLowerCase().endsWith("id")) {
                String tableName = inferTableFromField(snakeField, tables.keySet());
                if (tableName != null) {
                    TableRelation relation = new TableRelation(
                        "biz_statistic_wrongbook", tableName,
                        snakeField, "id",
                        "INFERRED", "Inferred from " + jsonField + "..."
                    );
                    relations.add(relation);
                    System.out.println("Found INFERRED relation: " + relation);
                }
            }
        }
        
        return relations;
    }
    
    // 根据字段名推断关联的表
    private static String inferTableFromField(String fieldName, Set<String> allTables) {
        if (!fieldName.endsWith("_id")) {
            return null;
        }
        
        String baseFieldName = fieldName.substring(0, fieldName.length() - 3);
        
        for (String tableName : allTables) {
            if (tableName.toLowerCase().contains(baseFieldName.toLowerCase()) ||
                baseFieldName.toLowerCase().contains(tableName.toLowerCase())) {
                return tableName;
            }
        }
        
        return null;
    }
    
    // 生成Mermaid ER图
    private static void generateMermaidER(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                         String outputDir, String domain) throws IOException {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("erDiagram\n");
        mermaid.append("    %% ").append(domain).append(" Domain ER Diagram\n");
        mermaid.append("    %% Generated: ").append(new Date()).append("\n");
        mermaid.append("    %% Tables: ").append(tables.size()).append(", Relations: ").append(relations.size()).append("\n");
        mermaid.append("\n");
        
        // 添加表定义
        for (TableInfo table : tables.values()) {
            mermaid.append("    ").append(table.getSimpleName()).append(" {\n");
            
            for (TableField field : table.fields) {
                mermaid.append("        ")
                       .append(field.type.toLowerCase())
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
        
        // 添加关系定义
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
        
        // 写入文件
        String filename = outputDir + "/" + domain + "-er-diagram.mermaid";
        Files.write(Paths.get(filename), mermaid.toString().getBytes());
        System.out.println("Generated: " + filename);
    }
    
    // 获取关系符号
    private static String getRelationSymbol(String relationshipType) {
        switch (relationshipType) {
            case "1:1": return "||--||";
            case "1:N": return "||--o{";
            case "M:N": return "}o--o{";
            default: return "||--o{";
        }
    }
    
    // 生成PlantUML ER图
    private static void generatePlantUMLER(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                          String outputDir, String domain) throws IOException {
        StringBuilder plantuml = new StringBuilder();
        plantuml.append("@startuml\n");
        plantuml.append("!define TITLE ").append(domain).append(" Domain ER Diagram\n");
        plantuml.append("title TITLE\n");
        plantuml.append("!theme aws-orange\n\n");
        
        // 添加表定义
        for (TableInfo table : tables.values()) {
            plantuml.append("entity \"").append(table.getSimpleName()).append("\" {\n");
            
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
        
        // 添加关系定义
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
        
        // 写入文件
        String filename = outputDir + "/" + domain + "-er-diagram.puml";
        Files.write(Paths.get(filename), plantuml.toString().getBytes());
        System.out.println("Generated: " + filename);
    }
    
    // 获取PlantUML关系符号
    private static String getPlantUMLRelationSymbol(String relationshipType) {
        switch (relationshipType) {
            case "1:1": return "||--||";
            case "1:N": return "||--o{";
            case "M:N": return "}o--o{";
            default: return "||--o{";
        }
    }
    
    // 生成Markdown报告
    private static void generateMarkdownReport(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                             String outputDir, String domain) throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("# ").append(domain).append(" Domain ER Diagram Report\n\n");
        report.append("**Generated**: ").append(new Date()).append("\n\n");
        
        // 统计信息
        report.append("## Statistics\n\n");
        report.append("- **Total Tables**: ").append(tables.size()).append("\n");
        report.append("- **Total Relations**: ").append(relations.size()).append("\n");
        report.append("- **Total Fields**: ").append(tables.values().stream().mapToInt(t -> t.fields.size()).sum()).append("\n\n");
        
        // 表详情
        report.append("## Table Details\n\n");
        for (TableInfo table : tables.values()) {
            report.append("### ").append(table.getSimpleName()).append("\n\n");
            if (!table.comment.isEmpty()) {
                report.append("**Comment**: ").append(table.comment).append("\n\n");
            }
            
            report.append("| Field | Type | Key | Nullable | Comment |\n");
            report.append("|-------|------|-----|----------|----------|\n");
            
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
                
                report.append(" | ").append(field.isNullable ? "Yes" : "No")
                      .append(" | ").append(field.comment)
                      .append(" |\n");
            }
            
            report.append("\n");
        }
        
        // 关系详情
        report.append("## Table Relations\n\n");
        report.append("| Source Table | Target Table | Relationship | Source Field | Target Field | JOIN Type | Condition |\n");
        report.append("|--------------|--------------|--------------|--------------|--------------|-----------|------------|\n");
        
        for (TableRelation relation : relations) {
            report.append("| ").append(relation.sourceTable)
                  .append(" | ").append(relation.targetTable)
                  .append(" | ").append(relation.relationshipType)
                  .append(" | ").append(relation.sourceField)
                  .append(" | ").append(relation.targetField)
                  .append(" | ").append(relation.joinType)
                  .append(" | ").append(relation.joinCondition.length() > 50 ? 
                          relation.joinCondition.substring(0, 50) + "..." : relation.joinCondition)
                  .append(" |\n");
        }
        
        report.append("\n## Files Generated\n\n");
        report.append("- `").append(domain).append("-er-diagram.mermaid` - Mermaid ER diagram\n");
        report.append("- `").append(domain).append("-er-diagram.puml` - PlantUML ER diagram\n");
        report.append("- `").append(domain).append("-er-report.md` - This detailed report\n");
        
        // 写入文件
        String filename = outputDir + "/" + domain + "-er-report.md";
        Files.write(Paths.get(filename), report.toString().getBytes());
        System.out.println("Generated: " + filename);
    }
}
