# 智能FlinkSQL作业生成模板

## 📋 基本信息

```yaml
# 作业元信息
job_info:
  name: "{job_name}"                    # 作业名称（自动生成）
  description: "{description}"          # 业务描述（用户输入）
  business_domain: "{domain}"          # 业务域
  complexity: "{simple|medium|complex}" # 复杂度评级
  priority: "{low|medium|high}"        # 优先级
  estimated_qps: "{qps}"               # 预估QPS
  created_by: "{user}"                 # 创建者
  created_at: "{timestamp}"            # 创建时间
```

## 🤖 AI辅助配置

### 自然语言需求描述
```text
请用自然语言描述您的业务需求，AI将自动解析并生成配置：

示例：
"我需要统计每个用户在错题本中的订正情况，包括订正次数、成功率、涉及的知识点等，
按天进行聚合，并且需要关联用户信息和知识点信息，结果保存到ODPS表中。"

您的需求：
{user_requirement}
```

### AI解析结果预览
```yaml
# AI自动解析的结果（供用户确认和调整）
parsed_requirement:
  source_tables:
    - table: "biz_statistic_wrongbook"
      events: ["wrongbook_fix"]
      key_fields: ["userId", "questionId", "patternId"]
  
  dim_tables:
    - table: "user_profile"
      join_key: "userId"
      purpose: "获取用户基本信息"
    - table: "tower_pattern"  
      join_key: "patternId"
      purpose: "获取知识点信息"
  
  business_logic:
    - "按用户ID分组"
    - "按天聚合统计"
    - "计算订正成功率"
    - "关联维表获取详细信息"
  
  output_table: "dws_user_wrongbook_daily_stats"
  output_format: "ODPS宽表"
```

## 🗄️ 数据源配置

### 主事件流配置
```yaml
source_config:
  # 主事件流（标准BusinessEvent格式）
  main_stream:
    table_name: "biz_statistic_{domain}"
    event_filter:
      domain: "{domain}"               # 事件域过滤
      types: ["{event_type}"]          # 事件类型过滤
    
    # Payload结构定义（AI自动识别）
    payload_schema:
      fields:
        - name: "{field1}"
          type: "STRING"
          description: "{description}"
          required: true
        - name: "{field2}"
          type: "BIGINT" 
          description: "{description}"
          required: false
      
    # 数据质量配置
    quality_rules:
      - field: "userId"
        rule: "NOT NULL"
        description: "用户ID不能为空"
      - field: "timestamp"
        rule: "BETWEEN CURRENT_TIME - INTERVAL '7' DAY AND CURRENT_TIME"
        description: "只处理最近7天的数据"
```

### 维表配置
```yaml
dim_tables:
  - table_name: "{dim_table_name}"
    alias: "{alias}"
    
    # 表结构（AI从Catalog自动获取）
    schema:
      primary_key: "{key}"
      fields: []  # AI自动填充
    
    # JOIN配置（AI智能推荐）
    join_config:
      type: "LEFT"                    # AI推荐JOIN类型
      condition: "{join_condition}"   # AI生成JOIN条件
      timing: "FOR SYSTEM_TIME AS OF PROCTIME()"
    
    # 缓存配置（AI性能优化）
    cache_config:
      enabled: true
      ttl: "30min"
      max_rows: 100000
      
    # 业务用途说明
    business_purpose: "{purpose}"     # AI理解的业务用途
```

## 🎯 业务逻辑配置

### 聚合逻辑
```yaml
aggregation_logic:
  # 分组字段（AI智能识别）
  group_by:
    - field: "payload.userId"
      alias: "user_id"
      description: "按用户分组"
    - field: "DATE(event_time)"
      alias: "stat_date"  
      description: "按天分组"
  
  # 聚合计算（AI根据需求生成）
  metrics:
    - name: "fix_count"
      expression: "COUNT(*)"
      description: "订正总次数"
    - name: "success_count"
      expression: "COUNT(CASE WHEN payload.fixResult = 1 THEN 1 END)"
      description: "订正成功次数"
    - name: "success_rate"
      expression: "success_count * 1.0 / NULLIF(fix_count, 0)"
      description: "订正成功率"
  
  # 业务规则（AI从知识库应用）
  business_rules:
    - rule: "只统计非删除的记录"
      condition: "payload.isDelete = false"
    - rule: "过滤测试用户"
      condition: "user_profile.userType != 'test'"
```

### 时间窗口配置
```yaml
time_window:
  # AI推荐的窗口策略
  strategy: "{TUMBLING|SLIDING|SESSION}"
  window_size: "{size}"
  slide_size: "{slide}"              # 仅SLIDING窗口
  
  # 迟到数据处理
  late_data:
    allowed_lateness: "5min"
    handling_strategy: "UPDATE"      # UPDATE|IGNORE|SIDEOUTPUT
```

