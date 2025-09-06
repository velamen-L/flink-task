import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;

/**
 * Request-based ER Diagram Generator
 * Generates ER diagrams based on flink-sql-request.md files
 */
public class RequestBasedERGenerator {
    
    private static class TableField {
        String name;
        String type;
        boolean isPrimaryKey;
        boolean isForeignKey;
        String comment;
        
        TableField(String name, String type, boolean isPK) {
            this.name = name;
            this.type = type;
            this.isPrimaryKey = isPK;
            this.isForeignKey = false;
            this.comment = "";
        }
    }
    
    private static class TableInfo {
        String name;
        String tableType; // "source", "dimension", "result"
        List<TableField> fields;
        String comment;
        String connector;
        String alias;
        
        TableInfo(String name, String tableType) {
            this.name = name;
            this.tableType = tableType;
            this.fields = new ArrayList<>();
            this.comment = "";
            this.connector = "";
            this.alias = "";
        }
    }
    
    private static class TableRelation {
        String sourceTable;
        String targetTable;
        String sourceField;
        String targetField;
        String joinCondition;
        String relationshipType;
        
        TableRelation(String sourceTable, String targetTable, String sourceField, String targetField, 
                     String joinCondition, String relationshipType) {
            this.sourceTable = sourceTable;
            this.targetTable = targetTable;
            this.sourceField = sourceField != null ? sourceField : "";
            this.targetField = targetField != null ? targetField : "";
            this.joinCondition = joinCondition != null ? joinCondition : "";
            this.relationshipType = relationshipType != null ? relationshipType : "M:1";
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
                System.out.println("Usage: java RequestBasedERGenerator <request-md-file> [domain] [output-dir]");
                return;
            }
            
            String requestFile = args[0];
            String domain = args.length > 1 ? args[1] : "default";
            String outputDir = args.length > 2 ? args[2] : "er-diagram";
            
            System.out.println("=== Request-based ER Diagram Generator ===");
            System.out.println("Request File: " + requestFile);
            System.out.println("Domain: " + domain);
            System.out.println("Output: " + outputDir);
            System.out.println();
            
            Files.createDirectories(Paths.get(outputDir));
            
            String requestContent = readFile(requestFile);
            
            // Extract table structures from CREATE TABLE statements in the request
            Map<String, TableInfo> tables = extractTableStructures(requestContent);
            System.out.println("Extracted " + tables.size() + " table structures from request");
            
            // Extract relationships from the request
            List<TableRelation> relations = extractRelationships(requestContent, tables);
            System.out.println("Extracted " + relations.size() + " relations from request");
            
            // Mark foreign key relationships
            markForeignKeys(tables, relations);
            
            // Generate ER diagrams
            generateMermaidER(tables, relations, outputDir, domain);
            generatePlantUMLER(tables, relations, outputDir, domain);
            generateMarkdownReport(tables, relations, outputDir, domain);
            
            System.out.println("\nRequest-based ER Diagram generated successfully!");
            System.out.println("Output directory: " + outputDir);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
    
    private static Map<String, TableInfo> extractTableStructures(String content) {
        Map<String, TableInfo> tables = new HashMap<>();
        
        // Extract source table (payload structure)
        extractPayloadAsSourceTable(content, tables);
        
        // Extract dimension tables from CREATE TABLE statements
        extractDimensionTables(content, tables);
        
        // Extract result table (skip in ER diagram as per requirement)
        extractResultTable(content, tables);
        
        return tables;
    }
    
