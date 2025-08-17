#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
阿里云Flink SQL作业生成器
专门用于生成可直接在阿里云Flink SQL平台运行的SQL文件

@author: yangfanlin
@date: 2025-01-17
"""

import json
import os
import sys
from typing import Dict
from jinja2 import Template
from datetime import datetime

class AliyunFlinkSQLGenerator:
    
    def __init__(self):
        pass
        
    def generate_sql_job(self, config: Dict):
        """
        生成阿里云Flink SQL作业
        
        Args:
            config: 作业配置，包含源表、维表、结果表结构
        """
        domain = config["domain"]
        job_name = config["job_name"]
        
        print(f"开始生成{domain}域的阿里云Flink SQL作业...")
        
        # 创建输出目录
        output_dir = f"generated/sql/{domain}"
        os.makedirs(output_dir, exist_ok=True)
        
        # 生成SQL文件
        sql_content = self._generate_sql_content(config)
        
        # 输出SQL文件
        output_path = os.path.join(output_dir, f"{domain}-flink-job.sql")
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(sql_content)
        
        # 生成配置说明文件
        config_content = self._generate_config_guide(config)
        config_path = os.path.join(output_dir, f"{domain}-配置说明.md")
        with open(config_path, 'w', encoding='utf-8') as f:
            f.write(config_content)
        
        print(f"SQL作业生成完成：{output_path}")
        print(f"配置说明生成完成：{config_path}")
        
        return output_path
    
    def _generate_sql_content(self, config: Dict) -> str:
        """生成SQL内容"""
        template_content = '''-- {{ job_name }}
-- 阿里云Flink SQL作业脚本
-- 生成时间: {{ current_time }}
-- 业务域: {{ domain }}
-- 
-- 使用说明：
-- 1. 将此SQL内容复制到阿里云Flink SQL开发平台
-- 2. 根据实际环境修改配置参数（用${}包围的变量）
-- 3. 创建作业并提交运行

-- =============================================
-- 1. 创建Kafka源表
-- =============================================
CREATE TABLE {{ source_table.name }} (
{% for field in source_table.fields %}
    {{ "%-20s"|format(field.name) }} {{ "%-15s"|format(field.type) }}{% if field.comment %} COMMENT '{{ field.comment }}'{% endif %}{{ ',' if not loop.last else '' }}
{% endfor %}
    -- 计算列：处理时间
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

{% if dim_tables %}
-- =============================================
-- 2. 创建MySQL维表
-- =============================================
{% for dim_table in dim_tables %}
-- {{ dim_table.name }} 维表
CREATE TABLE {{ dim_table.name }} (
{% for field in dim_table.fields %}
    {{ "%-20s"|format(field.name) }} {{ "%-15s"|format(field.type) }}{% if field.comment %} COMMENT '{{ field.comment }}'{% endif %}{{ ',' if not loop.last else '' }}
{% endfor %}
    PRIMARY KEY ({{ dim_table.key_field }}) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'url' = '${ mysql.url }',
    'username' = '${ mysql.username }',
    'password' = '${ mysql.password }',
    'table-name' = '{{ dim_table.name }}',
    -- 维表查询缓存配置
    'lookup.cache.max-rows' = '1000',
    'lookup.cache.ttl' = '60s',
    'lookup.max-retries' = '3',
    'lookup.cache.caching-missing-key' = 'true'
);

{% endfor %}
{% endif %}

-- =============================================
-- 3. 创建MySQL结果表
-- =============================================
CREATE TABLE {{ result_table.name }} (
{% for field in result_table.fields %}
    {{ "%-20s"|format(field.name) }} {{ "%-15s"|format(field.type) }}{% if field.comment %} COMMENT '{{ field.comment }}'{% endif %}{{ ',' if not loop.last else '' }}
{% endfor %}
    PRIMARY KEY ({{ result_table.key_field }}) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'url' = '${ mysql.url }',
    'username' = '${ mysql.username }',
    'password' = '${ mysql.password }',
    'table-name' = '{{ result_table.name }}',
    -- 批量写入配置
    'sink.buffer-flush.max-rows' = '100',
    'sink.buffer-flush.interval' = '1s',
    'sink.max-retries' = '3',
    'sink.parallelism' = '{{ parallelism }}'
);

-- =============================================
-- 4. 主要业务处理SQL
-- =============================================
INSERT INTO {{ result_table.name }}
SELECT 
    -- 基础字段
    s.eventId as event_id,
    s.domain,
    s.type,
{% for field in result_table.select_fields %}
    {{ field }}{{ ',' if not loop.last else '' }}
{% endfor %}
FROM {{ source_table.name }} s 
{% if dim_tables %}
-- 维表关联
{% for dim_table in dim_tables %}
LEFT JOIN {{ dim_table.name }} FOR SYSTEM_TIME AS OF s.proc_time AS {{ dim_table.alias }}
    ON JSON_VALUE(s.payload, '$.{{ dim_table.join_field }}') = {{ dim_table.alias }}.{{ dim_table.key_field }}
{% endfor %}
{% endif %}
WHERE 
    -- 过滤条件：只处理指定的事件类型
    s.type IN ({% for event_type in event_types %}'{{ event_type }}'{{ ',' if not loop.last else '' }}{% endfor %})
    -- 过滤条件：只处理指定的业务域
    AND s.domain = '{{ domain }}'
    -- 过滤条件：过滤空事件ID
    AND s.eventId IS NOT NULL;

{% if additional_sqls %}
-- =============================================
-- 5. 附加SQL语句
-- =============================================
{% for sql in additional_sqls %}
-- {{ sql.description }}
{{ sql.content }};

{% endfor %}
{% endif %}'''
        
        template = Template(template_content)
        content = template.render(
            job_name=config["job_name"],
            current_time=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            domain=config["domain"],
            source_table=config["source_table"],
            dim_tables=config.get("dim_tables", []),
            result_table=config["result_table"],
            event_types=config.get("event_types", []),
            parallelism=config.get("parallelism", 2),
            additional_sqls=config.get("additional_sqls", [])
        )
        
        return content
    
    def _generate_config_guide(self, config: Dict) -> str:
        """生成配置说明文档"""
        template_content = '''# {{ job_name }} - 阿里云Flink SQL作业配置指南

## 作业信息
- **作业名称**: {{ job_name }}
- **业务域**: {{ domain }}
- **生成时间**: {{ current_time }}

## 1. 参数配置

在阿里云Flink SQL作业中，需要配置以下参数：

### Kafka配置
```properties
kafka.input.topic={{ domain }}-events
kafka.bootstrap.servers=your-kafka-cluster:9092
kafka.group.id={{ domain }}-event-processor
```

### MySQL配置
```properties
mysql.url=jdbc:mysql://your-mysql-host:3306/{{ domain }}_db?useSSL=false&serverTimezone=UTC
mysql.username=your_mysql_username
mysql.password=your_mysql_password
```

## 2. 作业配置建议

### 基本配置
- **作业名称**: `{{ domain }}-event-processor`
- **并行度**: `{{ parallelism }}`
- **Checkpoint间隔**: `60s`
- **状态后端**: `RocksDB`

### 高级配置
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

# 网络配置
taskmanager.network.memory.fraction: 0.1
taskmanager.network.memory.min: 64mb
taskmanager.network.memory.max: 1gb
```

## 3. 表结构说明

### 源表结构 ({{ source_table.name }})
{% for field in source_table.fields %}
- **{{ field.name }}** ({{ field.type }}): {{ field.comment if field.comment else '无描述' }}
{% endfor %}

{% if dim_tables %}
### 维表结构
{% for dim_table in dim_tables %}

#### {{ dim_table.name }}
- **主键**: {{ dim_table.key_field }}
- **关联字段**: {{ dim_table.join_field }}

{% for field in dim_table.fields %}
- **{{ field.name }}** ({{ field.type }}): {{ field.comment if field.comment else '无描述' }}
{% endfor %}
{% endfor %}
{% endif %}

### 结果表结构 ({{ result_table.name }})
{% for field in result_table.fields %}
- **{{ field.name }}** ({{ field.type }}): {{ field.comment if field.comment else '无描述' }}
{% endfor %}

## 4. 事件类型

支持的事件类型：
{% for event_type in event_types %}
- `{{ event_type }}`
{% endfor %}

## 5. 性能优化建议

### 并行度调优
- 根据Kafka分区数设置合适的并行度
- 建议并行度 = Kafka分区数 或其约数
- 对于{{ domain }}域，建议并行度设置为: `{{ parallelism }}`

### 内存调优
```yaml
# TaskManager内存配置
taskmanager.memory.process.size: 4g
taskmanager.memory.managed.fraction: 0.4

# JobManager内存配置  
jobmanager.memory.process.size: 2g
```

### 维表缓存优化
- 根据维表数据量调整 `lookup.cache.max-rows`
- 根据维表更新频率调整 `lookup.cache.ttl`
- 当前配置：最大缓存1000行，TTL为60秒

### MySQL连接优化
- 调整MySQL连接池大小: `lookup.jdbc.read.connection.pool-size`
- 设置合适的连接超时: `lookup.jdbc.read.connection.timeout`
- 启用连接重用: `lookup.jdbc.read.connection.reuse`

## 6. 监控指标

关注以下关键指标：

### 业务指标
- 事件处理TPS
- 端到端延迟
- 处理成功率

### 技术指标
- CPU使用率
- 内存使用率
- Checkpoint成功率
- 背压指标

### 告警设置
- 事件处理延迟 > 30s
- 处理失败率 > 5%
- Checkpoint失败率 > 10%

## 7. 故障排查

### 常见问题

1. **维表查询超时**
   - 检查MySQL连接配置
   - 检查维表索引是否正确
   - 调整查询超时时间

2. **Kafka消费延迟**
   - 检查并行度设置
   - 检查Kafka分区均衡
   - 调整消费者配置

3. **内存不足**
   - 调整TaskManager内存配置
   - 检查状态大小
   - 优化SQL查询

### 调试技巧
- 使用EXPLAIN PLAN查看执行计划
- 启用详细日志记录
- 使用Flink Web UI监控作业状态

## 8. 部署步骤

1. **创建作业**
   - 在阿里云Flink SQL控制台创建新作业
   - 选择适当的Flink版本和资源配置

2. **配置参数**
   - 设置上述参数配置
   - 验证连接配置

3. **提交SQL**
   - 复制生成的SQL到编辑器
   - 进行语法检查

4. **测试运行**
   - 先进行调试运行
   - 检查日志和监控指标

5. **正式发布**
   - 停止调试作业
   - 启动正式作业
   - 设置监控告警

## 9. 版本历史

- **v1.0**: 初始版本
- 支持的事件类型: {{ event_types|join(', ') }}
- 最后更新: {{ current_time }}'''
        
        template = Template(template_content)
        content = template.render(
            job_name=config["job_name"],
            domain=config["domain"],
            current_time=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            source_table=config["source_table"],
            dim_tables=config.get("dim_tables", []),
            result_table=config["result_table"],
            event_types=config.get("event_types", []),
            parallelism=config.get("parallelism", 2)
        )
        
        return content

def load_config_from_file(config_file: str) -> Dict:
    """从配置文件加载作业配置"""
    with open(config_file, 'r', encoding='utf-8') as f:
        return json.load(f)

def main():
    if len(sys.argv) != 2:
        print("使用方法: python sql-generator.py <config.json>")
        print("配置文件示例请参考: examples/user-job-config.json")
        sys.exit(1)
    
    config_file = sys.argv[1]
    if not os.path.exists(config_file):
        print(f"配置文件不存在: {config_file}")
        sys.exit(1)
    
    try:
        config = load_config_from_file(config_file)
        generator = AliyunFlinkSQLGenerator()
        sql_file = generator.generate_sql_job(config)
        
        print("\n" + "="*50)
        print("🎉 阿里云Flink SQL作业生成成功！")
        print("="*50)
        print(f"📁 SQL文件: {sql_file}")
        print("\n📋 下一步操作:")
        print("1. 打开阿里云Flink SQL开发平台")
        print("2. 创建新的SQL作业")
        print("3. 复制生成的SQL内容")
        print("4. 配置作业参数")
        print("5. 测试并发布作业")
        
    except Exception as e:
        print(f"生成失败: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
