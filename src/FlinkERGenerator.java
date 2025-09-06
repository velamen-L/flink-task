import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;

/**
 * Flink SQL ER Diagram Generator
 * Specialized for Flink SQL with BusinessEvent, dimension tables, and proper relationships
 */
public class FlinkERGenerator {
    
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
        String tableType; // "source", "dimension", "result"
        List<TableField> fields;
        String comment;
        
        TableInfo(String name, String tableType) {
            this.name = name;
            this.tableType = tableType;
            this.fields = new ArrayList<>();
            this.comment = "";
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
                     String joinType, String joinCondition, String relationshipType) {
            this.sourceTable = sourceTable;
            this.targetTable = targetTable;
            this.sourceField = sourceField != null ? sourceField : "";
            this.targetField = targetField != null ? targetField : "";
            this.joinType = joinType != null ? joinType.toUpperCase() : "LEFT";
            this.joinCondition = joinCondition != null ? joinCondition : "";
            this.relationshipType = relationshipType != null ? relationshipType : "1:N";
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
                System.out.println("Usage: java FlinkERGenerator <sql-file> [domain] [output-dir]");
                return;
            }
            
            String sqlFile = args[0];
            String domain = args.length > 1 ? args[1] : "default";
            String outputDir = args.length > 2 ? args[2] : "er-diagram";
            
            System.out.println("=== Flink SQL ER Diagram Generator ===");
            System.out.println("SQL File: " + sqlFile);
            System.out.println("Domain: " + domain);
            System.out.println("Output: " + outputDir);
            System.out.println();
            
            Files.createDirectories(Paths.get(outputDir));
            
            String sql = readFile(sqlFile);
            
            Map<String, TableInfo> tables = extractFlinkTables(sql);
            System.out.println("Found " + tables.size() + " Flink tables");
            
            List<TableRelation> relations = extractFlinkRelations(sql, tables);
            System.out.println("Found " + relations.size() + " table relations");
            
            generateMermaidER(tables, relations, outputDir, domain);
            generatePlantUMLER(tables, relations, outputDir, domain);
            generateMarkdownReport(tables, relations, outputDir, domain);
            
            System.out.println("\nFlink ER Diagram generated successfully!");
            System.out.println("Output directory: " + outputDir);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
    
    private static Map<String, TableInfo> extractFlinkTables(String sql) {
        Map<String, TableInfo> tables = new HashMap<>();
        
        // Skip result table - not needed in ER diagram for business logic focus
        String resultTable = extractResultTable(sql);
        if (resultTable != null) {
            System.out.println("Found RESULT table: " + resultTable + " (skipped in ER diagram)");
        }
        
        // Extract source table from FROM clause
        String sourceTable = extractSourceTable(sql);
        if (sourceTable != null) {
            TableInfo table = new TableInfo(sourceTable, "source");
            extractSourceTableFields(sql, table);
            tables.put(sourceTable, table);
            System.out.println("Found SOURCE table: " + sourceTable + " (BusinessEvent)");
        }
        
        // Extract dimension tables from JOIN clauses
        List<String> dimTables = extractDimensionTables(sql);
        for (String dimTable : dimTables) {
            TableInfo table = new TableInfo(dimTable, "dimension");
            extractDimensionTableFields(sql, table);
            tables.put(dimTable, table);
            System.out.println("Found DIMENSION table: " + dimTable);
        }
        
        return tables;
    }
    
