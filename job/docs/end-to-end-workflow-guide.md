# 端到端工作流使用指南 v1.0

## 📖 概述

本指南介绍如何使用完整的端到端AI工作流，该工作流将三个核心流程串联起来：**SQL生成 → 数据验证 → ER知识库更新**。基于单个request文件输入，自动完成从业务需求到生产就绪的完整开发生命周期。

## 🎯 工作流概览

### 🔄 三阶段流程
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   阶段1: SQL生成  │ →  │  阶段2: 数据验证  │ →  │ 阶段3: ER知识库  │
│                │    │                │    │      更新       │
│ ✅ Flink SQL    │    │ ✅ 验证报告      │    │ ✅ ER图更新     │
│ ✅ 部署配置      │    │ ✅ 质量评分      │    │ ✅ 冲突检测     │
│ ✅ 监控配置      │    │ ✅ 测试数据      │    │ ✅ 知识库同步   │
│ ✅ 文档生成      │    │ ✅ 性能基准      │    │ ✅ 版本管理     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 🎯 核心价值
- **🚀 全自动化**: 从request文件到生产就绪的完全自动化
- **🔍 质量保证**: 多维度质量验证和评分体系
- **🗄️ 知识管理**: 智能维护的ER图知识库
- **📊 可追溯**: 完整的执行历史和产物清单
- **⚡ 高效率**: 5分钟内完成完整开发周期

## 🏗️ 系统架构

### 核心组件
```yaml
architecture_components:
  orchestration_engine:
    component: "intelligent-end-to-end-workflow.mdc"
    responsibility: "工作流编排和状态管理"
    
  phase_engines:
    sql_generation: "intelligent-sql-job-generator.mdc"
    data_validation: "intelligent-validation-workflow.mdc"
    er_kb_management: "intelligent-er-knowledge-base.mdc"
    
  configuration:
    workflow_config: "job/ai-config/end-to-end-workflow-config.yml"
    validation_config: "job/ai-config/validation-config.yml"
    
  state_management:
    persistence: "job/{domain}/.workflow/state.json"
    checkpoints: "after_each_phase"
    recovery: "resume_from_last_checkpoint"
```

### 数据流向
```yaml
data_flow:
  input:
    primary: "job/{domain}/{domain}-request-v3.md"
    
  phase_1_outputs:
    - "job/{domain}/sql/{domain}_wide_table_v3.sql"
    - "job/{domain}/deployment/deploy-{domain}-v3.yaml"
    - "job/{domain}/validation/data-quality-check-v3.sql"
    - "job/{domain}/docs/README-AI-Generated-v3.md"
    
  phase_2_outputs:
    - "job/{domain}/validation/validation-report-{domain}-v3.md"
    - "job/{domain}/validation/test-data-{domain}-v3.sql"
    - "job/{domain}/validation/performance-benchmark-{domain}-v3.sql"
    
  phase_3_outputs:
    - "job/knowledge-base/er-schemas/domains/{domain}/generated-er-diagram-v3.md"
    - "job/knowledge-base/er-schemas/domains/{domain}/source-payload.md"
    - "job/knowledge-base/er-schemas/domains/{domain}/dimension-tables.md"
    - "job/knowledge-base/er-schemas/domains/{domain}/relationships.md"
    
  workflow_outputs:
    - "job/{domain}/workflow/end-to-end-execution-report-v3.md"
```

## 🚀 使用方式

### 方式1：基于 Cursor 规则的智能执行 (推荐)

**适用场景**: 开发阶段，利用 Cursor 的 AI 能力进行端到端处理

#### 步骤1：准备输入文件
```bash
# 确保request文件格式正确
code job/{domain}/{domain}-request-v3.md

# 验证文件结构（必需的YAML sections）
# - job_info
# - field_mapping  
# - join_relationships
# - ER图定义
```

#### 步骤2：执行工作流
```bash
# 在 Cursor 中使用 AI 功能
# 规则文件会自动应用：
# - .cursor/rules/intelligent-end-to-end-workflow.mdc
```

