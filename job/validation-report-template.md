# Flink SQL 验证报告模板 v3.0

## 📋 验证概览

| 项目 | 值 |
|------|-----|
| **验证时间** | `{timestamp}` |
| **SQL文件** | `{sql_file_path}` |
| **业务域** | `{business_domain}` |
| **请求文件** | `{request_file_path}` |
| **验证方式** | AI Agent + 规则驱动 |
| **验证版本** | v3.0 |

---

## 🔍 SQL标准性验证结果

### ✅ 语法检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| SQL语法正确性 | `{syntax_status}` | `{syntax_details}` |
| Flink特定语法 | `{flink_syntax_status}` | `{flink_syntax_details}` |
| 关键字冲突检查 | `{keyword_status}` | `{keyword_details}` |
| 语句完整性 | `{completeness_status}` | `{completeness_details}` |

**详细信息**：
```
{syntax_detailed_info}
```

### 🔗 逻辑一致性验证

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 表结构验证 | `{table_structure_status}` | `{table_structure_details}` |
| 字段映射检查 | `{field_mapping_status}` | `{field_mapping_details}` |
| JOIN逻辑分析 | `{join_logic_status}` | `{join_logic_details}` |
| 数据类型兼容性 | `{data_type_status}` | `{data_type_details}` |

**详细信息**：
```
{logic_detailed_info}
```

### ⚡ 性能分析

| 分析项 | 评估结果 | 建议 |
|--------|----------|------|
| 执行计划分析 | `{execution_plan_result}` | `{execution_plan_advice}` |
| JOIN策略 | `{join_strategy_result}` | `{join_strategy_advice}` |
| 索引使用 | `{index_usage_result}` | `{index_usage_advice}` |
| 资源消耗预估 | `{resource_estimation}` | `{resource_advice}` |

**详细信息**：
```
{performance_detailed_info}
```

---

## 📊 数据准确性验证结果

### 🎯 功能正确性验证

| 验证项 | 状态 | 详情 |
|--------|------|------|
| 字段转换逻辑 | `{field_transform_status}` | `{field_transform_details}` |
| 业务规则实现 | `{business_rule_status}` | `{business_rule_details}` |
| 时间处理逻辑 | `{time_processing_status}` | `{time_processing_details}` |
| JSON数据解析 | `{json_parsing_status}` | `{json_parsing_details}` |

**验证SQL**：
```sql
{functional_validation_sql}
```

### 🔄 数据一致性验证

| 验证项 | 预期值 | 实际值 | 状态 |
|--------|--------|--------|------|
| 记录数一致性 | `{expected_record_count}` | `{actual_record_count}` | `{record_count_status}` |
| 业务指标准确性 | `{expected_metrics}` | `{actual_metrics}` | `{metrics_status}` |
| 维表关联完整性 | `{expected_join_rate}` | `{actual_join_rate}` | `{join_rate_status}` |
| 数据完整性 | `{expected_completeness}` | `{actual_completeness}` | `{completeness_status}` |

**验证SQL**：
```sql
{consistency_validation_sql}
```

### 🎯 端到端验证

| 验证场景 | 状态 | 说明 |
|----------|------|------|
| 完整数据流 | `{e2e_flow_status}` | `{e2e_flow_details}` |
| 异常数据处理 | `{exception_handling_status}` | `{exception_handling_details}` |
| 边界条件测试 | `{boundary_test_status}` | `{boundary_test_details}` |
| 并发处理验证 | `{concurrency_status}` | `{concurrency_details}` |

**测试数据**：
```sql
{e2e_test_data}
```

---

## 🧪 验证测试用例

### 📝 自动生成测试数据

```sql
-- ================================================================
-- 测试数据构造 SQL
-- 基于业务场景智能生成的测试用例
-- ================================================================

{test_data_generation_sql}
```

### ✨ 验证SQL集合

```sql
-- ================================================================
-- 数据一致性验证 SQL
-- ================================================================

{data_consistency_validation_sql}

-- ================================================================
-- 业务逻辑验证 SQL  
-- ================================================================

{business_logic_validation_sql}

-- ================================================================
-- 性能基准验证 SQL
-- ================================================================

{performance_benchmark_sql}
```

---

## 🚨 问题汇总

### ❌ Critical Issues (阻塞问题)

{critical_issues_list}