    private static String extractResultTable(String sql) {
        Pattern pattern = Pattern.compile(
            "INSERT\\s+INTO\\s+(?:`?\"?\\w+`?\"?\\.)*(?:`?\"?\\w+`?\"?\\.)*([`\"\\w]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1).replace("`", "").replace("\"", "");
        }
        return null;
    }
    
    private static String extractSourceTable(String sql) {
        Pattern pattern = Pattern.compile(
            "FROM\\s+(?:`?\"?\\w+`?\"?\\.)*(?:`?\"?\\w+`?\"?\\.)*([`\"\\w]+)\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1).replace("`", "").replace("\"", "");
        }
        return null;
    }
    
    private static List<String> extractDimensionTables(String sql) {
        List<String> tables = new ArrayList<>();
        
        Pattern pattern = Pattern.compile(
            "(?:LEFT|RIGHT|INNER|FULL)?\\s*JOIN\\s+(?:`?\"?\\w+`?\"?\\.)*(?:`?\"?\\w+`?\"?\\.)*([`\"\\w]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1).replace("`", "").replace("\"", "");
            if (!tables.contains(table)) {
                tables.add(table);
            }
        }
        
        return tables;
    }
    
    private static void extractResultTableFields(String sql, TableInfo table) {
        // Extract fields from SELECT clause
        Pattern selectPattern = Pattern.compile(
            "SELECT\\s+(.*?)\\s+FROM",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = selectPattern.matcher(sql);
        if (matcher.find()) {
            String selectClause = matcher.group(1);
            extractFieldsFromSelect(selectClause, table);
        }
    }
    
    private static void extractFieldsFromSelect(String selectClause, TableInfo table) {
        // Split by comma, but avoid splitting inside functions
        List<String> fields = splitSelectFields(selectClause);
        
        for (String field : fields) {
            field = field.trim();
            if (field.isEmpty() || field.startsWith("--")) continue;
            
            // Extract alias name
            String fieldName = extractFieldAlias(field);
            String fieldType = inferTypeFromExpression(field);
            
            if (fieldName != null && !fieldName.isEmpty()) {
                TableField tableField = new TableField(fieldName, fieldType);
                if (fieldName.equals("id")) {
                    tableField.isPrimaryKey = true;
                }
                table.fields.add(tableField);
            }
        }
    }
    
    private static List<String> splitSelectFields(String selectClause) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parentheses = 0;
        boolean inString = false;
        
        for (int i = 0; i < selectClause.length(); i++) {
            char c = selectClause.charAt(i);
            
            if (c == '\'' && (i == 0 || selectClause.charAt(i-1) != '\\')) {
                inString = !inString;
            }
            
            if (!inString) {
                if (c == '(') parentheses++;
                else if (c == ')') parentheses--;
                else if (c == ',' && parentheses == 0) {
                    fields.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            fields.add(current.toString().trim());
        }
        
        return fields;
    }
    
    private static String extractFieldAlias(String fieldExpression) {
        // Match "... AS alias" pattern
        Pattern aliasPattern = Pattern.compile(
            ".*\\s+AS\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = aliasPattern.matcher(fieldExpression);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // If no AS, try to extract simple field name
        Pattern simplePattern = Pattern.compile("(\\w+)\\s*$");
        Matcher simpleMatcher = simplePattern.matcher(fieldExpression.trim());
        if (simpleMatcher.find()) {
            return simpleMatcher.group(1);
        }
        
        return null;
    }
    
    private static String inferTypeFromExpression(String expression) {
        String upper = expression.toUpperCase();
        
        if (upper.contains("CAST(") && upper.contains("AS BIGINT")) {
            return "BIGINT";
        } else if (upper.contains("CAST(") && upper.contains("AS STRING")) {
            return "VARCHAR(255)";
        } else if (upper.contains("TO_TIMESTAMP_LTZ")) {
            return "TIMESTAMP(3)";
        } else if (upper.contains("JSON_VALUE")) {
            return "VARCHAR(255)";
        } else if (upper.contains("CASE") || upper.contains("WHEN")) {
            return "VARCHAR(255)";
        } else if (expression.matches(".*\\.(\\w+)")) {
            // Direct field reference like pt.name
            return "VARCHAR(255)";
        } else {
            return "VARCHAR(255)";
        }
    }
    
    private static void extractSourceTableFields(String sql, TableInfo table) {
        // Extract payload fields from JSON_VALUE calls
        Pattern jsonPattern = Pattern.compile(
            "JSON_VALUE\\([^,]+,\\s*'\\$\\.([^']+)'\\)",
            Pattern.CASE_INSENSITIVE
        );
        
        Set<String> payloadFields = new HashSet<>();
        Matcher matcher = jsonPattern.matcher(sql);
        while (matcher.find()) {
            String payloadField = matcher.group(1);
            payloadFields.add(camelToSnake(payloadField));
        }
        
        // Add ID field as primary key if present
        if (payloadFields.contains("fix_id")) {
            TableField idField = createField("fix_id", "BIGINT", true, false, "Primary key from payload");
            table.fields.add(idField);
        } else if (payloadFields.contains("id")) {
            TableField idField = createField("id", "BIGINT", true, false, "Primary key from payload");
            table.fields.add(idField);
        }
        
        // Add payload fields as business fields (without payload_ prefix)
        for (String field : payloadFields) {
            if (!field.equals("fix_id") && !field.equals("id")) {
                TableField businessField = createField(field, inferFieldType(field), false, false, "Business field");
                
                // Mark foreign key fields
                if (field.endsWith("_id")) {
                    businessField.isForeignKey = true;
                    businessField.referencedTable = inferReferencedTable(field);
                    businessField.referencedField = "id";
                }
                
                table.fields.add(businessField);
            }
        }
    }
    
    private static void extractDimensionTableFields(String sql, TableInfo table) {
        // Standard dimension table fields
        table.fields.add(createField("id", "BIGINT", true, false, "Primary key"));
        
        // Extract only fields that are actually referenced in the SQL
        Set<String> referencedFields = extractReferencedFields(table.name, sql);
        for (String field : referencedFields) {
            if (!field.equals("id") && !isBusinessEventField(field)) {
                TableField tableField = createField(field, inferFieldType(field), false, false, "");
                // Mark foreign key fields
                if (field.endsWith("_id")) {
                    tableField.isForeignKey = true;
                    tableField.referencedTable = inferReferencedTable(field);
                    tableField.referencedField = "id";
                }
                table.fields.add(tableField);
            }
        }
        
        // Add table-specific fields based on table name
        addTableSpecificFields(table);
    }
    
    private static boolean isBusinessEventField(String fieldName) {
        // Filter out BusinessEvent fields that shouldn't appear in dimension tables
        return fieldName.equals("domain") || fieldName.equals("type") || 
               fieldName.equals("payload") || fieldName.equals("timestamp") ||
               fieldName.equals("event_time");
    }
    
    private static void addTableSpecificFields(TableInfo table) {
        String tableName = table.name.toLowerCase();
        
        if (tableName.contains("pattern")) {
            // tower_pattern specific fields
            if (!hasField(table, "name")) {
                table.fields.add(createField("name", "VARCHAR(255)", false, false, "Pattern name"));
            }
            if (!hasField(table, "type")) {
                table.fields.add(createField("type", "INT", false, false, "Pattern type"));
            }
            if (!hasField(table, "subject")) {
                table.fields.add(createField("subject", "VARCHAR(255)", false, false, "Subject"));
            }
            if (!hasField(table, "difficulty")) {
                table.fields.add(createField("difficulty", "DECIMAL(5,3)", false, false, "Difficulty"));
            }
        } else if (tableName.contains("teaching_type_pt")) {
            // tower_teaching_type_pt specific fields
            if (!hasField(table, "teaching_type_id")) {
                TableField field = createField("teaching_type_id", "BIGINT", false, true, "Teaching type ID");
                field.referencedTable = "tower_teaching_type";
                field.referencedField = "id";
                table.fields.add(field);
            }
            if (!hasField(table, "pt_id")) {
                TableField field = createField("pt_id", "BIGINT", false, true, "Pattern ID");
                field.referencedTable = "tower_pattern";
                field.referencedField = "id";
                table.fields.add(field);
            }
            if (!hasField(table, "is_delete")) {
                table.fields.add(createField("is_delete", "TINYINT", false, false, "Delete flag"));
            }
        } else if (tableName.contains("teaching_type")) {
            // tower_teaching_type specific fields
            if (!hasField(table, "teaching_type_name")) {
                table.fields.add(createField("teaching_type_name", "VARCHAR(255)", false, false, "Teaching type name"));
            }
            if (!hasField(table, "chapter_id")) {
                TableField field = createField("chapter_id", "BIGINT", false, true, "Chapter ID");
                field.referencedTable = "chapter_table";
                field.referencedField = "id";
                table.fields.add(field);
            }
            if (!hasField(table, "is_delete")) {
                table.fields.add(createField("is_delete", "TINYINT", false, false, "Delete flag"));
            }
        }
    }
    
    private static boolean hasField(TableInfo table, String fieldName) {
        return table.fields.stream().anyMatch(f -> f.name.equals(fieldName));
    }
    
    private static TableField createField(String name, String type, boolean isPK, boolean isFK, String comment) {
        TableField field = new TableField(name, type);
        field.isPrimaryKey = isPK;
        field.isForeignKey = isFK;
        field.comment = comment;
        return field;
    }
    
    private static Set<String> extractReferencedFields(String tableName, String sql) {
        Set<String> fields = new HashSet<>();
        
        // Extract from SELECT clause: tableName.fieldName or alias.fieldName
        Pattern fieldPattern = Pattern.compile(
            "\\b(?:" + Pattern.quote(tableName) + "|\\w{1,3})\\.(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = fieldPattern.matcher(sql);
        while (matcher.find()) {
            fields.add(matcher.group(1));
        }
        
        return fields;
    }
    
    private static String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    private static String inferFieldType(String fieldName) {
        String lowerName = fieldName.toLowerCase();
        
        if (lowerName.endsWith("_id") || lowerName.equals("id")) {
            return "BIGINT";
        } else if (lowerName.contains("time") || lowerName.contains("date")) {
            return "TIMESTAMP(3)";
        } else if (lowerName.contains("result") || lowerName.contains("status") || lowerName.contains("delete")) {
            return "TINYINT";
        } else if (lowerName.contains("name") || lowerName.contains("type")) {
            return "VARCHAR(255)";
        } else {
            return "VARCHAR(255)";
        }
    }
    
    private static String inferReferencedTable(String fieldName) {
        if (fieldName.endsWith("_id")) {
            String baseName = fieldName.substring(0, fieldName.length() - 3);
            // Map common field patterns to actual table names
            switch (baseName) {
                case "pattern": return "tower_pattern";
                case "user": return "user_table";
                case "question": return "question_table";
                case "wrong": return "wrong_question_record";
                case "chapter": return "chapter_table";
                case "teaching_type": return "tower_teaching_type";
                default: return baseName + "_table";
            }
        }
        return "";
    }
    
    private static List<TableRelation> extractFlinkRelations(String sql, Map<String, TableInfo> tables) {
        List<TableRelation> relations = new ArrayList<>();
        
        // Extract JOIN relations with proper chain tracking
        relations.addAll(extractJoinRelations(sql));
        
        return relations;
    }
    
    private static List<TableRelation> extractJoinRelations(String sql) {
        List<TableRelation> relations = new ArrayList<>();
        
        // Extract all JOIN blocks individually
        String[] joinBlocks = sql.split("(?i)(?=LEFT\\s+JOIN|RIGHT\\s+JOIN|INNER\\s+JOIN|JOIN)");
        String sourceTable = extractSourceTable(sql);
        
        for (String block : joinBlocks) {
            if (!block.trim().toUpperCase().contains("JOIN")) continue;
            
            // Parse individual JOIN block
            TableRelation relation = parseJoinBlock(block.trim(), sourceTable);
            if (relation != null) {
                relations.add(relation);
                System.out.println("Found JOIN: " + relation);
                
                // For chained JOINs, the previous target becomes the source
                if (relation.targetTable.equals("tower_teaching_type_pt")) {
                    sourceTable = "tower_teaching_type_pt";
                }
            }
        }
        
        return relations;
    }
    
    private static TableRelation parseJoinBlock(String joinBlock, String currentSourceTable) {
        // Pattern to extract JOIN components
        Pattern joinPattern = Pattern.compile(
            "(LEFT|RIGHT|INNER|FULL)?\\s*JOIN\\s+" +
            "(?:`[^`]*`\\.)*(?:`[^`]*`\\.)*([`\\w]+)" +
            "(?:\\s+FOR\\s+SYSTEM_TIME\\s+AS\\s+OF\\s+PROCTIME\\(\\))?" +
            "(?:\\s+(\\w+))?" +
            "\\s+ON\\s+" +
            "(.*?)(?=\\s*(?:LEFT|RIGHT|INNER|FULL|WHERE|--)|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = joinPattern.matcher(joinBlock);
        if (matcher.find()) {
            String joinType = matcher.group(1);
            String targetTable = matcher.group(2).replace("`", "").replace("\"", "");
            String alias = matcher.group(3);
            String condition = matcher.group(4).trim();
            
            // Clean up condition - remove extra whitespace and newlines
            condition = condition.replaceAll("\\s+", " ").replaceAll("\\s*AND\\s*.*", "");
            
            // Parse the join condition
            JoinInfo joinInfo = parseJoinCondition(condition, currentSourceTable, targetTable, alias);
            
            if (joinInfo.sourceTable != null && joinInfo.targetTable != null) {
                return new TableRelation(
                    joinInfo.sourceTable, joinInfo.targetTable,
                    joinInfo.sourceField, joinInfo.targetField,
                    joinType, condition, "1:N"
                );
            }
        }
        