**AI 提示示例**:
```
请基于 intelligent-end-to-end-workflow.mdc 规则执行完整的端到端工作流，
处理这个 wrongbook-request-v3.md 文件。

请按照以下顺序执行：
1. 阶段1：基于 intelligent-sql-job-generator.mdc 生成 Flink SQL
2. 阶段2：基于 intelligent-validation-workflow.mdc 进行数据验证
3. 阶段3：基于 intelligent-er-knowledge-base.mdc 更新ER知识库

生成完整的执行报告，包括质量评分和部署建议。
```

#### 步骤3：查看执行结果
```bash
# 查看执行报告
cat job/{domain}/workflow/end-to-end-execution-report-v3.md

# 查看生成的SQL
cat job/{domain}/sql/{domain}_wide_table_v3.sql

# 查看验证报告
cat job/{domain}/validation/validation-report-{domain}-v3.md

# 查看ER图
cat job/knowledge-base/er-schemas/domains/{domain}/generated-er-diagram-v3.md
```

### 方式2：配置文件驱动的执行

**适用场景**: 自动化环境、批量处理

#### 配置工作流
```yaml
# 编辑 job/ai-config/end-to-end-workflow-config.yml
workflow_config:
  execution_mode: "sequential"
  quality_gate_enforcement: "strict"
  
# 针对特定域的配置
environments:
  development:
    quality_gate_enforcement: "permissive"
  production:
    quality_gate_enforcement: "strict"
```

#### 执行工作流
```bash
# 使用配置文件执行（未来扩展）
ai-workflow execute \
  --input job/{domain}/{domain}-request-v3.md \
  --config job/ai-config/end-to-end-workflow-config.yml \
  --output-dir job/{domain}/
```

### 方式3：手动分阶段执行

**适用场景**: 调试、问题排查、精确控制

#### 阶段1：SQL生成
```bash
# 使用SQL生成规则
# 基于 intelligent-sql-job-generator.mdc
# 输入：job/{domain}/{domain}-request-v3.md
# 输出：SQL文件 + 部署配置 + 文档
```

#### 阶段2：数据验证
```bash
# 使用验证规则
# 基于 intelligent-validation-workflow.mdc
# 输入：SQL文件 + request文件
# 输出：验证报告 + 测试数据
```

#### 阶段3：ER知识库更新
```bash
# 使用ER知识库规则
# 基于 intelligent-er-knowledge-base.mdc
# 输入：request文件 + 验证结果
# 输出：ER图 + 知识库更新
```

## 📋 质量门控系统

### 质量门控设置

#### 阶段1质量门控
```yaml
phase_1_quality_gates:
  syntax_validation:
    requirement: "SQL语法必须正确"
    validation_method: "Flink SQL parser"
    
  logic_validation:
    requirement: "业务逻辑映射完整"
    validation_method: "字段映射检查"
    
  performance_validation:
    requirement: "包含性能优化"
    validation_method: "最佳实践检查"
    
  success_criteria:
    - "SQL文件成功生成且大小 > 1KB"
    - "包含必要的INSERT INTO和JOIN语句"
    - "通过语法验证"
```

#### 阶段2质量门控
```yaml
phase_2_quality_gates:
  overall_score:
    threshold: 85
    requirement: "综合评分 ≥ 85分"
    
  critical_issues:
    threshold: 0
    requirement: "Critical级别问题 = 0"
    
  data_consistency:
    threshold: 99
    requirement: "数据一致性 ≥ 99%"
    
  dimension_scores:
    sql_standardness: ≥ 90
    data_accuracy: ≥ 95
    performance: ≥ 80
    business_compliance: ≥ 85
```

#### 阶段3质量门控
```yaml
phase_3_quality_gates:
  conflict_resolution:
    requirement: "所有Critical冲突必须解决"
    
  consistency_check:
    requirement: "跨域一致性检查通过"
    
  completeness_check:
    requirement: "ER图完整性验证通过"
    
  success_criteria:
    - "ER图成功生成或冲突报告生成"
    - "知识库元数据正确更新"
    - "版本信息正确记录"
```

