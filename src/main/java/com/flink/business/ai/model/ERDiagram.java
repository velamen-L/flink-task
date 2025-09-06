package com.flink.business.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;
import java.util.ArrayList;

/**
 * ER图数据模型
 * 
 * 企业级ER图知识库的核心数据结构
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "er_diagrams")
public class ERDiagram {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ER图名称
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * 业务域
     */
    private String businessDomain;
    
    /**
     * 版本号
     */
    private Integer version;
    
    /**
     * 描述
     */
    @Column(length = 1000)
    private String description;
    
    /**
     * 表关联关系列表
     */
    @OneToMany(mappedBy = "erDiagram", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TableRelation> relations = new ArrayList<>();
    
    /**
     * 涉及的表列表
     */
    @ElementCollection
    @CollectionTable(name = "er_diagram_tables", joinColumns = @JoinColumn(name = "er_diagram_id"))
    @Column(name = "table_name")
    private List<String> tables = new ArrayList<>();
    
    /**
     * 创建时间
     */
    private Long createdAt;
    
    /**
     * 最后更新时间
     */
    private Long lastUpdated;
    
    /**
     * 创建者
     */
    private String createdBy;
    
    /**
     * 是否为当前版本
     */
    private Boolean isActive;
    
    /**
     * ER图元数据
     */
    @Embedded
    private ERDiagramMetadata metadata;
    
    /**
     * 创建空的ER图
     */
    public static ERDiagram createEmpty() {
        return ERDiagram.builder()
            .name("Enterprise ER Diagram")
            .businessDomain("all")
            .version(1)
            .description("企业级统一ER图")
            .relations(new ArrayList<>())
            .tables(new ArrayList<>())
            .createdAt(System.currentTimeMillis())
            .lastUpdated(System.currentTimeMillis())
            .isActive(true)
            .metadata(ERDiagramMetadata.createDefault())
            .build();
    }
    
    /**
     * 添加表关联
     */
    public void addRelation(TableRelation relation) {
        if (this.relations == null) {
            this.relations = new ArrayList<>();
        }
        this.relations.add(relation);
        relation.setErDiagram(this);
        
        // 同时添加到表列表
        addTable(relation.getSourceTable());
        addTable(relation.getTargetTable());
    }
    
    /**
     * 添加表
     */
    public void addTable(String tableName) {
        if (this.tables == null) {
            this.tables = new ArrayList<>();
        }
        if (!this.tables.contains(tableName)) {
            this.tables.add(tableName);
        }
    }
    
    /**
     * 获取表的所有关联
     */
    public List<TableRelation> getRelationsForTable(String tableName) {
        return relations.stream()
            .filter(r -> tableName.equals(r.getSourceTable()) || tableName.equals(r.getTargetTable()))
            .collect(Collectors.toList());
    }
    
    /**
     * 检查是否包含特定关联
     */
    public boolean containsRelation(String sourceTable, String targetTable) {
        return relations.stream()
            .anyMatch(r -> 
                (sourceTable.equals(r.getSourceTable()) && targetTable.equals(r.getTargetTable())) ||
                (sourceTable.equals(r.getTargetTable()) && targetTable.equals(r.getSourceTable()))
            );
    }
    
    /**
     * 获取统计信息
     */
    public ERDiagramStats getStats() {
        return ERDiagramStats.builder()
            .totalTables(tables != null ? tables.size() : 0)
            .totalRelations(relations != null ? relations.size() : 0)
            .oneToOneRelations(countRelationsByType("ONE_TO_ONE"))
            .oneToManyRelations(countRelationsByType("ONE_TO_MANY"))
            .manyToManyRelations(countRelationsByType("MANY_TO_MANY"))
            .build();
    }
    
    private long countRelationsByType(String type) {
        return relations != null ? relations.stream()
            .filter(r -> type.equals(r.getCardinality()))
            .count() : 0;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class ERDiagramMetadata {
        
        /**
         * 数据源信息
         */
        private String dataSource;
        
        /**
         * 同步状态
         */
        private String syncStatus;
        
        /**
         * 最后同步时间
         */
        private Long lastSyncTime;
        
        /**
         * 质量评分
         */
        private Double qualityScore;
        
        /**
         * 完整性评分
         */
        private Double completenessScore;
        
        /**
         * 标签
         */
        private String tags;
        
        public static ERDiagramMetadata createDefault() {
            return ERDiagramMetadata.builder()
                .dataSource("AI_EXTRACTED")
                .syncStatus("ACTIVE")
                .lastSyncTime(System.currentTimeMillis())
                .qualityScore(0.8)
                .completenessScore(0.7)
                .tags("auto-generated")
                .build();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ERDiagramStats {
        private Integer totalTables;
        private Integer totalRelations;
        private Long oneToOneRelations;
        private Long oneToManyRelations;
        private Long manyToManyRelations;
    }
}
