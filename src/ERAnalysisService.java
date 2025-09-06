import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ERAnalysisService {
    
    private static final String ER_KNOWLEDGE_BASE_FILE = "er-knowledge-base.md";
    private static final String ER_CONFLICTS_FILE = "er-conflicts.md";
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ERAnalysisService <request-file> <domain>");
            System.exit(1);
        }
        
        String requestFile = args[0];
        String domain = args[1];
        
        try {
            ERAnalysisService service = new ERAnalysisService();
            ERAnalysisResult result = service.analyzeAndUpdate(requestFile, domain);
            
            System.out.println("=== ER Analysis Complete ===");
            System.out.println("Entities found: " + result.entities.size());
            System.out.println("Relationships found: " + result.relationships.size());
            System.out.println("Conflicts detected: " + result.conflicts.size());
            
            if (!result.conflicts.isEmpty()) {
                System.out.println("\n=== CONFLICTS DETECTED ===");
                for (String conflict : result.conflicts) {
                    System.out.println("- " + conflict);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error during ER analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public ERAnalysisResult analyzeAndUpdate(String requestFile, String domain) throws IOException {
        // 1. Parse input file
        String content = new String(Files.readAllBytes(Paths.get(requestFile)));
        ERModel currentModel = parseERModel(content, domain);
        
        // 2. Load existing knowledge base
        ERKnowledgeBase knowledgeBase = loadKnowledgeBase();
        
        // 3. Detect conflicts
        List<String> conflicts = detectConflicts(currentModel, knowledgeBase);
        
        // 4. Update knowledge base
        updateKnowledgeBase(currentModel, knowledgeBase);
        
        // 5. Generate enhanced ER diagram
        generateEnhancedERDiagram(currentModel, domain);
        
        return new ERAnalysisResult(currentModel.entities, currentModel.relationships, conflicts);
    }
    
    private ERModel parseERModel(String content, String domain) {
        ERModel model = new ERModel();
        
        // Extract source table from payload
        extractSourceTableFromPayload(content, model, domain);
        
        // Extract dimension tables
        extractDimensionTables(content, model);
        
        // Extract result table
        extractResultTable(content, model);
        
        // Extract relationships
        extractRelationships(content, model);
        
        return model;
    }
    
    private void extractSourceTableFromPayload(String content, ERModel model, String domain) {
        try {
            String eventType = extractEventType(content);
            String sourceTableName = domain + "_" + eventType;
            
            Pattern payloadPattern = Pattern.compile(
                "public class (\\w+Payload)\\s*\\{([^}]+)\\}", 
                Pattern.DOTALL
            );
            Matcher matcher = payloadPattern.matcher(content);
            
            if (matcher.find()) {
                String payloadClass = matcher.group(1);
                String payloadFields = matcher.group(2);
                
                EREntity sourceEntity = new EREntity(sourceTableName, "source");
                parseJavaFields(payloadFields, sourceEntity);
                
                model.entities.put(sourceTableName, sourceEntity);
            }
        } catch (Exception e) {
            System.err.println("Error extracting source table: " + e.getMessage());
        }
    }
    
    private void extractDimensionTables(String content, ERModel model) {
        Pattern tablePattern = Pattern.compile(
            "CREATE TABLE[^`]*`([^`]+)`[^(]*\\(([^)]+WITH[^)]+)\\)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = tablePattern.matcher(content);
        while (matcher.find()) {
            String fullTableName = matcher.group(1);
            String tableDefinition = matcher.group(2);
            
            String tableName = fullTableName.substring(fullTableName.lastIndexOf('.') + 1);
            
            if (!isResultTable(tableName, content)) {
                EREntity entity = new EREntity(tableName, "dimension");
                parseCreateTableFields(tableDefinition, entity);
                model.entities.put(tableName, entity);
            }
        }
    }
    
    private void extractResultTable(String content, ERModel model) {
        Pattern resultPattern = Pattern.compile(
            "## 🎯 结果表配置.*?CREATE TABLE[^`]*`([^`]+)`[^(]*\\(([^)]+WITH[^)]+)\\)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = resultPattern.matcher(content);
        if (matcher.find()) {
            String fullTableName = matcher.group(1);
            String tableDefinition = matcher.group(2);
            
            String tableName = fullTableName.substring(fullTableName.lastIndexOf('.') + 1);
            EREntity entity = new EREntity(tableName, "result");
            parseCreateTableFields(tableDefinition, entity);
            model.entities.put(tableName, entity);
        }
    }
    
    private void extractRelationships(String content, ERModel model) {
        Pattern relationPattern = Pattern.compile(
            "- \\*\\*关联条件\\*\\*: (.+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = relationPattern.matcher(content);
        while (matcher.find()) {
            String condition = matcher.group(1).trim();
            ERRelationship relationship = parseJoinCondition(condition);
            if (relationship != null) {
                model.relationships.add(relationship);
            }
        }
    }
    
    private ERRelationship parseJoinCondition(String condition) {
        Pattern pattern = Pattern.compile("(\\w+)\\.(\\w+)\\s*=\\s*(\\w+)\\.(\\w+)");
        Matcher matcher = pattern.matcher(condition);
        
        if (matcher.find()) {
            String sourceTable = matcher.group(1);
            String sourceField = matcher.group(2);
            String targetTable = matcher.group(3);
            String targetField = matcher.group(4);
            
            return new ERRelationship(
                sourceTable, sourceField,
                targetTable, targetField,
                "many-to-one", condition
            );
        }
        return null;
    }
    
    private void parseJavaFields(String fieldsText, EREntity entity) {
        Pattern fieldPattern = Pattern.compile("private\\s+(\\w+)\\s+(\\w+);");
        Matcher matcher = fieldPattern.matcher(fieldsText);
        
        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            
            ERField field = new ERField(name, mapJavaTypeToSQL(type));
            if (name.equalsIgnoreCase("id")) {
                field.isPrimaryKey = true;
            }
            if (name.toLowerCase().contains("id") && !name.equalsIgnoreCase("id")) {
                field.isForeignKey = true;
            }
            
            entity.fields.add(field);
        }
    }
    
    private void parseCreateTableFields(String tableDefinition, EREntity entity) {
        Pattern fieldPattern = Pattern.compile("`(\\w+)`\\s+(\\w+[^,\n]*)[,\n]");
        Matcher matcher = fieldPattern.matcher(tableDefinition);
        
        while (matcher.find()) {
            String name = matcher.group(1);
            String type = matcher.group(2).trim();
            
            if (name.equals("PRIMARY") || name.equals("COMMENT") || name.equals("WITH")) {
                continue;
            }
            
            ERField field = new ERField(name, type);
            
            if (tableDefinition.contains("PRIMARY KEY (" + name + ")")) {
                field.isPrimaryKey = true;
            }
            if (name.toLowerCase().contains("id") && !name.equalsIgnoreCase("id")) {
                field.isForeignKey = true;
            }
            
            entity.fields.add(field);
        }
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
    
    private String extractEventType(String content) {
        Pattern pattern = Pattern.compile("event_type:\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String eventType = matcher.group(1);
            if (eventType.contains("_")) {
                String[] parts = eventType.split("_");
                return parts[parts.length - 1];
            }
            return eventType;
        }
        return "event";
    }
    
    private boolean isResultTable(String tableName, String content) {
        return content.toLowerCase().contains("结果表配置") && 
               content.toLowerCase().contains(tableName.toLowerCase());
    }
    
    private ERKnowledgeBase loadKnowledgeBase() {
        ERKnowledgeBase kb = new ERKnowledgeBase();
        
        try {
            if (Files.exists(Paths.get(ER_KNOWLEDGE_BASE_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(ER_KNOWLEDGE_BASE_FILE)));
                parseKnowledgeBase(content, kb);
            }
        } catch (IOException e) {
            System.err.println("Error loading knowledge base: " + e.getMessage());
        }
        
        return kb;
    }
    
    private void parseKnowledgeBase(String content, ERKnowledgeBase kb) {
        // Parse existing entities and relationships from knowledge base
        Pattern entityPattern = Pattern.compile(
            "### (\\w+) \\((\\w+)\\).*?Fields:(.*?)(?=###|$)",
            Pattern.DOTALL
        );
        
        Matcher matcher = entityPattern.matcher(content);
        while (matcher.find()) {
            String entityName = matcher.group(1);
            String entityType = matcher.group(2);
            String fieldsText = matcher.group(3);
            
            EREntity entity = new EREntity(entityName, entityType);
            parseKnowledgeBaseFields(fieldsText, entity);
            kb.entities.put(entityName, entity);
        }
    }
    
    private void parseKnowledgeBaseFields(String fieldsText, EREntity entity) {
        Pattern fieldPattern = Pattern.compile("- (\\w+) \\((\\w+)\\)(.*)");
        Matcher matcher = fieldPattern.matcher(fieldsText);
        
        while (matcher.find()) {
            String name = matcher.group(1);
            String type = matcher.group(2);
            String attributes = matcher.group(3);
            
            ERField field = new ERField(name, type);
            field.isPrimaryKey = attributes.contains("PK");
            field.isForeignKey = attributes.contains("FK");
            
            entity.fields.add(field);
        }
    }
    
    private List<String> detectConflicts(ERModel currentModel, ERKnowledgeBase knowledgeBase) {
        List<String> conflicts = new ArrayList<>();
        
        for (Map.Entry<String, EREntity> entry : currentModel.entities.entrySet()) {
            String entityName = entry.getKey();
            EREntity currentEntity = entry.getValue();
            
            if (knowledgeBase.entities.containsKey(entityName)) {
                EREntity existingEntity = knowledgeBase.entities.get(entityName);
                List<String> entityConflicts = compareEntities(entityName, currentEntity, existingEntity);
                conflicts.addAll(entityConflicts);
            }
        }
        
        return conflicts;
    }
    
    private List<String> compareEntities(String entityName, EREntity current, EREntity existing) {
        List<String> conflicts = new ArrayList<>();
        
        Map<String, ERField> currentFields = new HashMap<>();
        Map<String, ERField> existingFields = new HashMap<>();
        
        for (ERField field : current.fields) {
            currentFields.put(field.name, field);
        }
        for (ERField field : existing.fields) {
            existingFields.put(field.name, field);
        }
        
        for (String fieldName : currentFields.keySet()) {
            if (existingFields.containsKey(fieldName)) {
                ERField currentField = currentFields.get(fieldName);
                ERField existingField = existingFields.get(fieldName);
                
                if (!currentField.type.equals(existingField.type)) {
                    conflicts.add(String.format(
                        "Field type conflict in %s.%s: current=%s, existing=%s",
                        entityName, fieldName, currentField.type, existingField.type
                    ));
                }
                
                if (currentField.isPrimaryKey != existingField.isPrimaryKey) {
                    conflicts.add(String.format(
                        "Primary key conflict in %s.%s: current=%s, existing=%s",
                        entityName, fieldName, currentField.isPrimaryKey, existingField.isPrimaryKey
                    ));
                }
            }
        }
        
        return conflicts;
    }
    
    private void updateKnowledgeBase(ERModel currentModel, ERKnowledgeBase knowledgeBase) throws IOException {
        // Merge current model into knowledge base
        for (Map.Entry<String, EREntity> entry : currentModel.entities.entrySet()) {
            String entityName = entry.getKey();
            EREntity currentEntity = entry.getValue();
            
            if (knowledgeBase.entities.containsKey(entityName)) {
                mergeEntities(knowledgeBase.entities.get(entityName), currentEntity);
            } else {
                knowledgeBase.entities.put(entityName, currentEntity);
            }
        }
        
        knowledgeBase.relationships.addAll(currentModel.relationships);
        
        saveKnowledgeBase(knowledgeBase);
    }
    
    private void mergeEntities(EREntity existing, EREntity current) {
        Map<String, ERField> existingFields = new HashMap<>();
        for (ERField field : existing.fields) {
            existingFields.put(field.name, field);
        }
        
        for (ERField currentField : current.fields) {
            if (!existingFields.containsKey(currentField.name)) {
                existing.fields.add(currentField);
            }
        }
    }
    
    private void saveKnowledgeBase(ERKnowledgeBase knowledgeBase) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# ER Knowledge Base\n\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        sb.append("## Entities\n\n");
        for (Map.Entry<String, EREntity> entry : knowledgeBase.entities.entrySet()) {
            EREntity entity = entry.getValue();
            sb.append("### ").append(entity.name).append(" (").append(entity.type).append(")\n\n");
            sb.append("Fields:\n");
            
            for (ERField field : entity.fields) {
                sb.append("- ").append(field.name).append(" (").append(field.type).append(")");
                if (field.isPrimaryKey) sb.append(" [PK]");
                if (field.isForeignKey) sb.append(" [FK]");
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        sb.append("## Relationships\n\n");
        for (ERRelationship rel : knowledgeBase.relationships) {
            sb.append("- ").append(rel.sourceTable).append(".").append(rel.sourceField)
              .append(" -> ").append(rel.targetTable).append(".").append(rel.targetField)
              .append(" (").append(rel.cardinality).append(")\n");
        }
        
        Files.write(Paths.get(ER_KNOWLEDGE_BASE_FILE), sb.toString().getBytes());
    }
    
    private void generateEnhancedERDiagram(ERModel model, String domain) throws IOException {
        generateMermaidDiagram(model, domain);
        generatePlantUMLDiagram(model, domain);
        generateMarkdownReport(model, domain);
    }
    
    private void generateMermaidDiagram(ERModel model, String domain) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("```mermaid\n");
        sb.append("erDiagram\n");
        
        for (EREntity entity : model.entities.values()) {
            if (entity.type.equals("result")) continue;
            
            sb.append("    ").append(entity.name).append(" {\n");
            for (ERField field : entity.fields) {
                sb.append("        ").append(field.type.toLowerCase()).append(" ").append(field.name);
                if (field.isPrimaryKey) sb.append(" PK");
                if (field.isForeignKey) sb.append(" FK");
                sb.append(" \"").append(field.name).append("\"\n");
            }
            sb.append("    }\n\n");
        }
        
        for (ERRelationship rel : model.relationships) {
            String relationship = getRelationshipSymbol(rel.cardinality);
            sb.append("    ").append(rel.sourceTable).append(" ")
              .append(relationship).append(" ").append(rel.targetTable)
              .append(" : \"").append(rel.condition).append("\"\n");
        }
        
        sb.append("```\n");
        
        String filename = "er-diagram/" + domain + "-enhanced-er-diagram.mermaid";
        Files.createDirectories(Paths.get("er-diagram"));
        Files.write(Paths.get(filename), sb.toString().getBytes());
        
        System.out.println("Enhanced Mermaid ER diagram generated: " + filename);
    }
    
    private void generatePlantUMLDiagram(ERModel model, String domain) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("!define TABLE(name,desc) entity name as \"desc\"\n");
        sb.append("!define COLUMN(name,type,desc) name : type desc\n\n");
        
        for (EREntity entity : model.entities.values()) {
            if (entity.type.equals("result")) continue;
            
            sb.append("entity ").append(entity.name).append(" {\n");
            for (ERField field : entity.fields) {
                sb.append("  ").append(field.name).append(" : ").append(field.type);
                if (field.isPrimaryKey) sb.append(" <<PK>>");
                if (field.isForeignKey) sb.append(" <<FK>>");
                sb.append("\n");
            }
            sb.append("}\n\n");
        }
        
        for (ERRelationship rel : model.relationships) {
            sb.append(rel.sourceTable).append(" }|--|| ").append(rel.targetTable).append("\n");
        }
        
        sb.append("@enduml\n");
        
        String filename = "er-diagram/" + domain + "-enhanced-er-diagram.puml";
        Files.write(Paths.get(filename), sb.toString().getBytes());
        
        System.out.println("Enhanced PlantUML ER diagram generated: " + filename);
    }
    
    private void generateMarkdownReport(ERModel model, String domain) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Enhanced ER Diagram Analysis Report - ").append(domain).append("\n\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        sb.append("## Summary\n\n");
        sb.append("- **Source Tables**: ").append(countEntitiesByType(model, "source")).append("\n");
        sb.append("- **Dimension Tables**: ").append(countEntitiesByType(model, "dimension")).append("\n");
        sb.append("- **Result Tables**: ").append(countEntitiesByType(model, "result")).append("\n");
        sb.append("- **Relationships**: ").append(model.relationships.size()).append("\n\n");
        
        sb.append("## Entity Details\n\n");
        for (EREntity entity : model.entities.values()) {
            sb.append("### ").append(entity.name).append(" (").append(entity.type).append(")\n\n");
            sb.append("| Field | Type | Constraints |\n");
            sb.append("|-------|------|-------------|\n");
            
            for (ERField field : entity.fields) {
                sb.append("| ").append(field.name).append(" | ").append(field.type).append(" | ");
                List<String> constraints = new ArrayList<>();
                if (field.isPrimaryKey) constraints.add("PK");
                if (field.isForeignKey) constraints.add("FK");
                sb.append(String.join(", ", constraints)).append(" |\n");
            }
            sb.append("\n");
        }
        
        sb.append("## Relationships\n\n");
        sb.append("| Source | Target | Cardinality | Condition |\n");
        sb.append("|--------|--------|-------------|------------|\n");
        
        for (ERRelationship rel : model.relationships) {
            sb.append("| ").append(rel.sourceTable).append(".").append(rel.sourceField)
              .append(" | ").append(rel.targetTable).append(".").append(rel.targetField)
              .append(" | ").append(rel.cardinality)
              .append(" | ").append(rel.condition).append(" |\n");
        }
        
        String filename = "er-diagram/" + domain + "-enhanced-er-analysis.md";
        Files.write(Paths.get(filename), sb.toString().getBytes());
        
        System.out.println("Enhanced ER analysis report generated: " + filename);
    }
    
    private int countEntitiesByType(ERModel model, String type) {
        return (int) model.entities.values().stream()
            .filter(entity -> entity.type.equals(type))
            .count();
    }
    
    private String getRelationshipSymbol(String cardinality) {
        switch (cardinality.toLowerCase()) {
            case "one-to-one": return "||--||";
            case "one-to-many": return "||--o{";
            case "many-to-one": return "}o--||";
            case "many-to-many": return "}o--o{";
            default: return "}o--||";
        }
    }
    
    // Data classes
    static class ERModel {
        Map<String, EREntity> entities = new HashMap<>();
        List<ERRelationship> relationships = new ArrayList<>();
    }
    
    static class EREntity {
        String name;
        String type;
        List<ERField> fields = new ArrayList<>();
        
        EREntity(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
    
    static class ERField {
        String name;
        String type;
        boolean isPrimaryKey = false;
        boolean isForeignKey = false;
        String references;
        
        ERField(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
    
    static class ERRelationship {
        String sourceTable;
        String sourceField;
        String targetTable;
        String targetField;
        String cardinality;
        String condition;
        
        ERRelationship(String sourceTable, String sourceField, String targetTable, 
                      String targetField, String cardinality, String condition) {
            this.sourceTable = sourceTable;
            this.sourceField = sourceField;
            this.targetTable = targetTable;
            this.targetField = targetField;
            this.cardinality = cardinality;
            this.condition = condition;
        }
    }
    
    static class ERKnowledgeBase {
        Map<String, EREntity> entities = new HashMap<>();
        List<ERRelationship> relationships = new ArrayList<>();
    }
    
    static class ERAnalysisResult {
        Map<String, EREntity> entities;
        List<ERRelationship> relationships;
        List<String> conflicts;
        
        ERAnalysisResult(Map<String, EREntity> entities, List<ERRelationship> relationships, List<String> conflicts) {
            this.entities = entities;
            this.relationships = relationships;
            this.conflicts = conflicts;
        }
    }
}