### 失败处理策略

#### 阶段失败处理
```yaml
failure_handling:
  phase_1_failure:
    action: "retry_with_fallback"
    max_retries: 3
    fallback: "使用模板生成基础SQL"
    
  phase_2_failure:
    critical_issues: "stop_workflow"
    warning_issues: "continue_with_warning"
    score_below_threshold: "generate_improvement_suggestions"
    
  phase_3_failure:
    conflicts_detected: "pause_for_manual_resolution"
    parsing_errors: "escalate_to_manual"
    consistency_failures: "trigger_global_consistency_check"
```

## 📊 监控和可观测性

### 执行监控

#### 实时状态监控
```yaml
real_time_monitoring:
  workflow_status:
    - "INITIATED" → "PHASE_1_RUNNING" → "PHASE_1_COMPLETED"
    - "PHASE_2_RUNNING" → "PHASE_2_COMPLETED"
    - "PHASE_3_RUNNING" → "PHASE_3_COMPLETED"
    - "COMPLETED_SUCCESS" | "FAILED" | "PAUSED"
    
  progress_tracking:
    phase_1: "SQL生成进度 (语法检查 → 逻辑验证 → 文件输出)"
    phase_2: "验证进度 (标准性检查 → 数据验证 → 报告生成)"
    phase_3: "知识库更新进度 (解析 → 冲突检测 → 更新)"
```

#### 性能指标
```yaml
performance_metrics:
  execution_time:
    total_duration: "目标 < 10分钟"
    phase_breakdown: "按阶段分解的执行时间"
    
  throughput:
    files_generated: "每分钟生成文件数"
    lines_of_code: "每分钟生成代码行数"
    
  resource_usage:
    memory_consumption: "内存使用峰值"
    cpu_utilization: "CPU使用率"
    disk_usage: "磁盘使用量"
```

#### 质量指标
```yaml
quality_metrics:
  success_rate:
    overall: "工作流整体成功率"
    by_phase: "各阶段成功率统计"
    
  quality_scores:
    average_scores: "各维度平均质量评分"
    trend_analysis: "质量评分趋势分析"
    
  issue_statistics:
    critical_issues: "Critical问题统计"
    warning_issues: "Warning问题统计"
    resolution_rate: "问题解决率"
```

### 告警系统

#### 告警规则
```yaml
alerting_rules:
  workflow_failure:
    condition: "workflow_status == 'FAILED'"
    severity: "critical"
    notification: ["email", "slack"]
    
  quality_degradation:
    condition: "overall_score < 85"
    severity: "warning"
    notification: ["slack"]
    
  execution_timeout:
    condition: "execution_time > timeout * 1.5"
    severity: "warning"
    notification: ["email"]
    
  conflict_detected:
    condition: "conflicts_count > 0"
    severity: "info"
    notification: ["email"]
```

## 🔧 配置和定制

### 工作流配置

#### 全局配置
```yaml
# job/ai-config/end-to-end-workflow-config.yml
workflow_config:
  execution_mode: "sequential"  # 执行模式
  quality_gate_enforcement: "strict"  # 质量门控严格程度
  
  timeouts:
    total_workflow: "30 minutes"
    phase_1_sql_generation: "5 minutes"
    phase_2_data_validation: "10 minutes"
    phase_3_er_kb_update: "8 minutes"
```

#### 环境特定配置
```yaml
environments:
  development:
    quality_gate_enforcement: "permissive"
    timeout_multiplier: 2.0
    detailed_logging: true
    
  testing:
    quality_gate_enforcement: "strict"
    timeout_multiplier: 1.5
    mock_external_dependencies: true
    
  production:
    quality_gate_enforcement: "strict"
    timeout_multiplier: 1.0
    enable_all_monitoring: true
```

### 业务域定制

#### 域特定配置
```yaml
# 错题本域配置示例
wrongbook_config:
  domain: "wrongbook"
  payload_entity: "WrongbookFixPayload"
  
  special_rules:
    chapter_matching: true
    soft_delete_handling: true
    
  quality_thresholds:
    join_success_rate: 95.0
    data_completeness: 98.0
    
  monitoring:
    business_metrics: ["fix_success_rate", "subject_distribution"]
```

