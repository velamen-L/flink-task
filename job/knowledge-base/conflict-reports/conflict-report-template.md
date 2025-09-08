# ER图知识库冲突报告模板 v1.0

## 📋 冲突报告概览

| 项目 | 值 |
|------|-----|
| **报告ID** | `{conflict_report_id}` |
| **检测时间** | `{detection_timestamp}` |
| **业务域** | `{domain}` |
| **输入文件** | `{input_request_file}` |
| **冲突等级** | `{conflict_severity}` |
| **冲突总数** | `{total_conflicts}` |
| **处理状态** | `{processing_status}` |

---

## 🚨 冲突检测结果

### ❌ Critical级别冲突 (阻塞性问题)

#### 冲突 #{conflict_id} - {conflict_type}
```yaml
conflict_details:
  conflict_id: "{conflict_id}"
  type: "{conflict_type}"
  severity: "CRITICAL"
  affected_entities: ["{entity_list}"]
  
detection_info:
  detected_at: "{timestamp}"
  detection_rule: "{detection_rule_name}"
  confidence_level: "{confidence}%"
  
conflict_description: |
    {详细的冲突描述}
    
current_state:
  existing_definition:
    entity: "{existing_entity}"
    field: "{existing_field}"
    type: "{existing_type}"
    constraints: ["{existing_constraints}"]
    
  new_definition:
    entity: "{new_entity}"
    field: "{new_field}"
    type: "{new_type}"
    constraints: ["{new_constraints}"]
    
impact_analysis:
  affected_systems: ["{system_list}"]
  data_migration_required: {boolean}
  breaking_change: {boolean}
  estimated_effort: "{effort_level}"
  risk_level: "{risk_assessment}"
  
business_impact:
  description: "{business_impact_description}"
  affected_processes: ["{process_list}"]
  user_impact: "{user_impact_description}"
```

### ⚠️ Warning级别冲突 (非阻塞性问题)

#### 冲突 #{conflict_id} - {conflict_type}
```yaml
# 同上结构，severity: "WARNING"
```

### 💡 Info级别冲突 (信息性提示)

#### 冲突 #{conflict_id} - {conflict_type}
```yaml
# 同上结构，severity: "INFO"
```

---

## 🔍 冲突详细分析

### 表结构冲突分析

#### 字段类型不匹配
```yaml
field_type_conflicts:
  - table_name: "{table_name}"
    field_name: "{field_name}"
    existing_type: "{existing_type}"
    new_type: "{new_type}"
    compatibility: "{compatible|incompatible|requires_conversion}"
    conversion_strategy: "{strategy_description}"
    data_loss_risk: "{high|medium|low|none}"
```

#### 主键约束冲突
```yaml
primary_key_conflicts:
  - table_name: "{table_name}"
    existing_pk: ["{existing_pk_fields}"]
    new_pk: ["{new_pk_fields}"]
    conflict_reason: "{reason_description}"
    resolution_complexity: "{simple|moderate|complex}"
```

#### 外键引用冲突
```yaml
foreign_key_conflicts:
  - source_table: "{source_table}"
    source_field: "{source_field}"
    target_table: "{target_table}"
    target_field: "{target_field}"
    conflict_type: "{missing_target|type_mismatch|cascade_conflict}"
    impact_on_joins: "{description}"
```

### 关联关系冲突分析

#### JOIN条件不一致
```yaml
join_condition_conflicts:
  - relationship_id: "{relationship_id}"
    existing_condition: "{existing_join_condition}"
    new_condition: "{new_join_condition}"
    semantic_difference: "{description}"
    data_result_impact: "{impact_description}"
```

#### 关联路径变更
```yaml
relationship_path_conflicts:
  - path_id: "{path_id}"
    existing_path: ["{existing_entity_chain}"]
    new_path: ["{new_entity_chain}"]
    path_length_change: "{increase|decrease|same}"
    performance_impact: "{impact_assessment}"
```

### 业务逻辑冲突分析

