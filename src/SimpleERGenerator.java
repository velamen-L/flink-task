import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;

/**
 * Simple ER Diagram Generator
 * Extracts table relationships from SQL and generates ER diagrams
 */
public class SimpleERGenerator {
    
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println("Usage: java SimpleERGenerator <sql-file> [domain] [output-dir]");
                return;
            }
            
            String sqlFile = args[0];
            String domain = args.length > 1 ? args[1] : "default";
            String outputDir = args.length > 2 ? args[2] : "er-diagram";
            
            System.out.println("=== ER Diagram Generator ===");
            System.out.println("SQL File: " + sqlFile);
            System.out.println("Domain: " + domain);
            System.out.println("Output: " + outputDir);
            System.out.println();
            
            // Create output directory
            Files.createDirectories(Paths.get(outputDir));
            
            // Read SQL file
            String sql = readFile(sqlFile);
            
            // Extract table relations
            List<TableRelation> relations = extractTableRelations(sql);
            System.out.println("Found " + relations.size() + " table relations");
            
            // Generate outputs
            generateMermaidER(relations, outputDir, domain);
            generatePlantUMLER(relations, outputDir, domain);
            generateMarkdownReport(relations, outputDir, domain);
            
            System.out.println("\nER Diagram generated successfully!");
            System.out.println("Output directory: " + outputDir);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static class TableRelation {
        String sourceTable;
        String targetTable;
        String joinType;
        String condition;
        String sourceColumn;
        String targetColumn;
        
        TableRelation(String sourceTable, String targetTable, String joinType, String condition) {
            this.sourceTable = cleanTableName(sourceTable);
            this.targetTable = cleanTableName(targetTable);
            this.joinType = joinType != null ? joinType.toUpperCase() : "INNER";
            this.condition = condition;
            extractColumns();
        }
        
        private void extractColumns() {
            if (condition != null) {
                // Extract columns from condition: table1.field1 = table2.field2
                Pattern pattern = Pattern.compile("(\\w+)\\.(\\w+)\\s*=\\s*(\\w+)\\.(\\w+)");
                Matcher matcher = pattern.matcher(condition);
                if (matcher.find()) {
                    String table1 = matcher.group(1);
                    String field1 = matcher.group(2);
                    String table2 = matcher.group(3);
                    String field2 = matcher.group(4);
                    
                    if (table1.equals(sourceTable) || table1.contains(sourceTable)) {
                        this.sourceColumn = field1;
                        this.targetColumn = field2;
                    } else {
                        this.sourceColumn = field2;
                        this.targetColumn = field1;
                    }
                }
            }
        }
        
        @Override
        public String toString() {
            return String.format("%s --%s--> %s (%s -> %s)", 
                sourceTable, joinType, targetTable, 
                sourceColumn != null ? sourceColumn : "?", 
                targetColumn != null ? targetColumn : "?");
        }
    }
    
    private static List<TableRelation> extractTableRelations(String sql) {
        List<TableRelation> relations = new ArrayList<>();
        
        // Extract tables from FROM clause first
        Set<String> allTables = extractAllTables(sql);
        System.out.println("All tables found: " + allTables);
        
        // Extract JOIN statements with enhanced pattern for Flink SQL
        Pattern joinPattern = Pattern.compile(
            "(LEFT|RIGHT|INNER|FULL)?\\s*JOIN\\s+" +
            "([`\"\\w\\.]+)" +  // table name with possible schema and quotes
            "(?:\\s+FOR\\s+SYSTEM_TIME\\s+AS\\s+OF\\s+PROCTIME\\(\\))?" +  // Flink temporal join
            "(?:\\s+(\\w+))?" +  // optional alias
            "\\s+ON\\s+" +
            "([^\\n]+)",  // condition
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = joinPattern.matcher(sql);
        
        while (matcher.find()) {
            String joinType = matcher.group(1);
            String targetTable = matcher.group(2);
            String alias = matcher.group(3);
            String condition = matcher.group(4).trim();
            
            // Find source table from FROM clause
            String sourceTable = extractSourceTable(sql, matcher.start());
            
            if (sourceTable != null && targetTable != null) {
                TableRelation relation = new TableRelation(sourceTable, targetTable, joinType, condition);
                relations.add(relation);
                System.out.println("Found JOIN: " + relation);
            }
        }
        
        // Also extract implicit relations from JSON_VALUE patterns
        relations.addAll(extractImplicitRelations(sql, allTables));
        
        return relations;
    }
    
    private static String extractSourceTable(String sql, int joinPosition) {
        // Find the nearest FROM clause before the JOIN
        String beforeJoin = sql.substring(0, joinPosition);
        Pattern fromPattern = Pattern.compile("FROM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = fromPattern.matcher(beforeJoin);
        
        String lastTable = null;
        while (matcher.find()) {
            lastTable = matcher.group(1);
        }
        
        return lastTable;
    }
    
    private static String cleanTableName(String tableName) {
        if (tableName == null) return null;
        
        // Remove schema prefix and special characters
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.");
            tableName = parts[parts.length - 1];
        }
        return tableName.replaceAll("[`\"\\[\\]]", "");
    }
    
    private static void generateMermaidER(List<TableRelation> relations, String outputDir, String domain) throws IOException {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("erDiagram\n");
        mermaid.append("    %% ").append(domain).append(" Domain ER Diagram\n");
        mermaid.append("    %% Generated: ").append(new Date()).append("\n");
        mermaid.append("    %% Relations: ").append(relations.size()).append("\n\n");
        
        // Get all tables
        Set<String> allTables = new HashSet<>();
        for (TableRelation rel : relations) {
            allTables.add(rel.sourceTable);
            allTables.add(rel.targetTable);
        }
        
        // Add table definitions
        for (String table : allTables) {
            mermaid.append("    ").append(table).append(" {\n");
            mermaid.append("        bigint id PK\n");
            mermaid.append("        string name\n");
            mermaid.append("        timestamp create_time\n");
            mermaid.append("    }\n\n");
        }
        
        // Add relationships
        for (TableRelation rel : relations) {
            String symbol = getRelationSymbol(rel.joinType);
            mermaid.append("    ")
                   .append(rel.sourceTable)
                   .append(" ")
                   .append(symbol)
                   .append(" ")
                   .append(rel.targetTable)
                   .append(" : \"")
                   .append(rel.sourceColumn != null ? rel.sourceColumn : "id")
                   .append(" -> ")
                   .append(rel.targetColumn != null ? rel.targetColumn : "id")
                   .append("\"\n");
        }
        
        writeFile(outputDir + "/" + domain + "-er-diagram.mermaid", mermaid.toString());
    }
    
    private static void generatePlantUMLER(List<TableRelation> relations, String outputDir, String domain) throws IOException {
        StringBuilder plantuml = new StringBuilder();
        plantuml.append("@startuml\n");
        plantuml.append("!theme plain\n");
        plantuml.append("title ").append(domain).append(" Domain ER Diagram\n\n");
        
        // Get all tables
        Set<String> allTables = new HashSet<>();
        for (TableRelation rel : relations) {
            allTables.add(rel.sourceTable);
            allTables.add(rel.targetTable);
        }
        
        // Add entities
        for (String table : allTables) {
            plantuml.append("entity \"").append(table).append("\" as ").append(table).append(" {\n");
            plantuml.append("  * id : PK\n");
            plantuml.append("  --\n");
            plantuml.append("  name : VARCHAR\n");
            plantuml.append("  create_time : TIMESTAMP\n");
            plantuml.append("}\n\n");
        }
        
        // Add relationships
        for (TableRelation rel : relations) {
            plantuml.append(rel.sourceTable)
                   .append(" ||--o{ ")
                   .append(rel.targetTable)
                   .append(" : ")
                   .append(rel.joinType)
                   .append("\n");
        }
        
        plantuml.append("\n@enduml");
        writeFile(outputDir + "/" + domain + "-er-diagram.puml", plantuml.toString());
    }
    
    private static void generateMarkdownReport(List<TableRelation> relations, String outputDir, String domain) throws IOException {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(domain).append(" Domain ER Diagram Report\n\n");
        md.append("**Generated**: ").append(new Date()).append("\n\n");
        
        // Get all tables
        Set<String> allTables = new HashSet<>();
        for (TableRelation rel : relations) {
            allTables.add(rel.sourceTable);
            allTables.add(rel.targetTable);
        }
        
        md.append("## Statistics\n\n");
        md.append("- **Total Tables**: ").append(allTables.size()).append("\n");
        md.append("- **Total Relations**: ").append(relations.size()).append("\n\n");
        
        md.append("## Table Relations\n\n");
        md.append("| Source Table | Target Table | JOIN Type | Join Condition |\n");
        md.append("|--------------|--------------|-----------|----------------|\n");
        for (TableRelation rel : relations) {
            md.append("| ").append(rel.sourceTable)
              .append(" | ").append(rel.targetTable)
              .append(" | ").append(rel.joinType)
              .append(" | ").append(rel.condition != null ? rel.condition.substring(0, Math.min(50, rel.condition.length())) + "..." : "N/A")
              .append(" |\n");
        }
        
        md.append("\n## Table List\n\n");
        for (String table : allTables) {
            md.append("- ").append(table).append("\n");
        }
        
        md.append("\n## Files Generated\n\n");
        md.append("- `").append(domain).append("-er-diagram.mermaid` - Mermaid ER diagram\n");
        md.append("- `").append(domain).append("-er-diagram.puml` - PlantUML ER diagram\n");
        md.append("- `").append(domain).append("-er-report.md` - This report\n");
        
        writeFile(outputDir + "/" + domain + "-er-report.md", md.toString());
    }
    
    private static String getRelationSymbol(String joinType) {
        if (joinType == null) return "||--||";
        
        switch (joinType.toUpperCase()) {
            case "LEFT": return "||--o{";
            case "RIGHT": return "}o--||";
            case "INNER": return "||--||";
            case "FULL": return "}o--o{";
            default: return "||--||";
        }
    }
    
    private static String readFile(String filename) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filename)));
    }
    
    private static void writeFile(String filename, String content) throws IOException {
        Files.write(Paths.get(filename), content.getBytes());
        System.out.println("Generated: " + filename);
    }
    
    private static Set<String> extractAllTables(String sql) {
        Set<String> tables = new HashSet<>();
        
        // Extract from FROM clause
        Pattern fromPattern = Pattern.compile(
            "FROM\\s+([`\"\\w\\.]+)(?:\\s+(\\w+))?", 
            Pattern.CASE_INSENSITIVE
        );
        Matcher fromMatcher = fromPattern.matcher(sql);
        while (fromMatcher.find()) {
            String tableName = cleanTableName(fromMatcher.group(1));
            if (tableName != null) {
                tables.add(tableName);
            }
        }
        
        // Extract from JOIN clauses
        Pattern joinPattern = Pattern.compile(
            "JOIN\\s+([`\"\\w\\.]+)(?:\\s+FOR\\s+SYSTEM_TIME\\s+AS\\s+OF\\s+PROCTIME\\(\\))?(?:\\s+(\\w+))?", 
            Pattern.CASE_INSENSITIVE
        );
        Matcher joinMatcher = joinPattern.matcher(sql);
        while (joinMatcher.find()) {
            String tableName = cleanTableName(joinMatcher.group(1));
            if (tableName != null) {
                tables.add(tableName);
            }
        }
        
        // Extract from INSERT INTO
        Pattern insertPattern = Pattern.compile(
            "INSERT\\s+INTO\\s+([`\"\\w\\.]+)", 
            Pattern.CASE_INSENSITIVE
        );
        Matcher insertMatcher = insertPattern.matcher(sql);
        while (insertMatcher.find()) {
            String tableName = cleanTableName(insertMatcher.group(1));
            if (tableName != null) {
                tables.add(tableName);
            }
        }
        
        return tables;
    }
    
    private static List<TableRelation> extractImplicitRelations(String sql, Set<String> allTables) {
        List<TableRelation> relations = new ArrayList<>();
        
        // Find main source table (from FROM clause)
        String mainTable = null;
        Pattern fromPattern = Pattern.compile("FROM\\s+([`\"\\w\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher fromMatcher = fromPattern.matcher(sql);
        if (fromMatcher.find()) {
            mainTable = cleanTableName(fromMatcher.group(1));
        }
        
        if (mainTable != null) {
            // Look for foreign key patterns in JSON_VALUE or direct references
            Pattern fkPattern = Pattern.compile(
                "JSON_VALUE\\([^,]+,\\s*'\\$\\.(\\w*[iI]d)'\\)|" +  // JSON_VALUE(payload, '$.userId')
                "(\\w+)\\.(\\w*[iI]d)",  // direct field references like wqr.id
                Pattern.CASE_INSENSITIVE
            );
            
            Matcher fkMatcher = fkPattern.matcher(sql);
            Set<String> foundRelations = new HashSet<>();
            
            while (fkMatcher.find()) {
                String fieldName = fkMatcher.group(1) != null ? fkMatcher.group(1) : fkMatcher.group(3);
                
                if (fieldName != null && fieldName.toLowerCase().contains("id")) {
                    // Try to infer target table from field name
                    String inferredTable = inferTableFromField(fieldName, allTables);
                    if (inferredTable != null && !inferredTable.equals(mainTable)) {
                        String relationKey = mainTable + "->" + inferredTable;
                        if (!foundRelations.contains(relationKey)) {
                            TableRelation relation = new TableRelation(
                                mainTable, 
                                inferredTable, 
                                "INFERRED", 
                                "Inferred from " + fieldName
                            );
                            relations.add(relation);
                            foundRelations.add(relationKey);
                            System.out.println("Found INFERRED relation: " + relation);
                        }
                    }
                }
            }
        }
        
        return relations;
    }
    
    private static String inferTableFromField(String fieldName, Set<String> allTables) {
        String lowerField = fieldName.toLowerCase();
        
        // Remove 'id' suffix to get potential table name
        String baseName = lowerField.replaceAll("_?id$", "");
        
        // Try to match with existing tables
        for (String table : allTables) {
            String lowerTable = table.toLowerCase();
            if (lowerTable.contains(baseName) || baseName.contains(lowerTable)) {
                return table;
            }
        }
        
        // Common mappings
        Map<String, String> commonMappings = Map.of(
            "user", "user_info",
            "pattern", "tower_pattern", 
            "question", "question_record",
            "teaching", "teaching_type"
        );
        
        for (Map.Entry<String, String> entry : commonMappings.entrySet()) {
            if (baseName.contains(entry.getKey())) {
                for (String table : allTables) {
                    if (table.toLowerCase().contains(entry.getValue())) {
                        return table;
                    }
                }
            }
        }
        
        return null;
    }
}
