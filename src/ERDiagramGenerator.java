import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;

/**
 * ER Diagram Generator - Standalone Tool
 * 
 * Usage:
 * gradle -b build-er-diagram.gradle generateERDiagram -PsqlFile=job/wrongbook/sql/wrongbook_fix_wide_table_v2.sql
 */
public class ERDiagramGenerator {
    
    private static final String CURRENT_ER_FILE = "er-diagram/current-er.md";
    private static final String CONFLICTS_FILE = "er-diagram/conflicts.md";
    
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println("用法: java ERDiagramGenerator <sql-file> [--domain <domain>] [--output <output-dir>]");
                return;
            }
            
            String sqlFile = args[0];
            String domain = getArgValue(args, "--domain", "default");
            String outputDir = getArgValue(args, "--output", "er-diagram");
            
            System.out.println("📊 开始生成ER图...");
            System.out.println("SQL文件: " + sqlFile);
            System.out.println("业务域: " + domain);
            System.out.println("输出目录: " + outputDir);
            
            // 创建输出目录
            Files.createDirectories(Paths.get(outputDir));
            
            // 读取SQL文件
            String sql = readFile(sqlFile);
            
            // 提取表关联
            List<TableRelation> relations = extractTableRelations(sql);
            System.out.println("✅ 提取到 " + relations.size() + " 个表关联");
            
            // 读取现有ER图
            ERDiagram currentER = loadCurrentERDiagram();
            
            // 检测冲突
            List<Conflict> conflicts = detectConflicts(relations, currentER);
            if (!conflicts.isEmpty()) {
                System.out.println("⚠️ 检测到 " + conflicts.size() + " 个冲突");
                saveConflicts(conflicts, outputDir);
            } else {
                System.out.println("✅ 无冲突检测");
            }
            
            // 合并到总ER图
            ERDiagram updatedER = mergeERDiagram(currentER, relations, domain);
            
            // 生成输出文件
            generateMermaidER(updatedER, outputDir, domain);
            generatePlantUMLER(updatedER, outputDir, domain);
            generateMarkdownReport(updatedER, relations, conflicts, outputDir, domain);
            
            // 保存当前ER图
            saveCurrentERDiagram(updatedER);
            
            System.out.println("🎉 ER图生成完成！");
            System.out.println("📁 输出目录: " + outputDir);
            
        } catch (Exception e) {
            System.err.println("❌ 生成失败: " + e.getMessage());
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
        String extractedFrom;
        
        TableRelation(String sourceTable, String targetTable, String joinType, String condition) {
            this.sourceTable = sourceTable;
            this.targetTable = targetTable;
            this.joinType = joinType;
            this.condition = condition;
            this.extractedFrom = "SQL_JOIN";
            extractColumns();
        }
        
        private void extractColumns() {
            if (condition != null) {
                // 简单的字段提取: table1.field1 = table2.field2
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
                sourceTable, joinType, targetTable, sourceColumn, targetColumn);
        }
    }
    
    static class ERDiagram {
        List<TableRelation> relations = new ArrayList<>();
        Set<String> tables = new HashSet<>();
        String version = "1.0";
        long lastUpdated = System.currentTimeMillis();
        
        void addRelation(TableRelation relation) {
            relations.add(relation);
            tables.add(relation.sourceTable);
            tables.add(relation.targetTable);
        }
    }
    
    static class Conflict {
        String type;
        TableRelation newRelation;
        TableRelation existingRelation;
        String description;
        String suggestion;
        
        Conflict(String type, TableRelation newRel, TableRelation existingRel, String desc) {
            this.type = type;
            this.newRelation = newRel;
            this.existingRelation = existingRel;
            this.description = desc;
            this.suggestion = generateSuggestion();
        }
        
        private String generateSuggestion() {
            switch (type) {
                case "JOIN_TYPE_CONFLICT":
                    return "建议检查JOIN类型，确认业务逻辑是否一致";
                case "CONDITION_CONFLICT":
                    return "建议检查JOIN条件，可能存在不同的关联逻辑";
                default:
                    return "建议人工审核冲突";
            }
        }
    }
    
    // 核心方法实现
    
    private static List<TableRelation> extractTableRelations(String sql) {
        List<TableRelation> relations = new ArrayList<>();
        
        // 提取JOIN语句
        Pattern joinPattern = Pattern.compile(
            "(LEFT|RIGHT|INNER|FULL)?\\s*JOIN\\s+(\\w+)(?:\\s+(?:FOR\\s+SYSTEM_TIME\\s+AS\\s+OF\\s+PROCTIME\\(\\))?\\s+(\\w+))?\\s+ON\\s+([^\\n]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = joinPattern.matcher(sql);
        
        while (matcher.find()) {
            String joinType = matcher.group(1) != null ? matcher.group(1) : "INNER";
            String targetTable = matcher.group(2);
            String alias = matcher.group(3);
            String condition = matcher.group(4).trim();
            
            // 查找源表（通过FROM子句）
            String sourceTable = extractSourceTable(sql, matcher.start());
            
            if (sourceTable != null && targetTable != null) {
                // 清理表名（去掉别名等）
                targetTable = cleanTableName(targetTable);
                sourceTable = cleanTableName(sourceTable);
                
                TableRelation relation = new TableRelation(sourceTable, targetTable, joinType, condition);
                relations.add(relation);
                
                System.out.println("🔗 发现关联: " + relation);
            }
        }
        
        return relations;
    }
    
    private static String extractSourceTable(String sql, int joinPosition) {
        // 向前查找FROM子句
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
        // 去掉schema前缀和特殊字符
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.");
            tableName = parts[parts.length - 1];
        }
        return tableName.replaceAll("[`\"\\[\\]]", "");
    }
    
    private static ERDiagram loadCurrentERDiagram() {
        ERDiagram diagram = new ERDiagram();
        
        try {
            if (Files.exists(Paths.get(CURRENT_ER_FILE))) {
                // 简化：如果文件存在，读取已有关联
                String content = readFile(CURRENT_ER_FILE);
                // 这里可以解析已有的关联，简化版本暂时返回空
            }
        } catch (Exception e) {
            System.out.println("⚠️ 无法读取现有ER图，创建新的");
        }
        
        return diagram;
    }
    
    private static List<Conflict> detectConflicts(List<TableRelation> newRelations, ERDiagram currentER) {
        List<Conflict> conflicts = new ArrayList<>();
        
        for (TableRelation newRel : newRelations) {
            for (TableRelation existingRel : currentER.relations) {
                if (isSameTablePair(newRel, existingRel)) {
                    // 检查JOIN类型冲突
                    if (!newRel.joinType.equals(existingRel.joinType)) {
                        conflicts.add(new Conflict("JOIN_TYPE_CONFLICT", newRel, existingRel,
                            "JOIN类型不一致: " + newRel.joinType + " vs " + existingRel.joinType));
                    }
                    
                    // 检查条件冲突
                    if (!Objects.equals(newRel.condition, existingRel.condition)) {
                        conflicts.add(new Conflict("CONDITION_CONFLICT", newRel, existingRel,
                            "JOIN条件不一致"));
                    }
                }
            }
        }
        
        return conflicts;
    }
    
    private static boolean isSameTablePair(TableRelation r1, TableRelation r2) {
        return (r1.sourceTable.equals(r2.sourceTable) && r1.targetTable.equals(r2.targetTable)) ||
               (r1.sourceTable.equals(r2.targetTable) && r1.targetTable.equals(r2.sourceTable));
    }
    
    private static ERDiagram mergeERDiagram(ERDiagram current, List<TableRelation> newRelations, String domain) {
        ERDiagram merged = new ERDiagram();
        merged.relations.addAll(current.relations);
        merged.tables.addAll(current.tables);
        
        // 添加新关联
        for (TableRelation newRel : newRelations) {
            boolean exists = current.relations.stream()
                .anyMatch(existing -> isSameTablePair(newRel, existing));
            
            if (!exists) {
                merged.addRelation(newRel);
                System.out.println("➕ 新增关联: " + newRel);
            }
        }
        
        merged.lastUpdated = System.currentTimeMillis();
        return merged;
    }
    
    private static void generateMermaidER(ERDiagram er, String outputDir, String domain) throws IOException {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("erDiagram\n");
        mermaid.append("    %% ").append(domain).append(" 业务域 ER图\n");
        mermaid.append("    %% 生成时间: ").append(new Date()).append("\n");
        mermaid.append("    %% 总表数: ").append(er.tables.size()).append("\n");
        mermaid.append("    %% 总关联数: ").append(er.relations.size()).append("\n\n");
        
        // 添加关联关系
        for (TableRelation rel : er.relations) {
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
    
    private static void generatePlantUMLER(ERDiagram er, String outputDir, String domain) throws IOException {
        StringBuilder plantuml = new StringBuilder();
        plantuml.append("@startuml\n");
        plantuml.append("!theme plain\n");
        plantuml.append("title ").append(domain).append(" 业务域 ER图\n\n");
        
        // 添加实体
        for (String table : er.tables) {
            plantuml.append("entity \"").append(table).append("\" as ").append(table).append(" {\n");
            plantuml.append("  * id : PK\n");
            plantuml.append("  --\n");
            plantuml.append("  其他字段...\n");
            plantuml.append("}\n\n");
        }
        
        // 添加关系
        for (TableRelation rel : er.relations) {
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
    
    private static void generateMarkdownReport(ERDiagram er, List<TableRelation> newRelations, 
                                             List<Conflict> conflicts, String outputDir, String domain) throws IOException {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(domain).append(" 业务域 ER图报告\n\n");
        md.append("**生成时间**: ").append(new Date()).append("\n\n");
        
        md.append("## 📊 统计信息\n\n");
        md.append("- **总表数**: ").append(er.tables.size()).append("\n");
        md.append("- **总关联数**: ").append(er.relations.size()).append("\n");
        md.append("- **本次新增**: ").append(newRelations.size()).append("\n");
        md.append("- **检测冲突**: ").append(conflicts.size()).append("\n\n");
        
        md.append("## 🔗 表关联关系\n\n");
        md.append("| 源表 | 目标表 | JOIN类型 | 关联字段 | 提取来源 |\n");
        md.append("|------|--------|----------|----------|----------|\n");
        for (TableRelation rel : er.relations) {
            md.append("| ").append(rel.sourceTable)
              .append(" | ").append(rel.targetTable)
              .append(" | ").append(rel.joinType)
              .append(" | ").append(rel.sourceColumn != null ? rel.sourceColumn + " -> " + rel.targetColumn : "N/A")
              .append(" | ").append(rel.extractedFrom)
              .append(" |\n");
        }
        
        if (!conflicts.isEmpty()) {
            md.append("\n## ⚠️ 冲突报告\n\n");
            for (Conflict conflict : conflicts) {
                md.append("### ").append(conflict.type).append("\n\n");
                md.append("**描述**: ").append(conflict.description).append("\n\n");
                md.append("**建议**: ").append(conflict.suggestion).append("\n\n");
                md.append("**新关联**: ").append(conflict.newRelation.toString()).append("\n\n");
                md.append("**现有关联**: ").append(conflict.existingRelation.toString()).append("\n\n");
                md.append("---\n\n");
            }
        }
        
        md.append("## 📋 表清单\n\n");
        for (String table : er.tables) {
            md.append("- ").append(table).append("\n");
        }
        
        writeFile(outputDir + "/" + domain + "-er-report.md", md.toString());
    }
    
    // 工具方法
    
    private static String getArgValue(String[] args, String flag, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
    
    private static String readFile(String filename) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filename)));
    }
    
    private static void writeFile(String filename, String content) throws IOException {
        Files.write(Paths.get(filename), content.getBytes());
        System.out.println("📄 生成文件: " + filename);
    }
    
    private static String getRelationSymbol(String joinType) {
        switch (joinType.toUpperCase()) {
            case "LEFT": return "||--o{";
            case "RIGHT": return "}o--||";
            case "INNER": return "||--||";
            case "FULL": return "}o--o{";
            default: return "||--||";
        }
    }
    
    private static void saveCurrentERDiagram(ERDiagram er) throws IOException {
        // 简化保存到文件
        StringBuilder content = new StringBuilder();
        content.append("# 当前ER图状态\n\n");
        content.append("版本: ").append(er.version).append("\n");
        content.append("更新时间: ").append(new Date(er.lastUpdated)).append("\n\n");
        
        for (TableRelation rel : er.relations) {
            content.append(rel.toString()).append("\n");
        }
        
        Files.createDirectories(Paths.get("er-diagram"));
        writeFile(CURRENT_ER_FILE, content.toString());
    }
    
    private static void saveConflicts(List<Conflict> conflicts, String outputDir) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("# ER图冲突报告\n\n");
        content.append("检测时间: ").append(new Date()).append("\n");
        content.append("冲突数量: ").append(conflicts.size()).append("\n\n");
        
        for (Conflict conflict : conflicts) {
            content.append("## ").append(conflict.type).append("\n\n");
            content.append("**描述**: ").append(conflict.description).append("\n\n");
            content.append("**建议**: ").append(conflict.suggestion).append("\n\n");
            content.append("---\n\n");
        }
        
        writeFile(outputDir + "/conflicts.md", content.toString());
    }
}