    private static void extractPayloadAsSourceTable(String content, Map<String, TableInfo> tables) {
        // Extract domain and event type
        String domain = extractDomain(content);
        String eventType = extractEventType(content);
        String sourceTableName = domain + "_" + eventType;
        
        // Extract payload structure as source table
        Pattern payloadPattern = Pattern.compile(
            "\\*\\*.*?Payload.*?\\*\\*.*?```java.*?public class.*?\\{(.*?)\\}.*?```",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = payloadPattern.matcher(content);
        if (matcher.find()) {
            String payloadFields = matcher.group(1);
            
            TableInfo sourceTable = new TableInfo(sourceTableName, "source");
            sourceTable.comment = "Business event payload for " + domain + " " + eventType;
            sourceTable.connector = "kafka";
            
            // Parse payload fields
            Pattern fieldPattern = Pattern.compile("private\\s+(\\w+)\\s+(\\w+);");
            Matcher fieldMatcher = fieldPattern.matcher(payloadFields);
            
            while (fieldMatcher.find()) {
                String type = fieldMatcher.group(1);
                String name = camelToSnake(fieldMatcher.group(2));
                
                String sqlType = mapJavaTypeToSQL(type);
                TableField field = new TableField(name, sqlType, false);
                
                if (name.equals("fix_id") || name.endsWith("_id")) {
                    if (name.equals("fix_id")) {
                        field.isPrimaryKey = true;
                        field.comment = "Primary key";
                    } else {
                        field.isForeignKey = true;
                        field.comment = "Foreign key";
                    }
                }
                
                sourceTable.fields.add(field);
            }
            
            tables.put(sourceTableName, sourceTable);
            System.out.println("Found SOURCE table: " + sourceTableName + " (payload) with " + sourceTable.fields.size() + " fields");
        }
    }
    
    private static String extractDomain(String content) {
        Pattern domainPattern = Pattern.compile("domain:\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = domainPattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "business";
    }
    
    private static String extractEventType(String content) {
        Pattern eventTypePattern = Pattern.compile("event_type:\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = eventTypePattern.matcher(content);
        if (matcher.find()) {
            String eventType = matcher.group(1);
            // Remove domain prefix if present (e.g., "wrongbook_fix" -> "fix")
            if (eventType.contains("_")) {
                String[] parts = eventType.split("_");
                if (parts.length > 1) {
                    return parts[parts.length - 1]; // Take the last part
                }
            }
            return eventType;
        }
        return "event";
    }
    
    private static void extractDimensionTables(String content, Map<String, TableInfo> tables) {
        // Pattern to match dimension table sections with CREATE TABLE
        Pattern dimSectionPattern = Pattern.compile(
            "\\*\\*.*?\\*\\*.*?```sql\\s*(CREATE TABLE.*?)```",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = dimSectionPattern.matcher(content);
        while (matcher.find()) {
            String createTableSQL = matcher.group(1);
            
            TableInfo table = parseCreateTableSQL(createTableSQL, "dimension");
            if (table != null && !isResultTable(table.name)) {
                tables.put(table.name, table);
                System.out.println("Found DIMENSION table: " + table.name + " with " + table.fields.size() + " fields");
            } else if (table != null && isResultTable(table.name)) {
                System.out.println("Skipped RESULT table: " + table.name + " (excluded from ER diagram)");
            }
        }
    }
    
    private static boolean isResultTable(String tableName) {
        String lowerName = tableName.toLowerCase();
        return lowerName.startsWith("dwd_") || 
               lowerName.startsWith("dws_") || 
               lowerName.startsWith("ads_") ||
               lowerName.contains("wide") ||
               lowerName.contains("delta");
    }
    
    private static void extractResultTable(String content, Map<String, TableInfo> tables) {
        // Extract result table but don't include in ER diagram
        Pattern resultPattern = Pattern.compile(
            "## .* result.*?```sql(.*?)```",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = resultPattern.matcher(content);
        if (matcher.find()) {
            String createTableSQL = matcher.group(1);
            TableInfo table = parseCreateTableSQL(createTableSQL, "result");
            if (table != null) {
                System.out.println("Found RESULT table: " + table.name + " (excluded from ER diagram)");
            }
        }
    }
    
    private static TableInfo parseCreateTableSQL(String sql, String tableType) {
        // Extract table name
        Pattern tableNamePattern = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:`[^`]*`\\.)*(?:`[^`]*`\\.)*`?([^`\\s]+)`?",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher tableNameMatcher = tableNamePattern.matcher(sql);
        if (!tableNameMatcher.find()) return null;
        
        String tableName = tableNameMatcher.group(1);
        TableInfo table = new TableInfo(tableName, tableType);
        
        // Extract comment
        Pattern commentPattern = Pattern.compile("COMMENT\\s*'([^']*)'", Pattern.CASE_INSENSITIVE);
        Matcher commentMatcher = commentPattern.matcher(sql);
        if (commentMatcher.find()) {
            table.comment = commentMatcher.group(1);
        }
        
        // Extract connector
        Pattern connectorPattern = Pattern.compile("'connector'\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE);
        Matcher connectorMatcher = connectorPattern.matcher(sql);
        if (connectorMatcher.find()) {
            table.connector = connectorMatcher.group(1);
        }
        
        // Extract fields
        Pattern fieldsPattern = Pattern.compile("\\((.*?)\\)\\s*(?:COMMENT|WITH)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher fieldsMatcher = fieldsPattern.matcher(sql);
        if (fieldsMatcher.find()) {
            String fieldsSection = fieldsMatcher.group(1);
            parseFieldsSection(fieldsSection, table);
        }
        
        return table;
    }
    
    private static void parseFieldsSection(String fieldsSection, TableInfo table) {
        String[] lines = fieldsSection.split(",");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("PRIMARY KEY") || line.startsWith("WATERMARK")) {
                if (line.startsWith("PRIMARY KEY")) {
                    markPrimaryKeyFromLine(line, table);
                }
                continue;
            }
            
            // Parse field definition
            Pattern fieldPattern = Pattern.compile(
                "`?([^`\\s]+)`?\\s+(\\w+(?:\\([^)]*\\))?)",
                Pattern.CASE_INSENSITIVE
            );
            
            Matcher matcher = fieldPattern.matcher(line);
            if (matcher.find()) {
                String fieldName = matcher.group(1);
                String fieldType = matcher.group(2);
                
                TableField field = new TableField(fieldName, fieldType, false);
                table.fields.add(field);
            }
        }
    }
    
    private static void markPrimaryKeyFromLine(String line, TableInfo table) {
        Pattern pkPattern = Pattern.compile("PRIMARY\\s+KEY\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pkPattern.matcher(line);
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
    
    private static List<TableRelation> extractRelationships(String content, Map<String, TableInfo> tables) {
        List<TableRelation> relations = new ArrayList<>();
        
        // Extract relationships from dimension table configurations
        Pattern relationPattern = Pattern.compile(
            "### .*: (\\w+).*?\\*\\*.*?\\*\\*: ([^\\n]+)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = relationPattern.matcher(content);
        while (matcher.find()) {
            String targetTable = matcher.group(1);
            String joinCondition = matcher.group(2);
            
            TableRelation relation = parseJoinCondition(joinCondition, targetTable);
            if (relation != null) {
                relations.add(relation);
                System.out.println("Found RELATION: " + relation);
            }
        }
        
        return relations;
    }
    
    private static TableRelation parseJoinCondition(String condition, String targetTable) {
        // Parse conditions like "pt.id = wqr.pattern_id" or "tt.id = ttp.teaching_type_id"
        Pattern pattern = Pattern.compile(
            "(\\w+)\\.(\\w+)\\s*=\\s*(\\w+)\\.(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(condition);
        if (matcher.find()) {
            String table1Alias = matcher.group(1);
            String field1 = matcher.group(2);
            String table2Alias = matcher.group(3);
            String field2 = matcher.group(4);
            
            String sourceTable = findTableByAlias(table2Alias);
            String sourceField = field2;
            String targetField = field1;
            
            // Determine relationship type based on field names
            String relationshipType = "M:1";
            if (targetField.equals("id")) {
                relationshipType = "M:1";
            } else if (sourceField.endsWith("_id")) {
                relationshipType = "M:1";
            }
            
            return new TableRelation(sourceTable, targetTable, sourceField, targetField, condition, relationshipType);
        }
        
        return null;
    }
    
    private static String findTableByAlias(String alias) {
        switch (alias.toLowerCase()) {
            case "wqr": return "wrongbook_fix"; // Use domain_eventType format
            case "be": return "wrongbook_fix"; // BusinessEvent alias
            case "pt": return "tower_pattern";
            case "ttp": return "tower_teaching_type_pt";
            case "tt": return "tower_teaching_type";
            default: return alias;
        }
    }
    
    private static String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    private static String mapJavaTypeToSQL(String javaType) {
        switch (javaType.toLowerCase()) {
            case "string": return "VARCHAR(255)";
            case "long": return "BIGINT";
            case "int": return "INT";
            case "boolean": return "BOOLEAN";
            case "double": return "DOUBLE";
            case "float": return "FLOAT";
            default: return "VARCHAR(255)";
        }
    }
    
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
    
    private static void generateMermaidER(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                         String outputDir, String domain) throws IOException {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("erDiagram\n");
        mermaid.append("    %% ").append(domain).append(" Domain Request-based ER Diagram\n");
        mermaid.append("    %% Generated: ").append(new Date()).append("\n");
        mermaid.append("    %% Tables: ").append(tables.size()).append(", Relations: ").append(relations.size()).append("\n");
        mermaid.append("\n");
        
        // Output tables by type (exclude result tables)
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
        
        // Output relationships
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
        
        String filename = outputDir + "/" + domain + "-request-based-er-diagram.mermaid";
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
            default: return "}o--||";
        }
    }
    
    private static void generatePlantUMLER(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                          String outputDir, String domain) throws IOException {
        StringBuilder plantuml = new StringBuilder();
        plantuml.append("@startuml\n");
        plantuml.append("!define TITLE ").append(domain).append(" Domain Request-based ER Diagram\n");
        plantuml.append("title TITLE\n");
        plantuml.append("!theme aws-orange\n\n");
        
        // Output tables by type (exclude result tables)
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
        
        // Output relationships
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
        
        String filename = outputDir + "/" + domain + "-request-based-er-diagram.puml";
        Files.write(Paths.get(filename), plantuml.toString().getBytes());
        System.out.println("Generated: " + filename);
    }
    
    private static String getPlantUMLRelationSymbol(String relationshipType) {
        switch (relationshipType) {
            case "1:1": return "||--||";
            case "1:N": return "||--o{";
            case "M:1": return "}o--||";
            case "M:N": return "}o--o{";
            default: return "}o--||";
        }
    }
    
    private static void generateMarkdownReport(Map<String, TableInfo> tables, List<TableRelation> relations, 
                                             String outputDir, String domain) throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("# ").append(domain).append(" Domain Request-based ER Diagram Report\n\n");
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
        
        // Table details
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
        
        // Relations
        report.append("## Table Relations\n\n");
        report.append("| Source Table | Target Table | Source Field | Target Field | Relationship | Join Condition |\n");
        report.append("|--------------|--------------|--------------|--------------|--------------|----------------|\n");
        
        for (TableRelation relation : relations) {
            report.append("| ").append(relation.sourceTable)
                  .append(" | ").append(relation.targetTable)
                  .append(" | ").append(relation.sourceField)
                  .append(" | ").append(relation.targetField)
                  .append(" | ").append(relation.relationshipType)
                  .append(" | ").append(relation.joinCondition)
                  .append(" |\n");
        }
        
        report.append("\n## Files Generated\n\n");
        report.append("- `").append(domain).append("-request-based-er-diagram.mermaid` - Mermaid ER diagram (Request-based)\n");
        report.append("- `").append(domain).append("-request-based-er-diagram.puml` - PlantUML ER diagram (Request-based)\n");
        report.append("- `").append(domain).append("-request-based-er-report.md` - This comprehensive report\n");
        
        String filename = outputDir + "/" + domain + "-request-based-er-report.md";
        Files.write(Paths.get(filename), report.toString().getBytes());
        System.out.println("Generated: " + filename);
    }
}
