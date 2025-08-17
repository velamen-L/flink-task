#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
阿里云Flink SQL作业生成器（简化版，不依赖jinja2）
根据配置文件生成可直接在阿里云Flink SQL平台运行的SQL文件

@author: yangfanlin
@date: 2025-01-17
"""

import json
import os
import sys
from datetime import datetime

def generate_aliyun_flink_sql(config):
    """生成阿里云Flink SQL作业"""
    
    domain = config["domain"]
    job_name = config["job_name"]
    source_table = config["source_table"]
    dim_tables = config.get("dim_tables", [])
    result_table = config["result_table"]
    event_types = config.get("event_types", [])
    parallelism = config.get("parallelism", 2)
    
    # 生成SQL内容
    sql_content = f"""-- {job_name}
-- 阿里云Flink SQL作业脚本
-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
-- 业务域: {domain}
-- 
-- 使用说明：
-- 1. 将此SQL内容复制到阿里云Flink SQL开发平台
-- 2. 根据实际环境修改配置参数（用${{}}包围的变量）
-- 3. 创建作业并提交运行

-- =============================================
-- 1. 创建Kafka源表
-- =============================================
CREATE TABLE {source_table['name']} (
"""
    
    # 添加源表字段
    for i, field in enumerate(source_table['fields']):
        comment = f" COMMENT '{field['comment']}'" if field.get('comment') else ""
        comma = "," if i < len(source_table['fields']) - 1 else ""
        sql_content += f"    {field['name']:<20} {field['type']:<15}{comment}{comma}\n"
    
    sql_content += """    -- 计算列：处理时间
    proc_time AS PROCTIME(),
    -- 计算列：事件时间
    event_time AS TO_TIMESTAMP_LTZ(`timestamp`, 3),
    -- 水印定义：允许5秒乱序
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = '${ kafka.input.topic }',
    'properties.bootstrap.servers' = '${ kafka.bootstrap.servers }',
    'properties.group.id' = '${ kafka.group.id }',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'json',
    'json.fail-on-missing-field' = 'false',
    'json.ignore-parse-errors' = 'true'
);

"""
    
    # 添加维表
    if dim_tables:
        sql_content += """-- =============================================
-- 2. 创建MySQL维表
-- =============================================
"""
        for dim_table in dim_tables:
            sql_content += f"-- {dim_table['name']} 维表\n"
            sql_content += f"CREATE TABLE {dim_table['name']} (\n"
            
            for i, field in enumerate(dim_table['fields']):
                comment = f" COMMENT '{field['comment']}'" if field.get('comment') else ""
                comma = "," if i < len(dim_table['fields']) - 1 else ""
                sql_content += f"    {field['name']:<20} {field['type']:<15}{comment}{comma}\n"
            
            sql_content += f"""    PRIMARY KEY ({dim_table['key_field']}) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'url' = '${{ mysql.url }}',
    'username' = '${{ mysql.username }}',
    'password' = '${{ mysql.password }}',
    'table-name' = '{dim_table['name']}',
    -- 维表查询缓存配置
    'lookup.cache.max-rows' = '1000',
    'lookup.cache.ttl' = '60s',
    'lookup.max-retries' = '3',
    'lookup.cache.caching-missing-key' = 'true'
);

"""
    
    # 添加结果表
    sql_content += """-- =============================================
-- 3. 创建MySQL结果表
-- =============================================
"""
    sql_content += f"CREATE TABLE {result_table['name']} (\n"
    
    for i, field in enumerate(result_table['fields']):
        comment = f" COMMENT '{field['comment']}'" if field.get('comment') else ""
        comma = "," if i < len(result_table['fields']) - 1 else ""
        sql_content += f"    {field['name']:<20} {field['type']:<15}{comment}{comma}\n"
    
    sql_content += f"""    PRIMARY KEY ({result_table['key_field']}) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'url' = '${{ mysql.url }}',
    'username' = '${{ mysql.username }}',
    'password' = '${{ mysql.password }}',
    'table-name' = '{result_table['name']}',
    -- 批量写入配置
    'sink.buffer-flush.max-rows' = '100',
    'sink.buffer-flush.interval' = '1s',
    'sink.max-retries' = '3',
    'sink.parallelism' = '{parallelism}'
);