## 📊 输出配置

### 结果表配置
```yaml
output_config:
  # 主要输出表
  primary_output:
    table_name: "{output_table}"
    connector: "odps"
    
    # 字段映射（AI智能生成）
    field_mapping:
      - source: "user_id"
        target: "user_id"
        type: "STRING"
        description: "用户ID"
      - source: "stat_date"
        target: "stat_date"
        type: "DATE"
        description: "统计日期"
      # AI自动生成更多字段映射...
    
    # 分区策略（AI性能优化）
    partition:
      fields: ["stat_date"]
      strategy: "DAILY"
  
  # 可选的实时输出
  realtime_output:
    enabled: false
    topic: "{kafka_topic}"
    format: "JSON"
```

## ⚡ 性能优化配置

### AI智能优化建议
```yaml
performance_optimization:
  # AI分析的性能瓶颈
  bottlenecks:
    - type: "JOIN_PERFORMANCE"
      description: "大表JOIN可能造成性能问题"
      suggestion: "建议增加JOIN条件过滤"
    - type: "DATA_SKEW"
      description: "数据倾斜风险"
      suggestion: "考虑增加随机前缀"
  
  # AI推荐的优化策略
  optimizations:
    - strategy: "PREDICATE_PUSHDOWN"
      enabled: true
      description: "谓词下推优化"
    - strategy: "MINI_BATCH"
      enabled: true
      config:
        batch_size: 1000
        latency: "1s"
```

## 🔍 质量检查配置

### 数据质量规则
```yaml
quality_checks:
  # 自动生成的质量检查
  auto_generated:
    - check_type: "NULL_CHECK"
      fields: ["user_id", "stat_date"]
      threshold: 0.95                 # 95%非空率
    - check_type: "RANGE_CHECK"
      field: "success_rate"
      min_value: 0.0
      max_value: 1.0
  
  # 业务规则检查
  business_rules:
    - rule: "用户活跃度合理性检查"
      expression: "fix_count BETWEEN 0 AND 1000"
      severity: "WARNING"
    - rule: "成功率合理性检查"
      expression: "success_rate BETWEEN 0.0 AND 1.0"
      severity: "ERROR"
```

## 📝 AI生成日志

### 生成过程记录
```yaml
generation_log:
  ai_model: "GPT-4"
  generation_time: "{timestamp}"
  confidence_score: 0.92              # AI生成的置信度
  
  # 关键决策记录
  key_decisions:
    - decision: "选择LEFT JOIN关联用户表"
      reason: "部分用户可能没有完整档案信息"
      confidence: 0.95
    - decision: "使用TUMBLING窗口"
      reason: "日统计场景适合无重叠窗口"
      confidence: 0.88
  
  # 知识库匹配记录
  knowledge_matches:
    - source: "template_library"
      template: "user_daily_stats_template"
      similarity: 0.89
    - source: "business_rules"
      rule: "wrongbook_calculation_rules"
      applied: true
  
  # 需要人工确认的项目
  manual_review_items:
    - item: "输出表分区策略"
      reason: "需要确认具体的分区需求"
      priority: "HIGH"
```

## 🎛️ 用户交互配置

### 可调整参数
```yaml
user_adjustable:
  # 高优先级参数（用户必须确认）
  high_priority:
    - parameter: "output_table_name"
      current_value: "{ai_suggestion}"
      description: "输出表名"
      validation: "^[a-z][a-z0-9_]*$"
    
  # 中优先级参数（用户可选调整）  
  medium_priority:
    - parameter: "cache_ttl"
      current_value: "30min"
      options: ["10min", "30min", "1hour", "2hour"]
    
  # 低优先级参数（AI自动处理）
  low_priority:
    - parameter: "mini_batch_size"
      current_value: 1000
      range: [100, 10000]
```

---

## 🚀 使用说明

### 1. 需求输入方式
- **自然语言**：直接描述业务需求，AI自动解析
- **模板选择**：从预定义模板中选择最相似的
- **参数配置**：在AI生成基础上微调参数

### 2. AI辅助流程
1. 需求理解 → 2. 表结构匹配 → 3. SQL生成 → 4. 质量检查 → 5. 优化建议

### 3. 质量保证
- AI置信度评分
- 多维度质量检查
- 专家审核机制
- 持续反馈优化

这个模板将成为AI Coding平台的核心，结合大语言模型的理解能力和企业知识库的专业性，实现真正的智能化FlinkSQL开发！