#### 业务规则矛盾
```yaml
business_rule_conflicts:
  - rule_id: "{rule_id}"
    existing_rule: "{existing_business_rule}"
    new_rule: "{new_business_rule}"
    contradiction_type: "{logical|semantic|procedural}"
    resolution_requires_business_decision: {boolean}
```

#### 数据约束冲突
```yaml
constraint_conflicts:
  - constraint_type: "{check|unique|not_null}"
    table_name: "{table_name}"
    field_name: "{field_name}"
    existing_constraint: "{existing_constraint}"
    new_constraint: "{new_constraint}"
    data_validation_impact: "{impact_description}"
```

---

## 🔧 解决方案建议

### 推荐解决方案

#### 方案A: 数据迁移方案 (推荐)
```yaml
migration_solution:
  solution_id: "MIGRATION_A"
  approach: "incremental_migration"
  
  steps:
    1:
      action: "backup_existing_data"
      description: "备份现有知识库数据"
      estimated_time: "{time_estimate}"
      
    2:
      action: "create_migration_script"
      description: "创建数据类型转换脚本"
      scripts_required: ["{script_list}"]
      
    3:
      action: "run_compatibility_tests"
      description: "运行兼容性测试"
      test_coverage: "{coverage_percentage}%"
      
    4:
      action: "deploy_incremental_changes"
      description: "增量部署变更"
      rollback_strategy: "available"
      
    5:
      action: "validate_data_integrity"
      description: "验证数据完整性"
      validation_rules: ["{validation_list}"]
      
  advantages:
    - "保持现有系统稳定性"
    - "支持回滚操作"
    - "分阶段风险控制"
    
  disadvantages:
    - "实施时间较长"
    - "需要额外的迁移环境"
    
  effort_estimate: "{effort_level}"
  risk_assessment: "{risk_level}"
  implementation_timeline: "{timeline}"
```

#### 方案B: 版本分支方案
```yaml
version_branch_solution:
  solution_id: "VERSION_BRANCH_B"
  approach: "parallel_versions"
  
  # 详细配置...
```

#### 方案C: 字段映射方案
```yaml
field_mapping_solution:
  solution_id: "FIELD_MAPPING_C"
  approach: "compatibility_layer"
  
  # 详细配置...
```

### 不推荐方案 (仅供参考)
```yaml
not_recommended_solutions:
  force_override:
    reason: "可能导致数据丢失"
    risk_level: "HIGH"
    
  ignore_conflicts:
    reason: "可能导致系统不一致"
    risk_level: "MEDIUM"
```

---

## 📊 影响评估矩阵

### 系统影响评估
| 系统组件 | 影响程度 | 修改需求 | 测试需求 | 风险等级 |
|----------|----------|----------|----------|----------|
| **ER知识库** | `{impact_level}` | `{modification_required}` | `{testing_required}` | `{risk_level}` |
| **SQL生成器** | `{impact_level}` | `{modification_required}` | `{testing_required}` | `{risk_level}` |
| **验证工作流** | `{impact_level}` | `{modification_required}` | `{testing_required}` | `{risk_level}` |
| **监控系统** | `{impact_level}` | `{modification_required}` | `{testing_required}` | `{risk_level}` |

### 业务影响评估
| 业务流程 | 影响程度 | 停机时间 | 用户影响 | 业务风险 |
|----------|----------|----------|----------|----------|
| **SQL生成** | `{impact_level}` | `{downtime}` | `{user_impact}` | `{business_risk}` |
| **数据验证** | `{impact_level}` | `{downtime}` | `{user_impact}` | `{business_risk}` |
| **监控告警** | `{impact_level}` | `{downtime}` | `{user_impact}` | `{business_risk}` |

---

## 📅 处理时间表

### 即时行动 (0-24小时)
```yaml
immediate_actions:
  - action: "conflict_acknowledgment"
    owner: "{responsible_person}"
    deadline: "within 2 hours"
    
  - action: "impact_assessment_review"
    owner: "{technical_lead}"
    deadline: "within 8 hours"
    
  - action: "stakeholder_notification"
    owner: "{project_manager}"
    deadline: "within 24 hours"
```