        return null;
    }
    
    private static class JoinInfo {
        String sourceTable;
        String targetTable;
        String sourceField;
        String targetField;
        
        JoinInfo(String sourceTable, String targetTable, String sourceField, String targetField) {
            this.sourceTable = sourceTable;
            this.targetTable = targetTable;
            this.sourceField = sourceField;
            this.targetField = targetField;
        }
    }
    
    private static JoinInfo parseJoinCondition(String condition, String sourceTable, String targetTable, String alias) {
        condition = condition.trim();
        
        // Handle JSON_VALUE conditions: pt.id = JSON_VALUE(be.payload, '$.patternId')
        if (condition.contains("JSON_VALUE")) {
            Pattern jsonPattern = Pattern.compile(
                "(\\w+)\\.(\\w+)\\s*=\\s*JSON_VALUE\\([^,]+,\\s*'\\$\\.([^']+)'\\)",
                Pattern.CASE_INSENSITIVE
            );
            
            Matcher matcher = jsonPattern.matcher(condition);
            if (matcher.find()) {
                String dimAlias = matcher.group(1);
                String dimField = matcher.group(2);
                String payloadField = camelToSnake(matcher.group(3));
                
                return new JoinInfo(sourceTable, targetTable, payloadField, dimField);
            }
        }
        
        // Handle regular conditions: ttp.teaching_type_id = tt.id
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
            
            // Determine which is source and which is target
            if (table2Alias.equals(alias)) {
                return new JoinInfo(findTableByAlias(table1Alias), targetTable, field1, field2);
            } else {
                return new JoinInfo(findTableByAlias(table2Alias), targetTable, field2, field1);
            }
        }
        
        return new JoinInfo(sourceTable, targetTable, "", "");
    }
    
    private static String findTableByAlias(String alias) {
        // Common alias mappings
        switch (alias.toLowerCase()) {
            case "be": return "biz_statistic_wrongbook";
            case "pt": return "tower_pattern";
            case "ttp": return "tower_teaching_type_pt";
            case "tt": return "tower_teaching_type";
            default: return alias;
        }
    }
    
    private static void generateMermaidER(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                         String outputDir, String domain) throws IOException {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("erDiagram\n");
        mermaid.append("    %% ").append(domain).append(" Domain Flink SQL ER Diagram\n");
        mermaid.append("    %% Generated: ").append(new Date()).append("\n");
        mermaid.append("    %% Tables: ").append(tables.size()).append(", Relations: ").append(relations.size()).append("\n");
        mermaid.append("\n");
        
        // Group tables by type (exclude result tables)
        for (String tableType : Arrays.asList("source", "dimension")) {
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
        
        // Add relations
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
        
        String filename = outputDir + "/" + domain + "-flink-er-diagram.mermaid";
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
        plantuml.append("!define TITLE ").append(domain).append(" Domain Flink SQL ER Diagram\n");
        plantuml.append("title TITLE\n");
        plantuml.append("!theme aws-orange\n\n");
        
        // Add tables by type with different colors (exclude result tables)
        for (String tableType : Arrays.asList("source", "dimension")) {
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
        
        // Add relations
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
        
        String filename = outputDir + "/" + domain + "-flink-er-diagram.puml";
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
        report.append("# ").append(domain).append(" Domain Flink SQL ER Diagram Report\n\n");
        report.append("**Generated**: ").append(new Date()).append("\n\n");
        
        // Statistics
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
        
        // Table details by type (exclude result tables)
        for (String tableType : Arrays.asList("source", "dimension")) {
            report.append("## ").append(tableType.substring(0, 1).toUpperCase()).append(tableType.substring(1)).append(" Tables\n\n");
            
            for (TableInfo table : tables.values()) {
                if (!table.tableType.equals(tableType)) continue;
                
                report.append("### ").append(table.name).append("\n\n");
                report.append("**Type**: ").append(table.tableType.toUpperCase()).append("\n\n");
                
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
        
        // Relations
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
        report.append("- `").append(domain).append("-flink-er-diagram.mermaid` - Mermaid ER diagram\n");
        report.append("- `").append(domain).append("-flink-er-diagram.puml` - PlantUML ER diagram\n");
        report.append("- `").append(domain).append("-flink-er-report.md` - This report\n");
        
        String filename = outputDir + "/" + domain + "-flink-er-report.md";
        Files.write(Paths.get(filename), report.toString().getBytes());
        System.out.println("Generated: " + filename);
    }
}