### ⚠️ Warning Issues (警告问题)

{warning_issues_list}

### 💡 Info Issues (优化建议)

{info_issues_list}

---

## 🔧 修复方案

### 📝 SQL修复代码

```sql
-- ================================================================
-- 修复后的完整 SQL 代码
-- 基于验证结果自动生成的优化版本
-- ================================================================

{fixed_sql_code}
```

### ⚙️ 配置优化建议

**并行度配置**：
```yaml
parallelism: {recommended_parallelism}
checkpoint.interval: {recommended_checkpoint_interval}
state.backend: {recommended_state_backend}
```

**资源配置**：
```yaml
taskmanager.memory.process.size: {recommended_tm_memory}
jobmanager.memory.process.size: {recommended_jm_memory}
parallelism.default: {recommended_default_parallelism}
```

**性能优化**：
```yaml
{performance_optimization_config}
```

---

## 📊 质量评分

### 🎯 综合评分

| 维度 | 得分 | 权重 | 加权得分 | 状态 |
|------|------|------|----------|------|
| **SQL标准性** | `{sql_standardness_score}`/100 | 25% | `{sql_weighted_score}` | `{sql_status}` |
| **数据准确性** | `{data_accuracy_score}`/100 | 35% | `{data_weighted_score}` | `{data_status}` |
| **性能表现** | `{performance_score}`/100 | 20% | `{performance_weighted_score}` | `{performance_status}` |
| **业务合规性** | `{business_compliance_score}`/100 | 20% | `{business_weighted_score}` | `{business_status}` |
| **综合评分** | **`{total_score}`/100** | 100% | **`{total_score}`** | **`{overall_status}`** |

### 🚦 上线建议

**评分说明**：
- **≥ 95分**: ✅ 可直接上线生产环境
- **85-94分**: ⚠️ 可部署测试环境，建议优化后上线
- **70-84分**: 🔄 需要修复主要问题后重新验证
- **< 70分**: ❌ 存在严重问题，禁止部署

**当前状态**: `{deployment_recommendation}`

**风险评估**：
```
{risk_assessment}
```

---

## 🔄 持续监控配置

### 📈 数据质量监控

```sql
-- ================================================================
-- 数据质量监控 SQL
-- 可集成到监控系统的质量检查脚本
-- ================================================================

{data_quality_monitoring_sql}
```

### 🚨 告警配置建议

**告警规则**：
```yaml
# 数据完整性告警
- name: data_completeness_alert
  condition: completeness_rate < 95
  severity: critical
  message: "数据完整性低于95%"

# JOIN关联率告警  
- name: join_rate_alert
  condition: join_success_rate < 90
  severity: warning
  message: "维表关联成功率低于90%"

# 数据延迟告警
- name: data_latency_alert  
  condition: processing_delay > 300
  severity: warning
  message: "数据处理延迟超过5分钟"

{additional_alert_config}
```

### 📊 可视化配置

**监控看板指标**：
```yaml
dashboard_metrics:
  - metric: record_count_per_hour
    title: "每小时记录数"
    type: line_chart
    
  - metric: data_completeness_rate
    title: "数据完整性率"
    type: gauge
    threshold: 95
    
  - metric: join_success_rate
    title: "维表关联成功率"
    type: gauge
    threshold: 90
    
  - metric: processing_latency
    title: "处理延迟"
    type: histogram
    
{additional_dashboard_config}
```

---

## 📋 验证总结

### ✅ 验证通过项
{passed_validation_items}

### ❌ 需要修复项
{failed_validation_items}

### 📈 优化建议
{optimization_recommendations}

### 🔄 下一步行动
{next_actions}

---

## 📚 附录

### 🔗 相关文档
- [Flink SQL开发规范](../docs/flink-sql-development-standards.md)
- [数据质量监控指南](../docs/data-quality-monitoring-guide.md)
- [性能调优最佳实践](../docs/performance-tuning-best-practices.md)

### 📞 联系信息
- **验证工具**: AI Agent v3.0
- **规则版本**: intelligent-validation-workflow v1.0
- **生成时间**: `{generation_timestamp}`
- **验证ID**: `{validation_id}`

---

*此报告由 AI Agent 基于 intelligent-validation-workflow.mdc 规则智能生成*
*验证结果基于当前提供的数据和配置，实际生产环境可能存在差异*
