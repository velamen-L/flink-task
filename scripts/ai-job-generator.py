#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI编程脚手架 - Flink作业生成器
基于阿里云Catalog和动态路由架构，自动生成Flink作业代码

功能特色：
1. 基于模板快速生成FlinkSQL和DataStream作业
2. 支持动态路由和热部署
3. 集成阿里云Catalog，无需手动配置表结构
4. 支持新事件类型的动态添加

作者：AI代码生成器
创建时间：2024
"""

import json
import os
import sys
import argparse
from datetime import datetime
from pathlib import Path

class FlinkJobGenerator:
    def __init__(self):
        self.project_root = Path(__file__).parent.parent
        self.templates_dir = self.project_root / "templates"
        self.output_dir = self.project_root / "generated"
        
        # 确保输出目录存在
        self.output_dir.mkdir(exist_ok=True)
        
    def generate_job(self, config_file, job_type="both"):
        """
        生成Flink作业
        
        Args:
            config_file: 作业配置文件路径
            job_type: 作业类型 (sql|datastream|both)
        """
        print(f"🚀 AI Flink作业生成器启动")
        print(f"配置文件: {config_file}")
        print(f"作业类型: {job_type}")
        print("-" * 50)
        
        # 加载配置
        config = self._load_config(config_file)
        
        # 验证配置
        self._validate_config(config)
        
        # 生成作业
        generated_files = []
        
        if job_type in ["sql", "both"]:
            sql_file = self._generate_sql_job(config)
            generated_files.append(sql_file)
            
        if job_type in ["datastream", "both"]:
            java_file = self._generate_datastream_job(config)
            generated_files.append(java_file)
            
        # 生成配置文件
        config_file = self._generate_config_file(config)
        generated_files.append(config_file)
        
        # 生成启动脚本
        script_file = self._generate_startup_script(config)
        generated_files.append(script_file)
        
        print("\n✅ 作业生成完成！")
        print("生成的文件:")
        for file in generated_files:
            print(f"  📄 {file}")
            
        print(f"\n📚 使用方法:")
        print(f"  FlinkSQL: 将SQL文件提交到VVP平台")
        print(f"  DataStream: ./generated/start-{config['domain']}-job.sh")
        print(f"  Java类已生成到: src/main/java/com/flink/realtime/business/{config['domain'].title()}WideTableApp.java")
        
        return generated_files
    
    def _load_config(self, config_file):
        """加载配置文件"""
        try:
            with open(config_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            raise Exception(f"加载配置文件失败: {e}")
    
    def _validate_config(self, config):
        """验证配置文件"""
        required_fields = ["domain", "job_name", "event_types", "source_table", "result_table"]
        for field in required_fields:
            if field not in config:
                raise Exception(f"配置文件缺少必需字段: {field}")
    
    def _generate_sql_job(self, config):
        """生成FlinkSQL作业"""
        print("🔧 生成FlinkSQL作业...")
        
        domain = config["domain"]
        job_name = config["job_name"]
        event_types = config["event_types"]
        
        # SQL模板
        sql_template = f"""-- ================================================================
-- {job_name} FlinkSQL 作业 - AI生成
-- 业务域: {domain}
-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
-- ================================================================

-- 设置作业配置
SET 'execution.checkpointing.interval' = '10s';
SET 'execution.checkpointing.mode' = 'EXACTLY_ONCE';
SET 'table.exec.state.ttl' = '1d';

-- ================================================================
-- 使用阿里云Catalog中已配置的表
-- 源表: {config['source_table']['name']}
-- 结果表: {config['result_table']['name']}
-- ================================================================

"""
        
        # 为每个事件类型生成处理视图
        for event_type in event_types:
            event_name = event_type["type"]
            view_name = f"{domain}_{event_name}_events"
            
            sql_template += f"""-- 创建{event_type['description']}处理视图
CREATE TEMPORARY VIEW {view_name} AS
SELECT 
    eventId,
    domain,
    type,
    timestamp,
    TO_TIMESTAMP_LTZ(timestamp, 3) as event_time,
    PROCTIME() as proc_time,
"""
            
            # 添加payload字段提取
            for field in event_type["payload_fields"]:
                field_name = field["name"]
                sql_template += f"    JSON_VALUE(payload, '$.{field_name}') as {field_name.lower()},\n"
            
            sql_template += f"""FROM {config['source_table']['name']} 
WHERE domain = '{domain}' 
  AND type = '{event_name}';

"""
        
        # 为每个事件类型生成INSERT语句
        for event_type in event_types:
            event_name = event_type["type"]
            view_name = f"{domain}_{event_name}_events"
            
            sql_template += f"""-- 处理{event_type['description']}