## 📚 最佳实践

### 开发阶段

#### 1. Request文件准备
```yaml
best_practices:
  file_structure:
    - "确保所有必需的YAML sections完整"
    - "ER图定义清晰，包含所有关联关系"
    - "字段映射覆盖所有业务字段"
    
  data_quality:
    - "提供真实的业务场景和数据示例"
    - "明确定义业务规则和约束条件"
    - "包含异常情况的处理逻辑"
```

#### 2. 迭代开发
```yaml
iterative_development:
  start_small:
    - "从核心业务逻辑开始"
    - "逐步添加复杂的业务规则"
    
  incremental_validation:
    - "每次修改后运行完整工作流"
    - "关注质量评分的变化趋势"
    
  version_management:
    - "为每次重大变更创建新版本"
    - "保持向后兼容性"
```

### 测试阶段

#### 1. 验证策略
```yaml
validation_strategy:
  comprehensive_testing:
    - "在测试环境运行24小时稳定性测试"
    - "使用真实数据进行端到端验证"
    
  performance_testing:
    - "验证处理性能满足要求"
    - "测试不同数据量下的表现"
    
  business_validation:
    - "与业务团队验证结果正确性"
    - "确认异常情况处理符合预期"
```

#### 2. 问题解决
```yaml
problem_resolution:
  systematic_approach:
    - "从低级别问题开始修复"
    - "优先解决Critical级别问题"
    
  root_cause_analysis:
    - "分析问题的根本原因"
    - "避免临时修复方案"
    
  documentation:
    - "记录所有问题和解决方案"
    - "更新最佳实践指南"
```

### 生产阶段

#### 1. 部署策略
```yaml
deployment_strategy:
  gradual_rollout:
    - "使用蓝绿部署或金丝雀发布"
    - "监控关键业务指标"
    
  rollback_preparation:
    - "准备回滚计划和程序"
    - "保留上一版本的配置"
    
  monitoring_setup:
    - "配置完整的监控和告警"
    - "建立运维响应流程"
```

#### 2. 持续改进
```yaml
continuous_improvement:
  performance_optimization:
    - "基于生产数据持续优化"
    - "监控并优化资源使用"
    
  knowledge_base_maintenance:
    - "定期检查ER知识库一致性"
    - "及时处理冲突和不一致"
    
  process_refinement:
    - "基于实际使用经验改进工作流"
    - "优化质量门控和阈值设置"
```

## 🚨 故障排查

### 常见问题

#### Q1: 工作流在阶段1失败
```yaml
problem: "SQL生成阶段失败"
symptoms:
  - "无法解析request文件"
  - "生成的SQL语法错误"
  - "业务逻辑映射不完整"
  
troubleshooting:
  1. "检查request文件YAML语法"
  2. "验证所有必需sections存在"
  3. "确认ER图定义完整"
  4. "检查字段映射配置"
  
solutions:
  - "使用YAML验证工具检查格式"
  - "参考working示例修正配置"
  - "联系业务团队确认逻辑"
```

#### Q2: 验证评分过低
```yaml
problem: "阶段2验证评分低于阈值"
symptoms:
  - "综合评分 < 85分"
  - "存在Critical级别问题"
  - "数据一致性验证失败"
  
troubleshooting:
  1. "查看详细的验证报告"
  2. "分析各维度评分"
  3. "检查Critical问题清单"
  4. "验证测试数据质量"
  
solutions:
  - "应用验证报告中的修复建议"
  - "优化SQL代码质量"
  - "改进业务逻辑实现"
  - "增强错误处理"
```

#### Q3: ER知识库冲突
```yaml
problem: "阶段3检测到冲突"
symptoms:
  - "知识库更新被暂停"
  - "生成冲突报告"
  - "一致性检查失败"
  
troubleshooting:
  1. "查看冲突报告详情"
  2. "分析冲突类型和严重程度"
  3. "检查变更的合理性"
  4. "评估解决方案选项"
  
solutions:
  - "按照冲突报告建议解决"
  - "与相关团队协调变更"
  - "考虑版本分支策略"
  - "更新业务规则文档"
```