### 短期行动 (1-7天)
```yaml
short_term_actions:
  - action: "detailed_solution_design"
    owner: "{architect}"
    deadline: "within 3 days"
    
  - action: "resource_allocation"
    owner: "{resource_manager}"
    deadline: "within 5 days"
    
  - action: "implementation_planning"
    owner: "{technical_team}"
    deadline: "within 7 days"
```

### 中期行动 (1-4周)
```yaml
medium_term_actions:
  - action: "solution_implementation"
    owner: "{development_team}"
    deadline: "within 2 weeks"
    
  - action: "testing_and_validation"
    owner: "{qa_team}"
    deadline: "within 3 weeks"
    
  - action: "production_deployment"
    owner: "{devops_team}"
    deadline: "within 4 weeks"
```

---

## 🔄 决策流程

### 决策节点
```yaml
decision_points:
  business_approval:
    required_for: "业务规则变更"
    approvers: ["{business_owner}", "{domain_expert}"]
    criteria: "业务影响评估"
    
  technical_approval:
    required_for: "技术架构变更"
    approvers: ["{technical_architect}", "{lead_developer}"]
    criteria: "技术可行性评估"
    
  security_approval:
    required_for: "数据结构变更"
    approvers: ["{security_officer}", "{data_protection_officer}"]
    criteria: "安全影响评估"
```

### 升级路径
```yaml
escalation_path:
  level_1: "Team Lead"
  level_2: "Technical Manager"
  level_3: "Engineering Director"
  level_4: "CTO"
  
  escalation_triggers:
    - "超过24小时无决策"
    - "资源冲突无法解决"
    - "跨域影响需要协调"
```

---

## 📈 成功指标

### 解决成功的衡量标准
```yaml
success_metrics:
  technical_metrics:
    - name: "冲突解决率"
      target: "100%"
      measurement: "已解决冲突数 / 总冲突数"
      
    - name: "系统稳定性"
      target: "> 99.9%"
      measurement: "系统可用时间 / 总时间"
      
    - name: "数据一致性"
      target: "100%"
      measurement: "数据验证通过率"
      
  business_metrics:
    - name: "业务流程中断时间"
      target: "< 4 hours"
      measurement: "实际停机时间"
      
    - name: "用户体验影响"
      target: "minimal"
      measurement: "用户反馈和投诉数量"
      
  process_metrics:
    - name: "问题响应时间"
      target: "< 2 hours"
      measurement: "从检测到响应的时间"
      
    - name: "解决方案实施时间"
      target: "< 2 weeks"
      measurement: "从决策到部署的时间"
```

---

## 📚 相关文档和资源

### 参考文档
- [ER知识库管理规则](../.cursor/rules/intelligent-er-knowledge-base.mdc)
- [冲突检测算法说明](../docs/conflict-detection-algorithms.md)
- [数据迁移最佳实践](../docs/data-migration-best-practices.md)
- [版本管理策略](../docs/version-management-strategy.md)

### 联系信息
```yaml
contacts:
  technical_support:
    email: "tech-support@company.com"
    slack: "#er-knowledge-base"
    
  business_owner:
    name: "{business_owner_name}"
    email: "{business_owner_email}"
    
  escalation_contact:
    name: "{escalation_contact_name}"
    email: "{escalation_contact_email}"
    phone: "{emergency_phone}"
```

---

## 📝 附录

### 冲突详细日志
```
{详细的冲突检测日志}
```

### 原始输入对比
```yaml
# 现有定义
existing_definition: |
  {现有ER图定义}

# 新输入定义  
new_definition: |
  {新的ER图定义}
```

### 自动化检测脚本
```sql
-- 冲突检测SQL脚本
{自动化冲突检测的SQL代码}
```

---

*此报告由 AI Agent 基于 intelligent-er-knowledge-base.mdc 规则自动生成*
*报告ID: {report_id} | 生成时间: {generation_timestamp}*
*如有疑问，请联系技术支持团队*
