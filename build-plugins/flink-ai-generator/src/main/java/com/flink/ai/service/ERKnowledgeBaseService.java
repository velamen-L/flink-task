package com.flink.ai.service;

import com.flink.ai.model.ERUpdateResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ERKnowledgeBaseService {
    
    public ERUpdateResult updateKnowledgeBase(File requestFile, File knowledgeBaseDir) throws IOException {
        ERUpdateResult result = new ERUpdateResult();
        
        // 创建知识库目录结构
        createKnowledgeBaseStructure(knowledgeBaseDir);
        
        // 解析请求文件中的ER信息
        String content = new String(Files.readAllBytes(requestFile.toPath()));
        
        // 模拟ER知识库更新过程
        result.setEntitiesAdded(3);
        result.setEntitiesUpdated(1);
        result.setRelationshipsAdded(4);
        
        // 生成增强ER图
        List<File> enhancedDiagrams = generateEnhancedDiagrams(knowledgeBaseDir, content);
        result.setEnhancedDiagrams(enhancedDiagrams);
        
        // 检查冲突（模拟）
        List<String> conflicts = detectConflicts(content);
        result.setConflicts(conflicts);
        
        // 更新知识库文件
        updateKnowledgeBaseFile(knowledgeBaseDir, result);
        
        return result;
    }
    
    private void createKnowledgeBaseStructure(File knowledgeBaseDir) throws IOException {
        knowledgeBaseDir.mkdirs();
        
        File diagramsDir = new File(knowledgeBaseDir, "diagrams");
        diagramsDir.mkdirs();
        
        File historyDir = new File(knowledgeBaseDir, "history");
        historyDir.mkdirs();
    }
    
    private List<File> generateEnhancedDiagrams(File knowledgeBaseDir, String content) throws IOException {
        List<File> diagrams = new ArrayList<>();
        
        File diagramsDir = new File(knowledgeBaseDir, "diagrams");
        
        // 生成Mermaid ER图
        File mermaidFile = new File(diagramsDir, "enhanced-er-diagram.mermaid");
        String mermaidContent = generateMermaidER(content);
        Files.write(mermaidFile.toPath(), mermaidContent.getBytes());
        diagrams.add(mermaidFile);
        
        // 生成PlantUML ER图
        File plantumlFile = new File(diagramsDir, "enhanced-er-diagram.puml");
        String plantumlContent = generatePlantUMLER(content);
        Files.write(plantumlFile.toPath(), plantumlContent.getBytes());
        diagrams.add(plantumlFile);
        
        // 生成分析报告
        File analysisFile = new File(diagramsDir, "er-analysis-report.md");
        String analysisContent = generateAnalysisReport(content);
        Files.write(analysisFile.toPath(), analysisContent.getBytes());
        diagrams.add(analysisFile);
        
        return diagrams;
    }
    
    private String generateMermaidER(String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("```mermaid\n");
        sb.append("erDiagram\n");
        sb.append("    wrongbook_fix {\n");
        sb.append("        string id PK \"修正记录ID\"\n");
        sb.append("        string originWrongRecordId FK \"原错题记录ID\"\n");
        sb.append("        string userId \"用户ID\"\n");
        sb.append("        integer result \"修正结果\"\n");
        sb.append("    }\n\n");
        sb.append("    wrong_question_record {\n");
        sb.append("        string id PK \"错题记录ID\"\n");
        sb.append("        string user_id \"用户ID\"\n");
        sb.append("        string pattern_id FK \"题型ID\"\n");
        sb.append("        string subject \"学科\"\n");
        sb.append("    }\n\n");
        sb.append("    tower_pattern {\n");
        sb.append("        string id PK \"题型ID\"\n");
        sb.append("        string name \"题型名称\"\n");
        sb.append("        string subject \"学科\"\n");
        sb.append("    }\n\n");
        sb.append("    wrongbook_fix }o--|| wrong_question_record : \"originWrongRecordId->id\"\n");
        sb.append("    wrong_question_record }o--|| tower_pattern : \"pattern_id->id\"\n");
        sb.append("```\n");
        return sb.toString();
    }
    
    private String generatePlantUMLER(String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("!theme plain\n\n");
        sb.append("entity wrongbook_fix {\n");
        sb.append("  * id : VARCHAR(255) <<PK>>\n");
        sb.append("  * originWrongRecordId : VARCHAR(255) <<FK>>\n");
        sb.append("  * userId : VARCHAR(255)\n");
        sb.append("  * result : INT\n");
        sb.append("}\n\n");
        sb.append("entity wrong_question_record {\n");
        sb.append("  * id : VARCHAR(255) <<PK>>\n");
        sb.append("  * user_id : VARCHAR(255)\n");
        sb.append("  * pattern_id : VARCHAR(255) <<FK>>\n");
        sb.append("  * subject : VARCHAR(255)\n");
        sb.append("}\n\n");
        sb.append("entity tower_pattern {\n");
        sb.append("  * id : VARCHAR(255) <<PK>>\n");
        sb.append("  * name : VARCHAR(255)\n");
        sb.append("  * subject : VARCHAR(255)\n");
        sb.append("}\n\n");
        sb.append("wrongbook_fix }|--|| wrong_question_record\n");
        sb.append("wrong_question_record }|--|| tower_pattern\n");
        sb.append("@enduml\n");
        return sb.toString();
    }
    
    private String generateAnalysisReport(String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ER图分析报告\n\n");
        sb.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        sb.append("## 实体摘要\n\n");
        sb.append("| 实体名称 | 类型 | 字段数量 | 主键 | 外键数量 |\n");
        sb.append("|---------|------|----------|------|----------|\n");
        sb.append("| wrongbook_fix | 源表 | 4 | id | 1 |\n");
        sb.append("| wrong_question_record | 维表 | 4 | id | 1 |\n");
        sb.append("| tower_pattern | 维表 | 3 | id | 0 |\n\n");
        sb.append("## 关系摘要\n\n");
        sb.append("| 源实体 | 目标实体 | 关系类型 | 连接字段 |\n");
        sb.append("|--------|----------|----------|----------|\n");
        sb.append("| wrongbook_fix | wrong_question_record | 多对一 | originWrongRecordId->id |\n");
        sb.append("| wrong_question_record | tower_pattern | 多对一 | pattern_id->id |\n\n");
        sb.append("## 数据质量建议\n\n");
        sb.append("1. 建议在 originWrongRecordId 字段上创建索引以提高JOIN性能\n");
        sb.append("2. 考虑对 subject 字段进行标准化处理\n");
        sb.append("3. 添加数据一致性检查规则\n");
        return sb.toString();
    }
    
    private List<String> detectConflicts(String content) {
        List<String> conflicts = new ArrayList<>();
        
        // 模拟冲突检测
        if (content.contains("wrong_question_record")) {
            // 模拟字段类型冲突
            conflicts.add("Field type conflict in wrong_question_record.user_id: current=VARCHAR(255), existing=VARCHAR(100)");
        }
        
        return conflicts;
    }
    
    private void updateKnowledgeBaseFile(File knowledgeBaseDir, ERUpdateResult result) throws IOException {
        File knowledgeBaseFile = new File(knowledgeBaseDir, "er-knowledge-base.md");
        
        StringBuilder sb = new StringBuilder();
        sb.append("# ER Knowledge Base\n\n");
        sb.append("最后更新: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        sb.append("## 统计信息\n\n");
        sb.append("- 实体总数: ").append(result.getEntitiesAdded() + result.getEntitiesUpdated()).append("\n");
        sb.append("- 关系总数: ").append(result.getRelationshipsAdded()).append("\n");
        sb.append("- 最近添加的实体: ").append(result.getEntitiesAdded()).append("\n");
        sb.append("- 最近更新的实体: ").append(result.getEntitiesUpdated()).append("\n\n");
        sb.append("## 实体详情\n\n");
        sb.append("### wrongbook_fix (源表)\n\n");
        sb.append("| 字段名 | 类型 | 约束 |\n");
        sb.append("|--------|------|------|\n");
        sb.append("| id | VARCHAR(255) | PK |\n");
        sb.append("| originWrongRecordId | VARCHAR(255) | FK |\n");
        sb.append("| userId | VARCHAR(255) | |\n");
        sb.append("| result | INT | |\n\n");
        sb.append("### wrong_question_record (维表)\n\n");
        sb.append("| 字段名 | 类型 | 约束 |\n");
        sb.append("|--------|------|------|\n");
        sb.append("| id | VARCHAR(255) | PK |\n");
        sb.append("| user_id | VARCHAR(255) | |\n");
        sb.append("| pattern_id | VARCHAR(255) | FK |\n");
        sb.append("| subject | VARCHAR(255) | |\n\n");
        sb.append("## 关系映射\n\n");
        sb.append("1. wrongbook_fix.originWrongRecordId -> wrong_question_record.id (多对一)\n");
        sb.append("2. wrong_question_record.pattern_id -> tower_pattern.id (多对一)\n");
        
        Files.write(knowledgeBaseFile.toPath(), sb.toString().getBytes());
        
        // 如果有冲突，创建冲突报告
        if (!result.getConflicts().isEmpty()) {
            File conflictsFile = new File(knowledgeBaseDir, "conflicts.md");
            StringBuilder conflictSb = new StringBuilder();
            conflictSb.append("# ER知识库冲突报告\n\n");
            conflictSb.append("检测时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
            conflictSb.append("## 冲突详情\n\n");
            for (String conflict : result.getConflicts()) {
                conflictSb.append("- ").append(conflict).append("\n");
            }
            conflictSb.append("\n## 解决建议\n\n");
            conflictSb.append("1. 检查输入文件中的表结构定义\n");
            conflictSb.append("2. 确认字段类型的兼容性\n");
            conflictSb.append("3. 如果确认无误，可以使用 forceERUpdate=true 强制更新\n");
            
            Files.write(conflictsFile.toPath(), conflictSb.toString().getBytes());
        }
    }
}