### 调试技巧

#### 1. 日志分析
```yaml
log_analysis:
  structured_logs:
    - "使用correlation_id追踪请求"
    - "分析每个阶段的详细日志"
    
  error_patterns:
    - "查找重复出现的错误模式"
    - "分析错误发生的时间点"
    
  performance_logs:
    - "检查执行时间分布"
    - "识别性能瓶颈"
```

#### 2. 状态检查
```yaml
state_inspection:
  workflow_state:
    file: "job/{domain}/.workflow/state.json"
    content: "检查当前执行状态和进度"
    
  checkpoint_data:
    - "检查各阶段的检查点数据"
    - "验证中间结果的正确性"
    
  recovery_options:
    - "确定可以恢复的检查点"
    - "评估重新执行的必要性"
```

## 📖 示例和案例

### 成功案例：错题本业务域

#### 输入文件
```yaml
# job/wrongbook/wrongbook-request-v3.md
job_info:
  name: "错题本修正记录实时宽表"
  domain: "wrongbook"
  event_type: "fix"
  
# 完整的field_mapping, join_relationships, ER图定义
```

#### 执行结果
```yaml
execution_summary:
  total_duration: "4分钟35秒"
  overall_score: "93.45/100"
  status: "COMPLETED_SUCCESS"
  
  generated_artifacts:
    sql_files: 4
    config_files: 2
    documentation: 3
    knowledge_base: 4
    
  key_achievements:
    - "完整的三层维表关联逻辑"
    - "复杂业务规则正确实现"
    - "知识库无冲突更新"
    - "生产就绪的部署配置"
```

#### 经验总结
```yaml
lessons_learned:
  preparation:
    - "详细的ER图定义显著提高成功率"
    - "完整的业务规则描述减少返工"
    
  execution:
    - "质量门控有效保证输出质量"
    - "分阶段执行便于问题定位"
    
  outcomes:
    - "自动化大幅提升开发效率"
    - "标准化确保代码质量一致性"
```

## 🔄 版本管理和升级

### 版本策略

#### 语义版本控制
```yaml
version_strategy:
  major_version: "重大架构变更或不兼容变更"
  minor_version: "新功能添加或显著改进"
  patch_version: "bug修复和小改进"
  
  current_versions:
    workflow_engine: "1.0.0"
    sql_generator: "3.0.0"
    validation_engine: "3.0.0"
    er_knowledge_base: "1.0.0"
```

#### 升级流程
```yaml
upgrade_process:
  preparation:
    - "备份现有配置和知识库"
    - "测试新版本兼容性"
    
  execution:
    - "逐步升级各组件"
    - "验证升级结果"
    
  rollback:
    - "准备回滚方案"
    - "监控升级后的稳定性"
```

---

## 📚 相关文档

- [AI规则文件文档](../.cursor/rules/)
  - [端到端工作流规则](../.cursor/rules/intelligent-end-to-end-workflow.mdc)
  - [SQL生成规则](../.cursor/rules/intelligent-sql-job-generator.mdc)
  - [数据验证规则](../.cursor/rules/intelligent-validation-workflow.mdc)
  - [ER知识库规则](../.cursor/rules/intelligent-er-knowledge-base.mdc)

- [配置文件文档](../ai-config/)
  - [工作流配置](../ai-config/end-to-end-workflow-config.yml)
  - [验证配置](../ai-config/validation-config.yml)

- [示例和模板](../wrongbook/)
  - [错题本执行报告](../wrongbook/workflow/end-to-end-execution-report-v3.md)
  - [请求文件模板](../flink-sql-request-template-v3.md)

---

*此指南基于端到端工作流 AI Agent v1.0 设计*
*涵盖了从开发到生产的完整工作流使用方法*
*持续更新中，欢迎反馈和建议*
