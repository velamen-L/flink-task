package com.flink.business.ai.service;

import com.flink.business.ai.model.ERDiagram;
import com.flink.business.ai.model.ERConflict;
import com.flink.business.ai.model.TableRelation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ER图管理服务
 * 
 * 核心功能：
 * 1. 汇总所有使用的表关联到统一ER图
 * 2. 检测与现有ER图的冲突
 * 3. 维护企业级知识库
 * 4. 支持增量更新和版本管理
 */
@Slf4j
@Service
public class ERDiagramService {

    @Autowired
    private ERDiagramRepository erDiagramRepository;

    @Autowired
    private TableMetadataService tableMetadataService;

    @Autowired
    private ConflictDetectionService conflictDetectionService;

    /**
     * 从SQL生成结果中提取表关联并更新ER图
     */
    public ERDiagramUpdateResult updateERDiagramFromSQL(String sql, String jobName, String businessDomain) {
        log.info("从SQL中提取表关联并更新ER图: {}", jobName);
        
        try {
            // 1. 解析SQL获取表关联
            List<TableRelation> extractedRelations = extractTableRelationsFromSQL(sql);
            
            // 2. 获取当前企业ER图
            ERDiagram currentERDiagram = getCurrentERDiagram();
            
            // 3. 检测冲突
            List<ERConflict> conflicts = detectConflicts(extractedRelations, currentERDiagram);
            
            // 4. 合并新的关联到ER图
            ERDiagram updatedERDiagram = mergeRelations(currentERDiagram, extractedRelations, conflicts);
            
            // 5. 保存更新后的ER图
            ERDiagram savedDiagram = erDiagramRepository.save(updatedERDiagram);
            
            // 6. 生成更新报告
            ERDiagramUpdateResult result = ERDiagramUpdateResult.builder()
                .jobName(jobName)
                .businessDomain(businessDomain)
                .extractedRelations(extractedRelations)
                .conflicts(conflicts)
                .updatedERDiagram(savedDiagram)
                .hasConflicts(!conflicts.isEmpty())
                .newRelationsCount(extractedRelations.size())
                .totalRelationsCount(savedDiagram.getRelations().size())
                .updateTime(System.currentTimeMillis())
                .build();
            
            log.info("ER图更新完成，新增关联: {}, 冲突数: {}", 
                    extractedRelations.size(), conflicts.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("ER图更新失败", e);
            throw new ERDiagramException("ER图更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从SQL中提取表关联
     */
    private List<TableRelation> extractTableRelationsFromSQL(String sql) {
        List<TableRelation> relations = new ArrayList<>();
        
        try {
            // 1. 解析SQL AST
            SQLParser parser = new SQLParser();
            SQLStatement statement = parser.parse(sql);
            
            // 2. 提取JOIN关系
            List<JoinInfo> joins = extractJoins(statement);
            
            // 3. 提取外键关系
            List<ForeignKeyInfo> foreignKeys = extractForeignKeys(statement);
            
            // 4. 转换为TableRelation对象
            for (JoinInfo join : joins) {
                TableRelation relation = TableRelation.builder()
                    .sourceTable(join.getLeftTable())
                    .targetTable(join.getRightTable())
                    .relationType(join.getJoinType())
                    .joinCondition(join.getCondition())
                    .sourceColumn(join.getLeftColumn())
                    .targetColumn(join.getRightColumn())
                    .cardinality(inferCardinality(join))
                    .extractedFrom("SQL_JOIN")
                    .confidence(calculateJoinConfidence(join))
                    .build();
                
                relations.add(relation);
            }
            
            // 5. 添加隐式关系（通过字段名推断）
            relations.addAll(inferImplicitRelations(statement));
            
        } catch (Exception e) {
            log.error("SQL解析失败", e);
            // 降级方案：使用正则表达式提取
            relations.addAll(extractRelationsUsingRegex(sql));
        }
        
        return relations;
    }

    /**
     * 检测ER图冲突
     */
    private List<ERConflict> detectConflicts(List<TableRelation> newRelations, ERDiagram currentDiagram) {
        List<ERConflict> conflicts = new ArrayList<>();
        
        for (TableRelation newRelation : newRelations) {
            // 查找现有相同表对的关联
            List<TableRelation> existingRelations = currentDiagram.getRelations().stream()
                .filter(r -> isSameTablePair(r, newRelation))
                .collect(Collectors.toList());
            
            for (TableRelation existing : existingRelations) {
                ERConflict conflict = detectRelationConflict(newRelation, existing);
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }
        
        return conflicts;
    }

    /**
     * 检测两个关联之间的冲突
     */
    private ERConflict detectRelationConflict(TableRelation newRelation, TableRelation existingRelation) {
        List<String> conflictTypes = new ArrayList<>();
        
        // 1. JOIN类型冲突
        if (!Objects.equals(newRelation.getRelationType(), existingRelation.getRelationType())) {
            conflictTypes.add("JOIN_TYPE_MISMATCH");
        }
        
        // 2. 基数冲突
        if (!Objects.equals(newRelation.getCardinality(), existingRelation.getCardinality())) {
            conflictTypes.add("CARDINALITY_MISMATCH");
        }
        
        // 3. 字段冲突
        if (!Objects.equals(newRelation.getSourceColumn(), existingRelation.getSourceColumn()) ||
            !Objects.equals(newRelation.getTargetColumn(), existingRelation.getTargetColumn())) {
            conflictTypes.add("COLUMN_MISMATCH");
        }
        
        // 4. 条件冲突
        if (!isSimilarCondition(newRelation.getJoinCondition(), existingRelation.getJoinCondition())) {
            conflictTypes.add("CONDITION_MISMATCH");
        }
        
        if (conflictTypes.isEmpty()) {
            return null;
        }
        
        return ERConflict.builder()
            .conflictId(UUID.randomUUID().toString())
            .conflictTypes(conflictTypes)
            .newRelation(newRelation)
            .existingRelation(existingRelation)
            .severity(calculateConflictSeverity(conflictTypes))
            .suggestion(generateConflictSuggestion(newRelation, existingRelation, conflictTypes))
            .autoResolvable(isAutoResolvable(conflictTypes))
            .detectedAt(System.currentTimeMillis())
            .build();
    }

    /**
     * 合并新关联到ER图
     */
    private ERDiagram mergeRelations(ERDiagram currentDiagram, List<TableRelation> newRelations, 
                                   List<ERConflict> conflicts) {
        
        ERDiagram.ERDiagramBuilder builder = currentDiagram.toBuilder();
        List<TableRelation> mergedRelations = new ArrayList<>(currentDiagram.getRelations());
        
        for (TableRelation newRelation : newRelations) {
            // 检查是否有冲突
            boolean hasConflict = conflicts.stream()
                .anyMatch(c -> c.getNewRelation().equals(newRelation));
            
            if (!hasConflict) {
                // 检查是否已存在相同关联
                boolean exists = mergedRelations.stream()
                    .anyMatch(r -> isSameRelation(r, newRelation));
                
                if (!exists) {
                    mergedRelations.add(newRelation);
                } else {
                    // 更新置信度
                    updateRelationConfidence(mergedRelations, newRelation);
                }
            } else {
                // 有冲突的关联需要人工决策
                log.warn("关联有冲突，需要人工处理: {} -> {}", 
                        newRelation.getSourceTable(), newRelation.getTargetTable());
            }
        }
        
        // 更新表列表
        Set<String> allTables = mergedRelations.stream()
            .flatMap(r -> Stream.of(r.getSourceTable(), r.getTargetTable()))
            .collect(Collectors.toSet());
        
        return builder
            .relations(mergedRelations)
            .tables(new ArrayList<>(allTables))
            .lastUpdated(System.currentTimeMillis())
            .version(currentDiagram.getVersion() + 1)
            .build();
    }

    /**
     * 生成Mermaid ER图
     */
    public String generateMermaidERDiagram(String businessDomain) {
        ERDiagram erDiagram = getCurrentERDiagram();
        
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("erDiagram\n");
        mermaid.append("    %% ").append(businessDomain).append(" 业务域 ER图\n");
        mermaid.append("    %% 生成时间: ").append(new Date()).append("\n");
        mermaid.append("    %% 总关联数: ").append(erDiagram.getRelations().size()).append("\n\n");
        
        // 添加表定义
        for (String table : erDiagram.getTables()) {
            TableMetadata metadata = tableMetadataService.getTableMetadata(table);
            if (metadata != null) {
                mermaid.append("    ").append(table).append(" {\n");
                for (ColumnMetadata column : metadata.getColumns()) {
                    mermaid.append("        ")
                           .append(column.getDataType()).append(" ")
                           .append(column.getName());
                    if (column.isPrimaryKey()) {
                        mermaid.append(" PK");
                    }
                    if (column.isForeignKey()) {
                        mermaid.append(" FK");
                    }
                    mermaid.append("\n");
                }
                mermaid.append("    }\n\n");
            }
        }
        
        // 添加关联关系
        for (TableRelation relation : erDiagram.getRelations()) {
            String relationSymbol = getRelationSymbol(relation.getCardinality());
            mermaid.append("    ")
                   .append(relation.getSourceTable())
                   .append(" ")
                   .append(relationSymbol)
                   .append(" ")
                   .append(relation.getTargetTable())
                   .append(" : \"")
                   .append(relation.getSourceColumn()).append(" -> ").append(relation.getTargetColumn())
                   .append("\"\n");
        }
        
        return mermaid.toString();
    }

    /**
     * 生成PlantUML ER图
     */
    public String generatePlantUMLERDiagram(String businessDomain) {
        ERDiagram erDiagram = getCurrentERDiagram();
        
        StringBuilder plantuml = new StringBuilder();
        plantuml.append("@startuml\n");
        plantuml.append("!theme plain\n");
        plantuml.append("title ").append(businessDomain).append(" 业务域 ER图\n\n");
        
        // 定义实体
        for (String table : erDiagram.getTables()) {
            TableMetadata metadata = tableMetadataService.getTableMetadata(table);
            if (metadata != null) {
                plantuml.append("entity \"").append(table).append("\" as ").append(table).append(" {\n");
                for (ColumnMetadata column : metadata.getColumns()) {
                    String prefix = "";
                    if (column.isPrimaryKey()) {
                        prefix = "* ";
                    } else if (column.isForeignKey()) {
                        prefix = "+ ";
                    }
                    plantuml.append("  ").append(prefix)
                           .append(column.getName()).append(" : ")
                           .append(column.getDataType()).append("\n");
                }
                plantuml.append("}\n\n");
            }
        }
        
        // 定义关系
        for (TableRelation relation : erDiagram.getRelations()) {
            String cardinality = convertToPlantUMLCardinality(relation.getCardinality());
            plantuml.append(relation.getSourceTable())
                   .append(" ")
                   .append(cardinality)
                   .append(" ")
                   .append(relation.getTargetTable())
                   .append(" : ")
                   .append(relation.getSourceColumn()).append(" -> ").append(relation.getTargetColumn())
                   .append("\n");
        }
        
        plantuml.append("\n@enduml");
        return plantuml.toString();
    }

    /**
     * 获取ER图冲突报告
     */
    public ERConflictReport getConflictReport() {
        List<ERConflict> allConflicts = conflictDetectionService.getAllUnresolvedConflicts();
        
        return ERConflictReport.builder()
            .totalConflicts(allConflicts.size())
            .criticalConflicts(allConflicts.stream()
                .filter(c -> c.getSeverity() == ConflictSeverity.CRITICAL)
                .collect(Collectors.toList()))
            .warningConflicts(allConflicts.stream()
                .filter(c -> c.getSeverity() == ConflictSeverity.WARNING)
                .collect(Collectors.toList()))
            .autoResolvableConflicts(allConflicts.stream()
                .filter(ERConflict::isAutoResolvable)
                .collect(Collectors.toList()))
            .conflictsByDomain(groupConflictsByDomain(allConflicts))
            .suggestions(generateResolutionSuggestions(allConflicts))
            .build();
    }

    /**
     * 自动解决可解决的冲突
     */
    public ConflictResolutionResult autoResolveConflicts() {
        List<ERConflict> autoResolvableConflicts = conflictDetectionService.getAutoResolvableConflicts();
        
        int resolvedCount = 0;
        List<String> resolutionActions = new ArrayList<>();
        
        for (ERConflict conflict : autoResolvableConflicts) {
            try {
                ConflictResolution resolution = generateResolution(conflict);
                applyResolution(resolution);
                resolvedCount++;
                resolutionActions.add(resolution.getDescription());
                
                log.info("自动解决冲突: {}", conflict.getConflictId());
                
            } catch (Exception e) {
                log.error("自动解决冲突失败: {}", conflict.getConflictId(), e);
            }
        }
        
        return ConflictResolutionResult.builder()
            .totalConflicts(autoResolvableConflicts.size())
            .resolvedCount(resolvedCount)
            .resolutionActions(resolutionActions)
            .resolvedAt(System.currentTimeMillis())
            .build();
    }

    // 辅助方法
    private ERDiagram getCurrentERDiagram() {
        return erDiagramRepository.findLatest()
            .orElse(ERDiagram.createEmpty());
    }

    private boolean isSameTablePair(TableRelation r1, TableRelation r2) {
        return (Objects.equals(r1.getSourceTable(), r2.getSourceTable()) && 
                Objects.equals(r1.getTargetTable(), r2.getTargetTable())) ||
               (Objects.equals(r1.getSourceTable(), r2.getTargetTable()) && 
                Objects.equals(r1.getTargetTable(), r2.getSourceTable()));
    }

    private boolean isSameRelation(TableRelation r1, TableRelation r2) {
        return isSameTablePair(r1, r2) && 
               Objects.equals(r1.getSourceColumn(), r2.getSourceColumn()) &&
               Objects.equals(r1.getTargetColumn(), r2.getTargetColumn());
    }

    private String getRelationSymbol(String cardinality) {
        switch (cardinality) {
            case "ONE_TO_ONE": return "||--||";
            case "ONE_TO_MANY": return "||--o{";
            case "MANY_TO_ONE": return "}o--||";
            case "MANY_TO_MANY": return "}o--o{";
            default: return "||--||";
        }
    }

    private String convertToPlantUMLCardinality(String cardinality) {
        switch (cardinality) {
            case "ONE_TO_ONE": return "||--||";
            case "ONE_TO_MANY": return "||--o{";
            case "MANY_TO_ONE": return "}o--||";
            case "MANY_TO_MANY": return "}o--o{";
            default: return "||--||";
        }
    }

    private ConflictSeverity calculateConflictSeverity(List<String> conflictTypes) {
        if (conflictTypes.contains("CARDINALITY_MISMATCH") || 
            conflictTypes.contains("COLUMN_MISMATCH")) {
            return ConflictSeverity.CRITICAL;
        } else if (conflictTypes.contains("JOIN_TYPE_MISMATCH")) {
            return ConflictSeverity.WARNING;
        } else {
            return ConflictSeverity.INFO;
        }
    }

    private String generateConflictSuggestion(TableRelation newRelation, TableRelation existingRelation, 
                                            List<String> conflictTypes) {
        StringBuilder suggestion = new StringBuilder();
        suggestion.append("建议解决方案：\n");
        
        for (String conflictType : conflictTypes) {
            switch (conflictType) {
                case "JOIN_TYPE_MISMATCH":
                    suggestion.append("- 检查JOIN类型：现有(")
                             .append(existingRelation.getRelationType())
                             .append(") vs 新(")
                             .append(newRelation.getRelationType())
                             .append(")\n");
                    break;
                case "CARDINALITY_MISMATCH":
                    suggestion.append("- 确认表关系基数：现有(")
                             .append(existingRelation.getCardinality())
                             .append(") vs 新(")
                             .append(newRelation.getCardinality())
                             .append(")\n");
                    break;
                case "COLUMN_MISMATCH":
                    suggestion.append("- 验证关联字段：现有(")
                             .append(existingRelation.getSourceColumn())
                             .append("->")
                             .append(existingRelation.getTargetColumn())
                             .append(") vs 新(")
                             .append(newRelation.getSourceColumn())
                             .append("->")
                             .append(newRelation.getTargetColumn())
                             .append(")\n");
                    break;
            }
        }
        
        return suggestion.toString();
    }

    private boolean isAutoResolvable(List<String> conflictTypes) {
        // 只有条件不匹配的冲突可以自动解决
        return conflictTypes.size() == 1 && conflictTypes.contains("CONDITION_MISMATCH");
    }
}