-- =============================================
-- 4. 主要业务处理SQL
-- =============================================
INSERT INTO {result_table['name']}
SELECT 
    -- 基础字段
    s.eventId as event_id,
    s.domain,
    s.type,
"""
    
    # 添加SELECT字段
    for i, field in enumerate(result_table['select_fields']):
        comma = "," if i < len(result_table['select_fields']) - 1 else ""
        sql_content += f"    {field}{comma}\n"
    
    sql_content += f"FROM {source_table['name']} s \n"
    
    # 添加维表关联
    if dim_tables:
        sql_content += "-- 维表关联\n"
        for dim_table in dim_tables:
            sql_content += f"LEFT JOIN {dim_table['name']} FOR SYSTEM_TIME AS OF s.proc_time AS {dim_table['alias']}\n"
            sql_content += f"    ON JSON_VALUE(s.payload, '$.{dim_table['join_field']}') = {dim_table['alias']}.{dim_table['key_field']}\n"
    
    sql_content += "WHERE \n"
    sql_content += "    -- 过滤条件：只处理指定的事件类型\n"
    
    # 添加事件类型过滤
    event_type_list = "', '".join(event_types)
    sql_content += f"    s.type IN ('{event_type_list}')\n"
    sql_content += f"    -- 过滤条件：只处理指定的业务域\n"
    sql_content += f"    AND s.domain = '{domain}'\n"
    sql_content += f"    -- 过滤条件：过滤空事件ID\n"
    sql_content += f"    AND s.eventId IS NOT NULL;\n\n"
    
    # 添加配置说明
    sql_content += f"""-- =============================================
-- 配置参数说明
-- =============================================
/*
部署时需要配置的参数：
- kafka.input.topic: Kafka输入Topic名称
- kafka.bootstrap.servers: Kafka集群地址
- kafka.group.id: 消费者组ID
- mysql.url: MySQL数据库连接地址
- mysql.username: MySQL用户名
- mysql.password: MySQL密码

示例配置:
kafka.input.topic={domain}-events
kafka.bootstrap.servers=your-kafka-cluster:9092
kafka.group.id={domain}-event-processor
mysql.url=jdbc:mysql://your-mysql-host:3306/{domain}_db?useSSL=false&serverTimezone=UTC
mysql.username=your_mysql_username
mysql.password=your_mysql_password

建议的作业配置：
- 并行度: {parallelism}
- Checkpoint间隔: 60s
- 状态后端: RocksDB
- 重启策略: fixed-delay (3次，10s间隔)

性能优化建议：
1. 根据数据量调整并行度
2. 维表缓存TTL根据数据更新频率调整
3. MySQL连接池大小根据并发度调整
4. 开启MiniBatch提升吞吐量

监控指标建议：
- 事件处理TPS
- 端到端延迟
- 处理成功率
- Checkpoint成功率
*/"""
    
    return sql_content

def generate_config_guide(config):
    """生成配置指南"""
    domain = config["domain"]
    job_name = config["job_name"]
    
    guide_content = f"""# {job_name} - 阿里云Flink SQL作业配置指南

## 作业信息
- **作业名称**: {job_name}
- **业务域**: {domain}
- **生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

## 1. 参数配置

在阿里云Flink SQL作业中，需要配置以下参数：

### Kafka配置
```properties
kafka.input.topic={domain}-events
kafka.bootstrap.servers=your-kafka-cluster:9092
kafka.group.id={domain}-event-processor
```

### MySQL配置
```properties
mysql.url=jdbc:mysql://your-mysql-host:3306/{domain}_db?useSSL=false&serverTimezone=UTC
mysql.username=your_mysql_username
mysql.password=your_mysql_password
```

## 2. 作业配置建议

### 基本配置
- **作业名称**: `{domain}-event-processor`
- **并行度**: `{config.get('parallelism', 2)}`
- **Checkpoint间隔**: `60s`
- **状态后端**: `RocksDB`

