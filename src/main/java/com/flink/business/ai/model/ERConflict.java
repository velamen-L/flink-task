package com.flink.business.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

/**
 * ER图冲突模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "er_conflicts")
public class ERConflict {
    
    @Id
    private String conflictId;
    
    /**
     * 冲突类型列表
     */
    @ElementCollection
    @CollectionTable(name = "conflict_types", joinColumns = @JoinColumn(name = "conflict_id"))
    @Column(name = "conflict_type")
    private List<String> conflictTypes;
    
    /**
     * 新关联
     */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "new_relation_id")
    private TableRelation newRelation;
    
    /**
     * 现有关联
     */
    @ManyToOne
    @JoinColumn(name = "existing_relation_id")
    private TableRelation existingRelation;
    
    /**
     * 严重程度
     */
    @Enumerated(EnumType.STRING)
    private ConflictSeverity severity;
    
    /**
     * 建议解决方案
     */
    @Column(length = 1000)
    private String suggestion;
    
    /**
     * 是否可自动解决
     */
    private Boolean autoResolvable;
    
    /**
     * 检测时间
     */
    private Long detectedAt;
    
    /**
     * 是否已解决
     */
    private Boolean resolved = false;
    
    /**
     * 解决时间
     */
    private Long resolvedAt;
    
    /**
     * 解决方式
     */
    private String resolutionMethod;
    
    public enum ConflictSeverity {
        INFO,
        WARNING, 
        CRITICAL
    }
}
