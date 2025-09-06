package com.flink.business.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * 表关联关系模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "table_relations")
public class TableRelation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 源表
     */
    @Column(nullable = false)
    private String sourceTable;
    
    /**
     * 目标表
     */
    @Column(nullable = false)
    private String targetTable;
    
    /**
     * 关联类型 (INNER_JOIN, LEFT_JOIN, RIGHT_JOIN, FULL_JOIN)
     */
    private String relationType;
    
    /**
     * JOIN条件
     */
    @Column(length = 500)
    private String joinCondition;
    
    /**
     * 源字段
     */
    private String sourceColumn;
    
    /**
     * 目标字段
     */
    private String targetColumn;
    
    /**
     * 基数关系 (ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY)
     */
    private String cardinality;
    
    /**
     * 提取来源 (SQL_JOIN, FOREIGN_KEY, INFERRED)
     */
    private String extractedFrom;
    
    /**
     * 置信度 (0.0-1.0)
     */
    private Double confidence;
    
    /**
     * 所属ER图
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "er_diagram_id")
    private ERDiagram erDiagram;
    
    /**
     * 创建时间
     */
    private Long createdAt;
    
    /**
     * 最后验证时间
     */
    private Long lastValidated;
    
    /**
     * 是否有效
     */
    private Boolean isValid;
}
