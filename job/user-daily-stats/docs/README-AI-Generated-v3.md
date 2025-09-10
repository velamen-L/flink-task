# 用户日统计作业 - AI生成文档 v3.0

## 📋 作业概览

| 项目 | 值 |
|------|-----|
| 作业名称 | 用户日统计作业 (user-daily-stats) |
| 作业类型 | 跨域统计分析 |
| 业务域 | 多域 (wrongbook + answer + user) |
| 生成时间 | 2024-12-27 |
| AI生成器 | intelligent-sql-job-generator.mdc v3.0 |
| 目标平台 | 阿里云VVR (实时计算Flink版) |

## 🎯 业务目标

### 核心功能
- **跨域用户行为分析**: 整合错题本、答题、用户登录三个域的数据
- **日活跃度统计**: 按日统计用户在各业务域的活跃情况
- **学习效果评估**: 计算学习参与度得分和综合指标
- **用户画像增强**: 结合用户基础信息提供完整画像

### 业务价值
- **数据统一**: 将分散的用户行为数据统一到一个宽表
- **实时洞察**: 实时了解用户的学习活跃度和效果
- **精准分析**: 支持个性化推荐和用户流失预警
- **决策支持**: 为产品优化和运营决策提供数据支撑

## 🏗️ 技术架构

### 数据流架构
```
[Kafka消息流]
     ↓
[多域事件源] → [跨域JOIN处理] → [指标计算] → [结果输出]
     ↓              ↓              ↓           ↓
[错题本域]     [FULL OUTER]    [15个指标]  [MaxCompute]
[答题域]       [JOIN策略]      [综合评分]  [实时Upsert]
[用户域]       [空值处理]      [画像增强]
```

### 核心技术特性
- **多流JOIN**: 使用FULL OUTER JOIN确保数据完整性
- **事件时间处理**: 基于event_time进行准确的时间窗口计算
- **智能空值处理**: 对缺失域数据进行合理的默认值填充
- **维表关联**: 集成用户画像信息增强统计维度
- **实时更新**: 支持upsert操作的增量更新

## 📊 数据模型

### 输入数据源

#### 1. 错题本事件流 (wrongbook_events)
**Topic**: `biz_statistic_wrongbook`
**事件类型**: `wrongbook_fix`
**关键字段**:
```json
{
  "domain": "wrongbook",
  "type": "wrongbook_fix", 
  "payload": {
    "userId": "用户ID",
    "fixResult": "订正结果(0/1)"
  },
  "event_time": "事件时间"
}
```

#### 2. 答题事件流 (answer_events)
**Topic**: `biz_statistic_answer`
**事件类型**: `answer_submit`
**关键字段**:
```json
{
  "domain": "answer",
  "type": "answer_submit",
  "payload": {
    "userId": "用户ID",
    "score": "得分"
  },
  "event_time": "事件时间"
}
```

#### 3. 用户事件流 (user_events)
**Topic**: `biz_statistic_user`
**事件类型**: `user_login`
**关键字段**:
```json
{
  "domain": "user",
  "type": "user_login",
  "payload": {
    "userId": "用户ID"
  },
  "event_time": "事件时间"
}
```

#### 4. 用户画像维表 (user_profile)
**连接器**: JDBC (MySQL)
**缓存策略**: 1小时TTL, 50万行缓存
**关键字段**:
- user_id: 用户ID (主键)
- user_name: 用户姓名
- grade: 年级
- city: 城市

### 输出数据模型

#### 用户日统计表 (dws_user_daily_stats)
**存储**: MaxCompute (ODPS)
**更新模式**: Upsert (主键: user_id + stat_date)

| 字段分类 | 字段名 | 类型 | 描述 | 业务规则 |
|---------|--------|------|------|----------|
| **基础维度** | user_id | STRING | 用户ID | 必填 |
| | stat_date | DATE | 统计日期 | 按天分区 |
| **错题本指标** | wrongbook_fix_count | BIGINT | 错题订正次数 | ≥0，无活动为0 |
| | fix_success_count | BIGINT | 订正成功次数 | ≤wrongbook_fix_count |
| | fix_success_rate | DOUBLE | 订正成功率 | [0,1] |
| **答题指标** | answer_submit_count | BIGINT | 答题提交次数 | ≥0 |
| | avg_score | DOUBLE | 平均得分 | [0,100] |
| | high_score_count | BIGINT | 高分次数(≥80分) | ≤answer_submit_count |
| **用户指标** | login_count | BIGINT | 登录次数 | ≥0 |
| | first_login_time | TIMESTAMP(3) | 首次登录时间 | 当日最早 |
| | last_login_time | TIMESTAMP(3) | 最后登录时间 | 当日最晚 |
| | online_duration | BIGINT | 在线时长(分钟) | ≥0 |
| **综合指标** | total_activity_count | BIGINT | 总活跃次数 | 三域活动总和 |
| | learning_engagement_score | DOUBLE | 学习参与度得分 | [0,1] |
| **画像信息** | user_name | STRING | 用户姓名 | 来自维表 |
| | grade | STRING | 年级 | 来自维表 |
| | city | STRING | 城市 | 来自维表 |
| **元数据** | update_time | TIMESTAMP(3) | 更新时间 | 处理时间戳 |