INSERT INTO {config['result_table']['name']}
SELECT 
    CAST(HASH_CODE(COALESCE(eventId)) AS BIGINT) as id,
    eventId as event_id,
    type as event_type,
    userid as user_id,
    questionid as question_id,
    -- 根据事件类型添加特定逻辑
"""
            
            if "add" in event_name:
                sql_template += """    '' as fix_id,
    CAST(NULL AS TIMESTAMP(3)) as fix_time,
    0 as fix_result,
    '未订正' as fix_result_desc,
"""
            elif "fix" in event_name:
                sql_template += """    eventId as fix_id,
    TO_TIMESTAMP_LTZ(CAST(fixtime AS BIGINT), 3) as fix_time,
    CASE WHEN fixresult = 'correct' THEN 1 ELSE 0 END as fix_result,
    CASE WHEN fixresult = 'correct' THEN '订正正确' ELSE '订正错误' END as fix_result_desc,
"""
            
            sql_template += f"""    proc_time as process_time
FROM {view_name} e;

"""
        
        # 保存SQL文件
        sql_file = self.output_dir / f"{domain}_wide_table.sql"
        with open(sql_file, 'w', encoding='utf-8') as f:
            f.write(sql_template)
        
        return sql_file
    
    def _generate_datastream_job(self, config):
        """生成DataStream作业"""
        print("🔧 生成DataStream作业...")
        
        domain = config["domain"]
        class_name = f"{domain.title()}WideTableApp"
        
        # Java类模板
        java_template = f"""package com.flink.realtime.business;

import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.bean.ProcessedEvent;
import com.flink.realtime.config.RoutingConfig;
import com.flink.realtime.common.AliyunFlinkUtils;
import com.flink.realtime.function.BusinessEventDeserializationSchema;
import com.flink.realtime.function.DynamicRoutingProcessFunction;
import com.flink.realtime.function.OutputRoutingProcessFunction;
import com.flink.realtime.sink.AliyunMySQLSinkFunction;
import com.flink.realtime.source.AliyunKafkaSourceBuilder;
import com.flink.realtime.source.DynamicRoutingConfigSource;
import com.flink.realtime.util.ConfigUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {config['job_name']} - AI生成
 * 业务域: {domain}
 * 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
 * 
 * 支持的事件类型:
"""
        
        for event_type in config["event_types"]:
            java_template += f" * - {event_type['type']}: {event_type['description']}\n"
        
        java_template += f""" * 
 * @author AI代码生成器
 */
public class {class_name} {{
    
    private static final Logger logger = LoggerFactory.getLogger({class_name}.class);
    
    public static void main(String[] args) throws Exception {{
        
        String domain = "{domain}";
        logger.info("启动{{}}宽表作业，业务域: {{}}", "{config['job_name']}", domain);
        
        // 1. 创建执行环境
        StreamExecutionEnvironment env = AliyunFlinkUtils.getAliyunStreamExecutionEnvironment(
                ConfigUtils.getInt("flink.parallelism", 2));
        
        // 2. 创建事件源
        KafkaSource<BusinessEvent> eventSource = AliyunKafkaSourceBuilder.buildAliyunKafkaSource(
                ConfigUtils.getString("kafka.bootstrap.servers"),
                domain + "-events",
                domain + "-wide-table-group",
                ConfigUtils.getString("kafka.username", null),
                ConfigUtils.getString("kafka.password", null)
        );
        
        DataStreamSource<BusinessEvent> eventStream = env.fromSource(
                eventSource,
                WatermarkStrategy.<BusinessEvent>forMonotonousTimestamps()
                        .withTimestampAssigner((event, timestamp) -> event.getTimestamp()),
                domain + " Event Source"
        );
        
        // 3. 创建动态路由配置源
        DataStreamSource<RoutingConfig> configStream = env.addSource(
                new DynamicRoutingConfigSource(domain), "Routing Config Source");
        
        MapStateDescriptor<String, RoutingConfig> configDescriptor = 
                new MapStateDescriptor<>("routing-config", String.class, 
                        TypeInformation.of(RoutingConfig.class));
        BroadcastStream<RoutingConfig> configBroadcast = configStream.broadcast(configDescriptor);
        
        // 4. 动态路由处理
        SingleOutputStreamOperator<ProcessedEvent> processedStream = eventStream
                .connect(configBroadcast)
                .process(new DynamicRoutingProcessFunction(configDescriptor))
                .name("Dynamic Event Processing")
                .uid("dynamic-event-processing-" + domain);
        
        // 5. 输出路由
        OutputTag<ProcessedEvent> alertTag = new OutputTag<ProcessedEvent>("alert", 
                TypeInformation.of(ProcessedEvent.class));
        OutputTag<ProcessedEvent> metricsTag = new OutputTag<ProcessedEvent>("metrics", 
                TypeInformation.of(ProcessedEvent.class));
        
        SingleOutputStreamOperator<ProcessedEvent> routedStream = processedStream
                .process(new OutputRoutingProcessFunction(alertTag, metricsTag, null))
                .name("Output Routing")
                .uid("output-routing-" + domain);
        
        // 6. 输出到宽表
        routedStream.addSink(new AliyunMySQLSinkFunction<>(
                getInsertSQL(),
                getParameterSetter(),
                100
        )).name("{domain}宽表输出");
        
        // 7. 执行作业
        env.execute("{config['job_name']} Dynamic Job");
    }}
    