### 阿里云Flink SQL作业高级配置
```yaml
# Checkpoint配置
execution.checkpointing.interval: 60s
execution.checkpointing.timeout: 10min
execution.checkpointing.max-concurrent-checkpoints: 1

# 重启策略
restart-strategy: fixed-delay
restart-strategy.fixed-delay.attempts: 3
restart-strategy.fixed-delay.delay: 10s

# 状态后端
state.backend: rocksdb
state.backend.incremental: true

# MiniBatch优化（阿里云推荐）
table.exec.mini-batch.enabled: true
table.exec.mini-batch.allow-latency: 1s
table.exec.mini-batch.size: 1000

# 网络配置
taskmanager.network.memory.fraction: 0.1
taskmanager.network.memory.min: 64mb
taskmanager.network.memory.max: 1gb
```

## 3. 部署步骤

1. **登录阿里云控制台**
   - 访问实时计算Flink版控制台
   - 选择对应的workspace

2. **创建SQL作业**
   - 点击"新建作业"
   - 选择"SQL作业"
   - 输入作业名称：`{domain}-event-processor`

3. **配置作业参数**
   - 在"参数配置"中设置上述参数
   - 选择合适的Flink版本（推荐1.15及以上）

4. **提交SQL代码**
   - 将生成的SQL代码复制到编辑器
   - 进行语法检查

5. **资源配置**
   - 选择适当的CU数量
   - 配置并行度

6. **启动作业**
   - 先进行"调试运行"
   - 验证无误后"启动"正式作业

## 4. 监控和运维

### 关键监控指标
- **业务指标**: 事件处理TPS、端到端延迟、处理成功率
- **技术指标**: CPU使用率、内存使用率、Checkpoint成功率
- **阿里云特有指标**: 作业健康度、背压状态、资源利用率

### 告警设置
在阿里云云监控中设置以下告警：
- 事件处理延迟 > 30s
- 处理失败率 > 5%
- Checkpoint失败率 > 10%
- 作业状态异常

### 性能优化
1. **并行度调优**: 根据Kafka分区数和数据量调整
2. **内存调优**: 根据状态大小调整TaskManager内存
3. **维表优化**: 调整缓存大小和TTL
4. **批量优化**: 开启MiniBatch提升吞吐量

## 5. 故障排查

### 常见问题
1. **维表查询超时**: 检查MySQL连接和索引
2. **Kafka消费延迟**: 检查并行度和分区均衡
3. **内存不足**: 调整资源配置或优化SQL
4. **Checkpoint失败**: 检查状态后端配置

### 日志查看
- 在阿里云控制台查看作业运行日志
- 关注ERROR和WARN级别的日志
- 使用链路追踪定位问题

参考文档: https://help.aliyun.com/zh/flink/realtime-flink/
"""
    
    return guide_content

def main():
    if len(sys.argv) != 2:
        print("使用方法: python3 simple-sql-generator.py <config.json>")
        print("配置文件示例请参考: examples/user-job-config.json")
        sys.exit(1)
    
    config_file = sys.argv[1]
    if not os.path.exists(config_file):
        print(f"配置文件不存在: {config_file}")
        sys.exit(1)
    
    try:
        # 加载配置文件
        with open(config_file, 'r', encoding='utf-8') as f:
            config = json.load(f)
        
        domain = config["domain"]
        
        # 创建输出目录
        output_dir = f"generated/sql/{domain}"
        os.makedirs(output_dir, exist_ok=True)
        
        # 生成SQL文件
        sql_content = generate_aliyun_flink_sql(config)
        sql_file = os.path.join(output_dir, f"{domain}-flink-job.sql")
        with open(sql_file, 'w', encoding='utf-8') as f:
            f.write(sql_content)
        
        # 生成配置指南
        guide_content = generate_config_guide(config)
        guide_file = os.path.join(output_dir, f"{domain}-配置指南.md")
        with open(guide_file, 'w', encoding='utf-8') as f:
            f.write(guide_content)
        
        print("\n" + "="*60)
        print("🎉 阿里云Flink SQL作业生成成功！")
        print("="*60)
        print(f"📁 SQL文件: {sql_file}")
        print(f"📁 配置指南: {guide_file}")
        print("\n📋 下一步操作:")
        print("1. 登录阿里云实时计算Flink版控制台")
        print("2. 创建新的SQL作业")
        print("3. 复制生成的SQL内容到编辑器")
        print("4. 参考配置指南设置作业参数")
        print("5. 调试运行并启动作业")
        print("\n🔗 参考文档: https://help.aliyun.com/zh/flink/realtime-flink/")
        
    except Exception as e:
        print(f"生成失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
