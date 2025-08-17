#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Flink作业代码生成器
根据输入的表结构生成DataStream API和Flink SQL版本的作业代码

@author: yangfanlin
@date: 2025-01-17
"""

import json
import os
import sys
from typing import Dict, List
from jinja2 import Template

class FlinkJobGenerator:
    
    def __init__(self, base_package="com.flink.realtime"):
        self.base_package = base_package
        self.templates_dir = os.path.join(os.path.dirname(__file__), "templates")
        
    def generate_job(self, config: Dict):
        """
        生成Flink作业代码
        
        Args:
            config: 作业配置，包含源表、维表、结果表结构
        """
        domain = config["domain"]
        job_name = config["job_name"]
        
        print(f"开始生成{domain}域的Flink作业代码...")
        
        # 创建输出目录
        output_dir = f"generated/{domain}"
        os.makedirs(output_dir, exist_ok=True)
        
        # 生成DataStream API版本
        self._generate_datastream_app(config, output_dir)
        
        # 生成Flink SQL版本
        self._generate_sql_app(config, output_dir)
        
        # 生成纯SQL文件（阿里云Flink SQL作业）
        self._generate_pure_sql(config, output_dir)
        
        # 生成事件处理器
        self._generate_event_processor(config, output_dir)
        
        # 生成事件模型
        self._generate_event_model(config, output_dir)
        
        # 生成配置文件
        self._generate_config(config, output_dir)
        
        print(f"代码生成完成，输出目录：{output_dir}")
    
    def _generate_datastream_app(self, config: Dict, output_dir: str):
        """生成DataStream API应用"""
        template_content = '''package {{ package }}.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.function.BusinessEventDeserializationSchema;
import com.flink.realtime.processor.EventProcessorFactory;
import com.flink.realtime.sink.MySQLSinkFunction;
import com.flink.realtime.source.MySQLDimSource;
import com.flink.realtime.util.ConfigUtils;
import {{ package }}.bean.{{ domain_class }}Event;
import {{ package }}.processor.impl.{{ domain_class }}EventProcessor;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Map;

/**
 * {{ job_name }} DataStream API处理应用
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class {{ domain_class }}DataStreamApp {
    
    private static final Logger logger = LoggerFactory.getLogger({{ domain_class }}DataStreamApp.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 维表状态描述符
    private static final MapStateDescriptor<String, Map<String, Object>> DIM_STATE_DESCRIPTOR =
            new MapStateDescriptor<>("dimState", String.class, TypeInformation.of(Map.class));
    
    public static void main(String[] args) throws Exception {
        // 创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(ConfigUtils.getInt("flink.parallelism", 1));
        env.enableCheckpointing(ConfigUtils.getLong("flink.checkpoint.interval", 5000L));
        
        logger.info("{{ job_name }}DataStream处理应用启动...");
        
        // 注册事件处理器
        EventProcessorFactory.registerProcessor("{{ domain }}_*", new {{ domain_class }}EventProcessor());
        
        // 1. 创建Kafka数据源
        KafkaSource<BusinessEvent> kafkaSource = createKafkaSource();
        DataStreamSource<BusinessEvent> eventStream = env.fromSource(
                kafkaSource,
                WatermarkStrategy.<BusinessEvent>forMonotonousTimestamps()
                        .withTimestampAssigner((event, timestamp) -> event.getTimestamp()),
                "{{ domain_class }} Event Source"
        );
        
        {% if dim_tables %}
        // 2. 创建维表数据源（广播流）
        {% for dim_table in dim_tables %}
        DataStreamSource<Map<String, Object>> {{ dim_table.name|lower }}Stream = env.addSource(
                new MySQLDimSource(
                        "SELECT {{ dim_table.fields|join(', ') }} FROM {{ dim_table.name }}",
                        "{{ dim_table.key_field }}",
                        60000L // 1分钟刷新一次
                )
        );
        {% endfor %}
        BroadcastStream<Map<String, Object>> broadcastDimStream = {{ dim_tables[0].name|lower }}Stream.broadcast(DIM_STATE_DESCRIPTOR);
        {% endif %}
        
        // 3. 关联维表数据并处理业务逻辑
        SingleOutputStreamOperator<Map<String, Object>> processedStream = eventStream
                {% if dim_tables %}.connect(broadcastDimStream){% endif %}
                .process(new {{ domain_class }}EventProcessor());
        
        // 4. 写入结果表
        processedStream.addSink(new MySQLSinkFunction<>(
                "INSERT INTO {{ result_table.name }} ({{ result_table.insert_fields|join(', ') }}) VALUES ({{ result_table.placeholders|join(', ') }})",
                (PreparedStatement ps, Map<String, Object> value) -> {
                    {% for i, field in enumerate(result_table.insert_fields) %}
                    ps.set{{ result_table.field_types[i] }}({{ i + 1 }}, ({{ result_table.java_types[i] }}) value.get("{{ field }}"));
                    {% endfor %}
                }
        ));
        
        // 执行任务
        env.execute("{{ job_name }} DataStream Processing Job");
    }
    
    /**
     * 创建Kafka数据源
     */
    private static KafkaSource<BusinessEvent> createKafkaSource() {
        return KafkaSource.<BusinessEvent>builder()
                .setBootstrapServers(ConfigUtils.getString("kafka.bootstrap.servers"))
                .setTopics(ConfigUtils.getString("kafka.input.topic"))
                .setGroupId(ConfigUtils.getString("kafka.group.id"))
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new BusinessEventDeserializationSchema())
                .build();
    }
    
    /**
     * {{ domain_class }}事件处理器
     */
    private static class {{ domain_class }}EventProcessor extends BroadcastProcessFunction<BusinessEvent, Map<String, Object>, Map<String, Object>> {
        
        private MapState<String, Map<String, Object>> dimState;
        
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            dimState = getRuntimeContext().getMapState(DIM_STATE_DESCRIPTOR);
        }
        
        @Override
        public void processElement(BusinessEvent event, ReadOnlyContext ctx, Collector<Map<String, Object>> out) throws Exception {
            try {
                // 处理事件
                {{ domain_class }}EventProcessor processor = new {{ domain_class }}EventProcessor();
                Object processedData = processor.process(event);
                
                // 关联维表数据
                Map<String, Object> result = enrichWithDimData(event, processedData);
                
                out.collect(result);
                logger.debug("事件处理完成: {}", event.getEventId());
            } catch (Exception e) {
                logger.error("处理事件失败: {}", event, e);
            }
        }
        
        @Override
        public void processBroadcastElement(Map<String, Object> dimData, Context ctx, Collector<Map<String, Object>> out) throws Exception {
            // 更新维表状态
            String key = (String) dimData.get("_key");
            String table = (String) dimData.get("_table");
            
            if (key != null && table != null) {
                dimState.put(table + ":" + key, dimData);
                logger.debug("更新维表数据: {}:{}", table, key);
            }
        }
        
        /**
         * 关联维表数据
         */
        private Map<String, Object> enrichWithDimData(BusinessEvent event, Object processedData) throws Exception {
            Map<String, Object> result = new java.util.HashMap<>();
            
            // 基础事件信息
            result.put("event_id", event.getEventId());
            result.put("domain", event.getDomain());
            result.put("type", event.getType());
            result.put("timestamp", event.getTimestamp());
            result.put("data", processedData);
            
            {% if dim_tables %}
            // 关联维表数据
            JsonNode payload = event.getPayload();
            {% for dim_table in dim_tables %}
            if (payload.has("{{ dim_table.key_field }}")) {
                String {{ dim_table.key_field }} = payload.get("{{ dim_table.key_field }}").asText();
                Map<String, Object> {{ dim_table.name|lower }}Dim = dimState.get("{{ dim_table.name }}:" + {{ dim_table.key_field }});
                if ({{ dim_table.name|lower }}Dim != null) {
                    {% for field in dim_table.output_fields %}
                    result.put("{{ field }}", {{ dim_table.name|lower }}Dim.get("{{ field }}"));
                    {% endfor %}
                }
            }
            {% endfor %}
            {% endif %}
            
            return result;
        }
    }
}'''
        
        template = Template(template_content)
        content = template.render(
            package=self.base_package,
            domain=config["domain"],
            domain_class=config["domain"].title(),
            job_name=config["job_name"],
            dim_tables=config.get("dim_tables", []),
            result_table=config["result_table"]
        )
        
        output_path = os.path.join(output_dir, f"{config['domain'].title()}DataStreamApp.java")
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"生成DataStream应用：{output_path}")
    
    def _generate_sql_app(self, config: Dict, output_dir: str):
        """生成Flink SQL应用"""
        template_content = '''package {{ package }}.app;

import com.flink.realtime.util.ConfigUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {{ job_name }} Flink SQL处理应用
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class {{ domain_class }}SqlApp {
    
    private static final Logger logger = LoggerFactory.getLogger({{ domain_class }}SqlApp.class);
    
    public static void main(String[] args) throws Exception {
        // 创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
        
        env.setParallelism(ConfigUtils.getInt("flink.parallelism", 1));
        env.enableCheckpointing(ConfigUtils.getLong("flink.checkpoint.interval", 5000L));
        
        logger.info("{{ job_name }}Flink SQL处理应用启动...");
        
        // 1. 创建Kafka源表
        createKafkaSourceTable(tableEnv);
        
        {% if dim_tables %}
        // 2. 创建MySQL维表
        {% for dim_table in dim_tables %}
        create{{ dim_table.name|title }}DimTable(tableEnv);
        {% endfor %}
        {% endif %}
        
        // 3. 创建MySQL结果表
        createMySQLSinkTable(tableEnv);
        
        // 4. 执行业务SQL
        executeBusinessSQL(tableEnv);
        
        logger.info("Flink SQL任务提交完成");
    }
    
    /**
     * 创建Kafka源表
     */
    private static void createKafkaSourceTable(StreamTableEnvironment tableEnv) {
        String createKafkaSourceSql = String.format(
            "CREATE TABLE {{ source_table.name }} (" +
            {% for field in source_table.fields %}
            "  {{ field.name }} {{ field.type }}{{ ',' if not loop.last else '' }}" +
            {% endfor %}
            "  proc_time AS PROCTIME()," +
            "  event_time AS TO_TIMESTAMP_LTZ(timestamp, 3)," +
            "  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND" +
            ") WITH (" +
            "  'connector' = 'kafka'," +
            "  'topic' = '%s'," +
            "  'properties.bootstrap.servers' = '%s'," +
            "  'properties.group.id' = '%s'," +
            "  'scan.startup.mode' = 'latest-offset'," +
            "  'format' = 'json'" +
            ")",
            ConfigUtils.getString("kafka.input.topic"),
            ConfigUtils.getString("kafka.bootstrap.servers"),
            ConfigUtils.getString("kafka.group.id")
        );
        
        tableEnv.executeSql(createKafkaSourceSql);
        logger.info("Kafka源表创建完成");
    }
    
    {% if dim_tables %}
    {% for dim_table in dim_tables %}
    /**
     * 创建{{ dim_table.name|title }}维表
     */
    private static void create{{ dim_table.name|title }}DimTable(StreamTableEnvironment tableEnv) {
        String createDimSql = String.format(
            "CREATE TABLE {{ dim_table.name }} (" +
            {% for field in dim_table.fields %}
            "  {{ field.name }} {{ field.type }}{{ ',' if not loop.last else '' }}" +
            {% endfor %}
            "  PRIMARY KEY ({{ dim_table.key_field }}) NOT ENFORCED" +
            ") WITH (" +
            "  'connector' = 'jdbc'," +
            "  'url' = '%s'," +
            "  'username' = '%s'," +
            "  'password' = '%s'," +
            "  'table-name' = '{{ dim_table.name }}'," +
            "  'lookup.cache.max-rows' = '1000'," +
            "  'lookup.cache.ttl' = '60s'" +
            ")",
            ConfigUtils.getString("mysql.url"),
            ConfigUtils.getString("mysql.username"),
            ConfigUtils.getString("mysql.password")
        );
        
        tableEnv.executeSql(createDimSql);
        logger.info("{{ dim_table.name|title }}维表创建完成");
    }
    
    {% endfor %}
    {% endif %}
    
    /**
     * 创建MySQL结果表
     */
    private static void createMySQLSinkTable(StreamTableEnvironment tableEnv) {
        String createSinkSql = String.format(
            "CREATE TABLE {{ result_table.name }} (" +
            {% for field in result_table.fields %}
            "  {{ field.name }} {{ field.type }}{{ ',' if not loop.last else '' }}" +
            {% endfor %}
            "  PRIMARY KEY ({{ result_table.key_field }}) NOT ENFORCED" +
            ") WITH (" +
            "  'connector' = 'jdbc'," +
            "  'url' = '%s'," +
            "  'username' = '%s'," +
            "  'password' = '%s'," +
            "  'table-name' = '{{ result_table.name }}'" +
            ")",
            ConfigUtils.getString("mysql.url"),
            ConfigUtils.getString("mysql.username"),
            ConfigUtils.getString("mysql.password")
        );
        
        tableEnv.executeSql(createSinkSql);
        logger.info("MySQL结果表创建完成");
    }
    
    /**
     * 执行业务SQL逻辑
     */
    private static void executeBusinessSQL(StreamTableEnvironment tableEnv) {
        String businessSql = 
            "INSERT INTO {{ result_table.name }} " +
            "SELECT " +
            "  s.eventId as event_id," +
            "  s.domain," +
            "  s.type," +
            {% for field in result_table.select_fields %}
            "  {{ field }}{{ ',' if not loop.last else '' }}" +
            {% endfor %}
            "FROM {{ source_table.name }} s " +
            {% if dim_tables %}
            {% for dim_table in dim_tables %}
            "LEFT JOIN {{ dim_table.name }} FOR SYSTEM_TIME AS OF s.proc_time AS {{ dim_table.alias }} " +
            "  ON JSON_VALUE(s.payload, '$.{{ dim_table.join_field }}') = {{ dim_table.alias }}.{{ dim_table.key_field }} " +
            {% endfor %}
            {% endif %}
            "WHERE s.type IN ({{ event_types|map('quote')|join(', ') }})";
        
        tableEnv.executeSql(businessSql);
        logger.info("业务SQL执行完成");
    }
}'''
        
        template = Template(template_content)
        content = template.render(
            package=self.base_package,
            domain=config["domain"],
            domain_class=config["domain"].title(),
            job_name=config["job_name"],
            source_table=config["source_table"],
            dim_tables=config.get("dim_tables", []),
            result_table=config["result_table"],
            event_types=config.get("event_types", [])
        )
        
        output_path = os.path.join(output_dir, f"{config['domain'].title()}SqlApp.java")
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"生成SQL应用：{output_path}")
    
    def _generate_pure_sql(self, config: Dict, output_dir: str):
        """生成纯SQL文件（阿里云Flink SQL作业）"""
        template_content = '''-- {{ job_name }}
-- 阿里云Flink SQL作业脚本
-- 生成时间: {{ current_time }}
-- 业务域: {{ domain }}

-- =============================================
-- 1. 创建Kafka源表
-- =============================================
CREATE TABLE {{ source_table.name }} (
{% for field in source_table.fields %}
    {{ field.name }} {{ field.type }}{% if field.comment %} COMMENT '{{ field.comment }}'{% endif %}{{ ',' if not loop.last else '' }}
{% endfor %}
    -- 处理时间
    proc_time AS PROCTIME(),
    -- 事件时间
    event_time AS TO_TIMESTAMP_LTZ(`timestamp`, 3),
    -- 水印定义
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
) WITH (
    'connector' = 'kafka',
    'topic' = '${kafka.input.topic}',
    'properties.bootstrap.servers' = '${kafka.bootstrap.servers}',
    'properties.group.id' = '${kafka.group.id}',
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
CREATE TABLE {{ dim_table.name }} (
{% for field in dim_table.fields %}
    {{ field.name }} {{ field.type }}{% if field.comment %} COMMENT '{{ field.comment }}'{% endif %}{{ ',' if not loop.last else '' }}
{% endfor %}
    PRIMARY KEY ({{ dim_table.key_field }}) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'url' = '${mysql.url}',
    'username' = '${mysql.username}',
    'password' = '${mysql.password}',
    'table-name' = '{{ dim_table.name }}',
    'lookup.cache.max-rows' = '1000',
    'lookup.cache.ttl' = '60s',
    'lookup.max-retries' = '3'
);

{% endfor %}
{% endif %}

-- =============================================
-- 3. 创建MySQL结果表
-- =============================================
CREATE TABLE {{ result_table.name }} (
{% for field in result_table.fields %}
    {{ field.name }} {{ field.type }}{% if field.comment %} COMMENT '{{ field.comment }}'{% endif %}{{ ',' if not loop.last else '' }}
{% endfor %}
    PRIMARY KEY ({{ result_table.key_field }}) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'url' = '${mysql.url}',
    'username' = '${mysql.username}',
    'password' = '${mysql.password}',
    'table-name' = '{{ result_table.name }}',
    'sink.buffer-flush.max-rows' = '100',
    'sink.buffer-flush.interval' = '1s',
    'sink.max-retries' = '3'
);

-- =============================================
-- 4. 业务处理SQL
-- =============================================
INSERT INTO {{ result_table.name }}
SELECT 
    s.eventId as event_id,
    s.domain,
    s.type,
{% for field in result_table.select_fields %}
    {{ field }}{{ ',' if not loop.last else '' }}
{% endfor %}
FROM {{ source_table.name }} s 
{% if dim_tables %}
{% for dim_table in dim_tables %}
LEFT JOIN {{ dim_table.name }} FOR SYSTEM_TIME AS OF s.proc_time AS {{ dim_table.alias }}
    ON JSON_VALUE(s.payload, '$.{{ dim_table.join_field }}') = {{ dim_table.alias }}.{{ dim_table.key_field }}
{% endfor %}
{% endif %}
WHERE s.type IN ({% for event_type in event_types %}'{{ event_type }}'{{ ',' if not loop.last else '' }}{% endfor %})
    AND s.domain = '{{ domain }}';

-- =============================================
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
kafka.input.topic={{ domain }}-events
kafka.bootstrap.servers=your-kafka-cluster:9092
kafka.group.id={{ domain }}-event-processor
mysql.url=jdbc:mysql://your-mysql:3306/{{ domain }}_db
mysql.username=your_username
mysql.password=your_password
*/

-- =============================================
-- 作业配置建议
-- =============================================
/*
建议的作业配置：
- 并行度: {{ parallelism if parallelism else 2 }}
- Checkpoint间隔: 60s
- 状态后端: RocksDB
- 重启策略: fixed-delay (3次，10s间隔)

性能优化建议：
1. 根据数据量调整并行度
2. 维表缓存TTL根据数据更新频率调整
3. MySQL连接池大小根据并发度调整
4. 开启MiniBatch提升吞吐量
*/'''
        
        from datetime import datetime
        
        template = Template(template_content)
        content = template.render(
            job_name=config["job_name"],
            current_time=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            domain=config["domain"],
            source_table=config["source_table"],
            dim_tables=config.get("dim_tables", []),
            result_table=config["result_table"],
            event_types=config.get("event_types", []),
            parallelism=config.get("parallelism", 2)
        )
        
        output_path = os.path.join(output_dir, f"{config['domain']}-flink-job.sql")
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"生成纯SQL文件：{output_path}")
    
    def _generate_event_processor(self, config: Dict, output_dir: str):
        """生成事件处理器"""
        template_content = '''package {{ package }}.processor.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.flink.realtime.bean.BusinessEvent;
import com.flink.realtime.processor.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * {{ domain_class }}事件处理器
 * 处理{{ domain }}相关的业务事件
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class {{ domain_class }}EventProcessor implements EventProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger({{ domain_class }}EventProcessor.class);
    
    @Override
    public Object process(BusinessEvent event) throws Exception {
        JsonNode payload = event.getPayload();
        Map<String, Object> result = new HashMap<>();
        
        // 根据事件类型进行不同的处理
        switch (event.getType()) {
            {% for event_type in event_types %}
            case "{{ event_type }}":
                result = process{{ event_type|title|replace('_', '') }}(payload);
                break;
            {% endfor %}
            default:
                logger.warn("未支持的{{ domain }}事件类型: {}", event.getType());
                result.put("raw_data", payload);
        }
        
        // 添加通用字段
        result.put("processed_time", System.currentTimeMillis());
        result.put("processor", this.getClass().getSimpleName());
        
        return result;
    }
    
    @Override
    public String getSupportedEventType() {
        return "{{ domain }}_*";
    }
    
    {% for event_type in event_types %}
    /**
     * 处理{{ event_type }}事件
     */
    private Map<String, Object> process{{ event_type|title|replace('_', '') }}(JsonNode payload) {
        Map<String, Object> result = new HashMap<>();
        
        // TODO: 实现具体的业务逻辑
        {% for field in event_processing_fields.get(event_type, []) %}
        result.put("{{ field.name }}", payload.get("{{ field.source }}").as{{ field.type }}());
        {% endfor %}
        
        logger.debug("处理{{ event_type }}事件完成");
        return result;
    }
    
    {% endfor %}
}'''
        
        template = Template(template_content)
        content = template.render(
            package=self.base_package,
            domain=config["domain"],
            domain_class=config["domain"].title(),
            event_types=config.get("event_types", []),
            event_processing_fields=config.get("event_processing_fields", {})
        )
        
        output_path = os.path.join(output_dir, f"{config['domain'].title()}EventProcessor.java")
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"生成事件处理器：{output_path}")
    
    def _generate_event_model(self, config: Dict, output_dir: str):
        """生成事件模型"""
        template_content = '''package {{ package }}.bean;

import com.flink.realtime.bean.BaseBean;

/**
 * {{ domain_class }}事件数据模型
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class {{ domain_class }}Event extends BaseBean {
    
    {% for field in event_fields %}
    private {{ field.java_type }} {{ field.name }};
    {% endfor %}
    
    public {{ domain_class }}Event() {}
    
    {% for field in event_fields %}
    public {{ field.java_type }} get{{ field.name|title }}() {
        return {{ field.name }};
    }
    
    public void set{{ field.name|title }}({{ field.java_type }} {{ field.name }}) {
        this.{{ field.name }} = {{ field.name }};
    }
    
    {% endfor %}
    
    @Override
    public String toString() {
        return "{{ domain_class }}Event{" +
                {% for field in event_fields %}
                "{{ field.name }}=" + {{ field.name }} +
                {{ ", '" if not loop.last else "'" }}
                {% endfor %}
                '}';
    }
}'''
        
        template = Template(template_content)
        content = template.render(
            package=self.base_package,
            domain_class=config["domain"].title(),
            event_fields=config.get("event_fields", [])
        )
        
        output_path = os.path.join(output_dir, f"{config['domain'].title()}Event.java")
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"生成事件模型：{output_path}")
    
    def _generate_config(self, config: Dict, output_dir: str):
        """生成配置文件"""
        template_content = '''# {{ job_name }}配置文件

# 应用基本信息
app.name={{ job_name }}
app.version=1.0.0
app.domain={{ domain }}

# Kafka配置
kafka.bootstrap.servers=${KAFKA_SERVERS:localhost:9092}
kafka.input.topic=${KAFKA_INPUT_TOPIC:{{ domain }}-events}
kafka.group.id=${KAFKA_GROUP_ID:{{ domain }}-event-processor}

# MySQL配置
mysql.url=${MYSQL_URL:jdbc:mysql://localhost:3306/{{ domain }}_db}
mysql.username=${MYSQL_USERNAME:root}
mysql.password=${MYSQL_PASSWORD:root}

# Flink配置
flink.parallelism=${FLINK_PARALLELISM:1}
flink.checkpoint.interval=${CHECKPOINT_INTERVAL:5000}
flink.checkpoint.timeout=${CHECKPOINT_TIMEOUT:60000}
flink.restart.attempts=${RESTART_ATTEMPTS:3}
flink.restart.delay=${RESTART_DELAY:10000}

# 业务配置
{% for key, value in business_config.items() %}
{{ domain }}.{{ key }}={{ value }}
{% endfor %}'''
        
        template = Template(template_content)
        content = template.render(
            job_name=config["job_name"],
            domain=config["domain"],
            business_config=config.get("business_config", {})
        )
        
        output_path = os.path.join(output_dir, f"{config['domain']}-application.properties")
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"生成配置文件：{output_path}")

def load_config_from_file(config_file: str) -> Dict:
    """从配置文件加载作业配置"""
    with open(config_file, 'r', encoding='utf-8') as f:
        return json.load(f)

def main():
    if len(sys.argv) != 2:
        print("使用方法: python job-generator.py <config.json>")
        print("配置文件示例请参考: examples/user-job-config.json")
        sys.exit(1)
    
    config_file = sys.argv[1]
    if not os.path.exists(config_file):
        print(f"配置文件不存在: {config_file}")
        sys.exit(1)
    
    try:
        config = load_config_from_file(config_file)
        generator = FlinkJobGenerator()
        generator.generate_job(config)
    except Exception as e:
        print(f"生成失败: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