    private static String getInsertSQL() {{
        return "INSERT INTO {config['result_table']['name']} " +
               "(id, event_id, event_type, user_id, question_id, process_time) " +
               "VALUES (?, ?, ?, ?, ?, ?)";
    }}
    
    private static AliyunMySQLSinkFunction.SqlParameterSetter<ProcessedEvent> getParameterSetter() {{
        return (ps, event) -> {{
            ps.setLong(1, event.getOriginalEvent().getEventId().hashCode());
            ps.setString(2, event.getOriginalEvent().getEventId());
            ps.setString(3, event.getOriginalEvent().getType());
            // 根据具体业务添加字段映射
            ps.setTimestamp(6, new java.sql.Timestamp(event.getProcessTime()));
        }};
    }}
}}
"""
        
        # 保存Java文件到business目录
        business_dir = Path("src/main/java/com/flink/realtime/business")
        business_dir.mkdir(parents=True, exist_ok=True)
        java_file = business_dir / f"{class_name}.java"
        with open(java_file, 'w', encoding='utf-8') as f:
            f.write(java_template)
        
        return java_file
    
    def _generate_config_file(self, config):
        """生成作业配置文件"""
        print("🔧 生成作业配置文件...")
        
        # 生成运行时配置
        runtime_config = {
            "domain": config["domain"],
            "job_name": config["job_name"],
            "kafka": {
                "bootstrap_servers": "localhost:9092",
                "group_id": f"{config['domain']}-wide-table-group"
            },
            "flink": {
                "parallelism": 2,
                "checkpoint_interval": "10s"
            },
            "generated_time": datetime.now().isoformat(),
            "supported_event_types": [et["type"] for et in config["event_types"]]
        }
        
        config_file = self.output_dir / f"{config['domain']}_job_config.json"
        with open(config_file, 'w', encoding='utf-8') as f:
            json.dump(runtime_config, f, indent=2, ensure_ascii=False)
        
        return config_file
    
    def _generate_startup_script(self, config):
        """生成启动脚本"""
        print("🔧 生成启动脚本...")
        
        domain = config["domain"]
        script_content = f"""#!/bin/bash

# {config['job_name']} 启动脚本 - AI生成
# 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

set -e

DOMAIN="{domain}"
PROJECT_ROOT="$(cd "$(dirname "${{BASH_SOURCE[0]}}")/.." && pwd)"
JAR_FILE="$PROJECT_ROOT/target/flink-task-1.0-SNAPSHOT.jar"
MAIN_CLASS="com.flink.realtime.business.{domain.title()}WideTableApp"

echo "🚀 启动${{DOMAIN}}宽表作业..."
echo "JAR文件: $JAR_FILE"
echo "主类: $MAIN_CLASS"

# 检查JAR文件
if [[ ! -f "$JAR_FILE" ]]; then
    echo "❌ JAR文件不存在，开始编译..."
    cd "$PROJECT_ROOT"
    mvn clean package -DskipTests
fi

# 启动作业
if [[ -n "$FLINK_HOME" ]]; then
    echo "✅ 提交到Flink集群..."
    $FLINK_HOME/bin/flink run \\
        --class "$MAIN_CLASS" \\
        --parallelism 2 \\
        "$JAR_FILE" \\
        "$DOMAIN"
else
    echo "❌ 请设置FLINK_HOME环境变量"
    exit 1
fi
"""
        
        script_file = self.output_dir / f"start-{domain}-job.sh"
        with open(script_file, 'w', encoding='utf-8') as f:
            f.write(script_content)
        
        # 添加执行权限
        os.chmod(script_file, 0o755)
        
        return script_file

def main():
    parser = argparse.ArgumentParser(description='AI Flink作业生成器')
    parser.add_argument('config', help='作业配置文件路径')
    parser.add_argument('--type', choices=['sql', 'datastream', 'both'], 
                       default='both', help='生成的作业类型')
    parser.add_argument('--output', help='输出目录（可选）')
    
    args = parser.parse_args()
    
    try:
        generator = FlinkJobGenerator()
        if args.output:
            generator.output_dir = Path(args.output)
            generator.output_dir.mkdir(exist_ok=True)
        
        generator.generate_job(args.config, args.type)
        
    except Exception as e:
        print(f"❌ 生成失败: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
