import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;

/**
 * Detailed ER Diagram Generator
 * Enhanced version with complete field information, foreign keys, and relationship types
 */
public class DetailedERGenerator {
    
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
    
    private static class TableInfo {
        String name;
        String schema;
        String catalog;
        List<TableField> fields;
        String comment;
        
        TableInfo(String fullName) {
            this.fields = new ArrayList<>();
            this.comment = "";
            
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
            if ((sourceField != null && sourceField.toLowerCase().contains("id") && targetField != null && targetField.toLowerCase().contains("id")) &&
                (sourceField.toLowerCase().equals("id") || targetField.toLowerCase().equals("id"))) {
                return "1:1";
            }
            
            if (targetField != null && targetField.toLowerCase().equals("id")) {
                return "1:N";
            }
            
            if ((sourceField != null && sourceField.toLowerCase().contains("_id")) || 
                (targetField != null && targetField.toLowerCase().contains("_id"))) {
                return "1:N";
            }
            
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
                System.out.println("Usage: java DetailedERGenerator <sql-file> [domain] [output-dir]");
                return;
            }
            
            String sqlFile = args[0];
            String domain = args.length > 1 ? args[1] : "default";
            String outputDir = args.length > 2 ? args[2] : "er-diagram";
            
            System.out.println("=== Detailed ER Diagram Generator ===");
            System.out.println("SQL File: " + sqlFile);
            System.out.println("Domain: " + domain);
            System.out.println("Output: " + outputDir);
            System.out.println();
            
            Files.createDirectories(Paths.get(outputDir));
            
            String sql = readFile(sqlFile);
            
            Map<String, TableInfo> tables = extractTableInfo(sql);
            System.out.println("Found " + tables.size() + " tables with detailed information");
            
            List<TableRelation> relations = extractTableRelations(sql, tables);
            System.out.println("Found " + relations.size() + " table relations");
            
            generateMermaidER(tables, relations, outputDir, domain);
            generatePlantUMLER(tables, relations, outputDir, domain);
            generateMarkdownReport(tables, relations, outputDir, domain);
            
            System.out.println("\nDetailed ER Diagram generated successfully!");
            System.out.println("Output directory: " + outputDir);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
    
    private static Map<String, TableInfo> extractTableInfo(String sql) {
        Map<String, TableInfo> tables = new HashMap<>();
        
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
            
            parseTableFields(fieldsSection, table);
            
            tables.put(table.getSimpleName(), table);
            System.out.println("Extracted table: " + table.getSimpleName() + " with " + table.fields.size() + " fields");
        }
        
        if (tables.isEmpty()) {
            tables = inferTablesFromSQL(sql);
        }
        
        return tables;
    }
    