## 🔄 业务逻辑

### 跨域JOIN策略
```sql
-- 使用FULL OUTER JOIN确保数据完整性
错题本域统计 
FULL OUTER JOIN 答题域统计 ON (user_id, stat_date)
FULL OUTER JOIN 用户域统计 ON (user_id, stat_date)
LEFT JOIN 用户画像维表 ON user_id
```

### 核心算法

#### 1. 学习参与度得分算法
```sql
learning_engagement_score = 
  CASE 
    WHEN 有错题本活动 AND 有答题活动 THEN
      (订正成功率 * 0.4) + (平均得分/100 * 0.6)
    WHEN 仅有错题本活动 THEN
      订正成功率 * 0.4
    WHEN 仅有答题活动 THEN  
      平均得分/100 * 0.6
    ELSE 0.0
  END
```

#### 2. 空值处理策略
- **数值字段**: 使用COALESCE()设置默认值0
- **时间字段**: 保留NULL值表示无相关活动
- **比率字段**: 分母为0时设置为0.0
- **用户ID**: 优先级 wrongbook > answer > user

#### 3. 数据质量保证
- **有效性检查**: total_activity_count > 0
- **时间范围**: 只处理最近7天数据
- **非空约束**: user_id和stat_date必须非空

## ⚡ 性能优化

### JOIN优化策略
1. **维表缓存**: 用户画像表缓存1小时，50万行
2. **JOIN顺序**: 先处理大表关联，后关联维表
3. **过滤下推**: 在JOIN前进行域和时间过滤
4. **状态TTL**: 设置24小时状态TTL避免内存泄漏

### 资源配置建议
```yaml
# VVR平台配置
parallelism: 16                    # 并行度
taskmanager.slots: 8               # 每个TM的slot数
taskmanager.memory: 6g             # TM内存
state.backend: rocksdb             # 状态后端
checkpoint.interval: 60s           # 检查点间隔
```

### 监控指标
- **处理用户数**: daily_stats_users_processed_total
- **跨域JOIN成功率**: cross_domain_join_success_rate (>90%)
- **处理延迟**: user_daily_stats_processing_delay (<10分钟)
- **活跃用户占比**: active_users_ratio

## 🚀 部署说明

### VVR平台部署
```bash
# 创建作业域脚手架
gradle createFlinkDomain -Pdomain=user-daily-stats

# 部署到VVR平台
kubectl apply -f job/user-daily-stats/deployment/deploy-user-daily-stats-v3.yaml

# 监控作业状态
kubectl get flinkdeployment user-daily-stats-v3 -n vvr-flink-ai
```

### 环境依赖
- **VVR平台**: 阿里云实时计算Flink版 4.0.14+
- **Kafka集群**: 支持SASL认证的Kafka集群
- **MySQL**: 用户画像维表存储
- **MaxCompute**: 结果数据存储

### 配置检查清单
- [ ] Kafka Topic创建和权限配置
- [ ] MySQL用户画像表准备
- [ ] MaxCompute项目和表权限
- [ ] VVR工作空间和资源配额
- [ ] 监控告警规则配置

## 📈 监控运维

### 关键监控指标
| 指标类型 | 指标名称 | 正常范围 | 告警阈值 |
|---------|----------|----------|----------|
| 业务指标 | 日处理用户数 | 10K-100K | <5K或>200K |
| 质量指标 | 跨域JOIN成功率 | >95% | <90% |
| 性能指标 | P95处理延迟 | <5分钟 | >10分钟 |
| 系统指标 | CPU使用率 | 50-70% | >80% |
| 系统指标 | 内存使用率 | 60-80% | >90% |

### 常见问题处理

#### 1. 跨域JOIN成功率低
**原因**: 事件时间不对齐或数据倾斜
**解决**: 
- 检查事件时间字段格式
- 调整Watermark设置
- 优化数据分区策略

#### 2. 处理延迟过高
**原因**: 背压或资源不足
**解决**:
- 增加并行度和TM数量
- 优化状态存储配置
- 检查下游MaxCompute写入性能

#### 3. 内存使用率高
**原因**: 维表缓存过大或状态积累
**解决**:
- 调整维表缓存大小和TTL
- 设置合理的状态TTL
- 优化RocksDB配置

## 🔄 扩展规划

### 短期优化 (1-2月)
- **性能调优**: 基于运行数据优化并行度和资源配置
- **监控完善**: 增加更多业务监控指标
- **数据质量**: 增强数据清洗和异常检测逻辑

### 中期发展 (3-6月)  
- **行为序列**: 增加用户行为序列分析能力
- **实时推荐**: 支持实时个性化推荐数据准备
- **流失预警**: 基于活跃度变化进行用户流失预警

### 长期规划 (6月+)
- **AI增强**: 集成机器学习模型进行智能分析
- **多维分析**: 支持更多维度的用户行为分析
- **实时特征**: 为实时推荐系统提供特征数据

---

*本文档由AI Agent基于intelligent-sql-job-generator.mdc规则自动生成*
