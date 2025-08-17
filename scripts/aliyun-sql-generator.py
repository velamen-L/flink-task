#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
阿里云Flink SQL作业生成器（修复版）
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
    
    # 添加计算列和水印
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
-- 阿里云Flink SQL作业配置说明
-- =============================================
/*
部署时需要配置的参数：

1. Kafka配置：
   kafka.input.topic={domain}-events
   kafka.bootstrap.servers=your-kafka-instance.kafka.cn-hangzhou.aliyuncs.com:9092
   kafka.group.id={domain}-event-processor
   
   # 如果使用SASL认证（阿里云Kafka实例推荐）：
   kafka.security.protocol=SASL_PLAINTEXT
   kafka.sasl.mechanism=PLAIN
   kafka.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="your_username" password="your_password";

2. MySQL配置：
   mysql.url=jdbc:mysql://your-rds-instance.mysql.rds.aliyuncs.com:3306/{domain}_db?useSSL=true&serverTimezone=UTC
   mysql.username=your_mysql_username
   mysql.password=your_mysql_password

3. 作业配置建议：
   - 并行度: {parallelism}
   - Checkpoint间隔: 60s
   - 状态后端: RocksDB
   - 重启策略: fixed-delay (3次，10s间隔)

4. 阿里云特有配置：
   # MiniBatch优化
   table.exec.mini-batch.enabled: true
   table.exec.mini-batch.allow-latency: 1s
   table.exec.mini-batch.size: 1000
   
   # 状态TTL
   table.exec.state.ttl: 3600000
   
   # Checkpoint配置
   execution.checkpointing.interval: 60s
   execution.checkpointing.timeout: 10min
   execution.checkpointing.max-concurrent-checkpoints: 1

5. 监控指标关注：
   - 事件处理TPS
   - 端到端延迟
   - 处理成功率
   - Checkpoint成功率
   - 维表命中率

6. 性能优化建议：
   - 根据Kafka分区数调整并行度
   - 调整维表缓存大小和TTL
   - 监控背压状态
   - 合理设置资源配置（CU数量）
*/"""
    
    return sql_content

def main():
    if len(sys.argv) != 2:
        print("使用方法: python3 aliyun-sql-generator.py <config.json>")
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
        output_dir = f"generated/aliyun-sql/{domain}"
        os.makedirs(output_dir, exist_ok=True)
        
        # 生成SQL文件
        sql_content = generate_aliyun_flink_sql(config)
        sql_file = os.path.join(output_dir, f"{domain}-aliyun-flink-job.sql")
        with open(sql_file, 'w', encoding='utf-8') as f:
            f.write(sql_content)
        
        print("\n" + "="*60)
        print("🎉 阿里云Flink SQL作业生成成功！")
        print("="*60)
        print(f"📁 SQL文件: {sql_file}")
        print("\n📋 下一步操作:")
        print("1. 登录阿里云实时计算Flink版控制台")
        print("   https://realtime-compute.console.aliyun.com/")
        print("2. 创建新的SQL作业")
        print("3. 复制生成的SQL内容到编辑器")
        print("4. 在'参数配置'中设置必要的参数")
        print("5. 选择合适的资源配置（CU数量）")
        print("6. 调试运行并启动作业")
        print("\n🔗 参考文档:")
        print("- 阿里云Flink官方文档: https://help.aliyun.com/zh/flink/realtime-flink/")
        print("- SQL开发指南: https://help.aliyun.com/zh/flink/developer-reference/")
        
    except Exception as e:
        print(f"生成失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