    private static void parseTableFields(String fieldsSection, TableInfo table) {
        String[] lines = fieldsSection.split(",(?=\\s*[`\"\\w])");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.toUpperCase().startsWith("PRIMARY KEY") || 
                line.toUpperCase().startsWith("WATERMARK") ||
                line.toUpperCase().startsWith("UNIQUE")) {
                
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
            
            parseFieldDefinition(line, table);
        }
    }
    
    private static void parseFieldDefinition(String fieldDef, TableInfo table) {
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
    
    private static Map<String, TableInfo> inferTablesFromSQL(String sql) {
        Map<String, TableInfo> tables = new HashMap<>();
        Set<String> tableNames = extractAllTables(sql);
        
        for (String tableName : tableNames) {
            TableInfo table = new TableInfo(tableName);
            
            inferCommonFields(table, sql);
            
            tables.put(table.getSimpleName(), table);
        }
        
        return tables;
    }
    
    private static void inferCommonFields(TableInfo table, String sql) {
        table.fields.add(new TableField("id", "BIGINT"));
        table.fields.get(0).isPrimaryKey = true;
        
        Set<String> mentionedFields = extractFieldsForTable(table.getSimpleName(), sql);
        for (String fieldName : mentionedFields) {
            if (!fieldName.equals("id")) {
                TableField field = new TableField(fieldName, inferFieldType(fieldName));
                if (fieldName.toLowerCase().endsWith("_id")) {
                    field.isForeignKey = true;
                    field.referencedTable = inferReferencedTable(fieldName);
                    field.referencedField = "id";
                }
                table.fields.add(field);
            }
        }
        
        if (!mentionedFields.contains("create_time")) {
            table.fields.add(new TableField("create_time", "TIMESTAMP(3)"));
        }
        if (!mentionedFields.contains("update_time")) {
            table.fields.add(new TableField("update_time", "TIMESTAMP(3)"));
        }
    }
    
    private static String inferReferencedTable(String fieldName) {
        if (fieldName.endsWith("_id")) {
            String baseName = fieldName.substring(0, fieldName.length() - 3);
            return baseName + "_table";
        }
        return "";
    }
    
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
    
    private static Set<String> extractFieldsForTable(String tableName, String sql) {
        Set<String> fields = new HashSet<>();
        
        Pattern jsonPattern = Pattern.compile("JSON_VALUE\\([^,]+,\\s*'\\$\\.([^']+)'\\)", Pattern.CASE_INSENSITIVE);
        Matcher jsonMatcher = jsonPattern.matcher(sql);
        while (jsonMatcher.find()) {
            fields.add(camelToSnake(jsonMatcher.group(1)));
        }
        
        Pattern fieldPattern = Pattern.compile("\\b" + Pattern.quote(tableName) + "\\.(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher fieldMatcher = fieldPattern.matcher(sql);
        while (fieldMatcher.find()) {
            fields.add(fieldMatcher.group(1));
        }
        
        return fields;
    }
    
    private static String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    private static Set<String> extractAllTables(String sql) {
        Set<String> tables = new HashSet<>();
        
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
    
    private static List<TableRelation> extractTableRelations(String sql, Map<String, TableInfo> tables) {
        List<TableRelation> relations = new ArrayList<>();
        
        Pattern joinPattern = Pattern.compile(
            "(LEFT|RIGHT|INNER|FULL)?\\s*JOIN\\s+" +
            "(?:`?\"?\\w+`?\"?\\.)*([`\"\\w]+)" +
            "(?:\\s+FOR\\s+SYSTEM_TIME\\s+AS\\s+OF\\s+PROCTIME\\(\\))?" +
            "(?:\\s+(\\w+))?" +
            "\\s+ON\\s+" +
            "([^\\n]+)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = joinPattern.matcher(sql);
        String currentSourceTable = null;
        
        while (matcher.find()) {
            String joinType = matcher.group(1);
            String targetTable = matcher.group(2).replace("`", "").replace("\"", "");
            String alias = matcher.group(3);
            String condition = matcher.group(4).trim();
            
            if (currentSourceTable == null) {
                currentSourceTable = findSourceTableFromSQL(sql, matcher.start());
            }
            
            if (currentSourceTable != null) {
                String[] fieldMapping = parseJoinCondition(condition, currentSourceTable, targetTable, alias);
                
                TableRelation relation = new TableRelation(
                    currentSourceTable, targetTable, 
                    fieldMapping[0], fieldMapping[1], 
                    joinType, condition
                );
                
                relations.add(relation);
                System.out.println("Found JOIN: " + relation);
                
                markForeignKey(tables, relation);
            }
        }
        
        relations.addAll(extractImplicitRelations(sql, tables));
        
        return relations;
    }
    
    private static void markForeignKey(Map<String, TableInfo> tables, TableRelation relation) {
        TableInfo sourceTable = tables.get(relation.sourceTable);
        if (sourceTable != null && !relation.sourceField.isEmpty()) {
            for (TableField field : sourceTable.fields) {
                if (field.name.equals(relation.sourceField)) {
                    field.isForeignKey = true;
                    field.referencedTable = relation.targetTable;
                    field.referencedField = relation.targetField;
                    break;
                }
            }
        }
    }
    
    private static String[] parseJoinCondition(String condition, String sourceTable, String targetTable, String alias) {
        String sourceField = "";
        String targetField = "";
        
        condition = condition.trim().replaceAll("\\s+", " ");
        
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
        
        if (condition.contains("JSON_VALUE")) {
            Pattern jsonPattern = Pattern.compile("JSON_VALUE\\([^,]+,\\s*'\\$\\.([^']+)'\\)", Pattern.CASE_INSENSITIVE);
            Matcher jsonMatcher = jsonPattern.matcher(condition);
            if (jsonMatcher.find()) {
                sourceField = camelToSnake(jsonMatcher.group(1));
            }
            
            Pattern targetPattern = Pattern.compile("=\\s*(\\w+)\\.(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher targetMatcher = targetPattern.matcher(condition);
            if (targetMatcher.find()) {
                targetField = targetMatcher.group(2);
            }
        }
        
        return new String[]{sourceField, targetField};
    }
    
    private static String findSourceTableFromSQL(String sql, int position) {
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
    
    private static List<TableRelation> extractImplicitRelations(String sql, Map<String, TableInfo> tables) {
        List<TableRelation> relations = new ArrayList<>();
        
        Pattern jsonPattern = Pattern.compile(
            "JSON_VALUE\\([^,]+,\\s*'\\$\\.([^']+)'\\)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = jsonPattern.matcher(sql);
        while (matcher.find()) {
            String jsonField = matcher.group(1);
            String snakeField = camelToSnake(jsonField);
            
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
    
    private static void generateMermaidER(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                         String outputDir, String domain) throws IOException {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("erDiagram\n");
        mermaid.append("    %% ").append(domain).append(" Domain ER Diagram\n");
        mermaid.append("    %% Generated: ").append(new Date()).append("\n");
        mermaid.append("    %% Tables: ").append(tables.size()).append(", Relations: ").append(relations.size()).append("\n");
        mermaid.append("\n");
        
        for (TableInfo table : tables.values()) {
            mermaid.append("    ").append(table.getSimpleName()).append(" {\n");
            
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
        
        String filename = outputDir + "/" + domain + "-detailed-er-diagram.mermaid";
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
            case "M:N": return "}o--o{";
            default: return "||--o{";
        }
    }
    
    private static void generatePlantUMLER(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                          String outputDir, String domain) throws IOException {
        StringBuilder plantuml = new StringBuilder();
        plantuml.append("@startuml\n");
        plantuml.append("!define TITLE ").append(domain).append(" Domain ER Diagram (Detailed)\n");
        plantuml.append("title TITLE\n");
        plantuml.append("!theme aws-orange\n\n");
        
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
                       
                if (!field.isNullable) {
                    plantuml.append(" NOT NULL");
                }
                
                if (field.isForeignKey && !field.referencedTable.isEmpty()) {
                    plantuml.append(" FK(").append(field.referencedTable).append(".").append(field.referencedField).append(")");
                }
                
                if (!field.comment.isEmpty()) {
                    plantuml.append(" <<").append(field.comment).append(">>");
                }
                
                plantuml.append("\n");
            }
            
            plantuml.append("}\n\n");
        }
        
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
        
        String filename = outputDir + "/" + domain + "-detailed-er-diagram.puml";
        Files.write(Paths.get(filename), plantuml.toString().getBytes());
        System.out.println("Generated: " + filename);
    }
    
    private static String getPlantUMLRelationSymbol(String relationshipType) {
        switch (relationshipType) {
            case "1:1": return "||--||";
            case "1:N": return "||--o{";
            case "M:N": return "}o--o{";
            default: return "||--o{";
        }
    }
    
    private static void generateMarkdownReport(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                             String outputDir, String domain) throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("# ").append(domain).append(" Domain ER Diagram Report (Detailed)\n\n");
        report.append("**Generated**: ").append(new Date()).append("\n\n");
        
        report.append("## Statistics\n\n");
        report.append("- **Total Tables**: ").append(tables.size()).append("\n");
        report.append("- **Total Relations**: ").append(relations.size()).append("\n");
        report.append("- **Total Fields**: ").append(tables.values().stream().mapToInt(t -> t.fields.size()).sum()).append("\n");
        
        int pkCount = 0;
        int fkCount = 0;
        for (TableInfo table : tables.values()) {
            for (TableField field : table.fields) {
                if (field.isPrimaryKey) pkCount++;
                if (field.isForeignKey) fkCount++;
            }
        }
        report.append("- **Primary Keys**: ").append(pkCount).append("\n");
        report.append("- **Foreign Keys**: ").append(fkCount).append("\n\n");
        
        report.append("## Table Details\n\n");
        for (TableInfo table : tables.values()) {
            report.append("### ").append(table.getSimpleName()).append("\n\n");
            if (!table.comment.isEmpty()) {
                report.append("**Comment**: ").append(table.comment).append("\n\n");
            }
            
            report.append("| Field | Type | Key | Nullable | References | Comment |\n");
            report.append("|-------|------|-----|----------|------------|----------|\n");
            
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
                      .append(" | ");
                      
                if (field.isForeignKey && !field.referencedTable.isEmpty()) {
                    report.append(field.referencedTable).append(".").append(field.referencedField);
                } else {
                    report.append("-");
                }
                
                report.append(" | ").append(field.comment)
                      .append(" |\n");
            }
            
            report.append("\n");
        }
        
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
        report.append("- `").append(domain).append("-detailed-er-diagram.mermaid` - Mermaid ER diagram with full field details\n");
        report.append("- `").append(domain).append("-detailed-er-diagram.puml` - PlantUML ER diagram with full field details\n");
        report.append("- `").append(domain).append("-detailed-er-report.md` - This comprehensive report\n");
        
        String filename = outputDir + "/" + domain + "-detailed-er-report.md";
        Files.write(Paths.get(filename), report.toString().getBytes());
        System.out.println("Generated: " + filename);
    }
}
