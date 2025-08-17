package com.flink.realtime.source;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flink.realtime.config.RoutingConfig;
import com.flink.realtime.util.ConfigUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态路由配置源
 * 从数据库读取路由配置并实现热更新
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class DynamicRoutingConfigSource extends RichSourceFunction<RoutingConfig> {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicRoutingConfigSource.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String domain;
    private volatile boolean isRunning = true;
    private Connection connection;
    private final Map<String, Long> lastUpdateTimes = new ConcurrentHashMap<>();
    
    public DynamicRoutingConfigSource(String domain) {
        this.domain = domain;
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        
        // 建立数据库连接
        String url = ConfigUtils.getString("mysql.url");
        String username = ConfigUtils.getString("mysql.username");
        String password = ConfigUtils.getString("mysql.password");
        
        // 加载MySQL驱动
        Class.forName("com.mysql.cj.jdbc.Driver");
        
        connection = DriverManager.getConnection(url, username, password);
        
        logger.info("动态路由配置源已连接到数据库，域: {}", domain);
    }
    
    @Override
    public void run(SourceContext<RoutingConfig> ctx) throws Exception {
        
        // 初始加载所有配置
        loadAllConfigs(ctx);
        
        // 定期检查配置更新
        while (isRunning) {
            try {
                Thread.sleep(30000); // 30秒检查一次
                checkConfigUpdates(ctx);
            } catch (InterruptedException e) {
                logger.info("配置检查线程被中断");
                break;
            } catch (Exception e) {
                logger.error("检查配置更新失败", e);
                Thread.sleep(5000); // 出错后等待5秒重试
            }
        }
    }
    
    /**
     * 初始加载所有配置
     */
    private void loadAllConfigs(SourceContext<RoutingConfig> ctx) throws SQLException {
        String sql = "SELECT * FROM flink_routing_config WHERE domain = ? ORDER BY priority DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, domain);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RoutingConfig config = buildRoutingConfig(rs);
                    ctx.collect(config);
                    
                    // 记录最后更新时间
                    lastUpdateTimes.put(config.getRoutingKey(), config.getUpdateTime());
                    
                    logger.info("加载路由配置: {}", config);
                }
            }
        }
        
        logger.info("初始配置加载完成，域: {}, 配置数量: {}", domain, lastUpdateTimes.size());
    }
    
    /**
     * 检查配置更新
     */
    private void checkConfigUpdates(SourceContext<RoutingConfig> ctx) throws SQLException {
        String sql = "SELECT * FROM flink_routing_config WHERE domain = ? AND update_time > ?";
        
        // 获取最后更新时间
        long lastCheckTime = lastUpdateTimes.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, domain);
            stmt.setTimestamp(2, new Timestamp(lastCheckTime));
            
            try (ResultSet rs = stmt.executeQuery()) {
                int updateCount = 0;
                while (rs.next()) {
                    RoutingConfig config = buildRoutingConfig(rs);
                    ctx.collect(config);
                    
                    // 更新最后更新时间
                    lastUpdateTimes.put(config.getRoutingKey(), config.getUpdateTime());
                    updateCount++;
                    
                    logger.info("配置更新: {}", config);
                }
                
                if (updateCount > 0) {
                    logger.info("检测到配置更新，域: {}, 更新数量: {}", domain, updateCount);
                }
            }
        }
    }
    
    /**
     * 构建路由配置对象
     */
    private RoutingConfig buildRoutingConfig(ResultSet rs) throws SQLException {
        RoutingConfig config = new RoutingConfig();
        config.setDomain(rs.getString("domain"));
        config.setEventType(rs.getString("event_type"));
        config.setProcessorClass(rs.getString("processor_class"));
        config.setEnabled(rs.getBoolean("is_enabled"));
        config.setPriority(rs.getInt("priority"));
        config.setUpdateTime(rs.getTimestamp("update_time").getTime());
        
        // 解析输出配置JSON
        String outputConfigJson = rs.getString("output_config");
        if (outputConfigJson != null && !outputConfigJson.trim().isEmpty()) {
            try {
                Map<String, Object> outputConfig = objectMapper.readValue(
                        outputConfigJson, new TypeReference<Map<String, Object>>() {});
                config.setOutputConfig(outputConfig);
            } catch (Exception e) {
                logger.warn("解析输出配置失败: {}", outputConfigJson, e);
            }
        }
        
        return config;
    }
    
    @Override
    public void cancel() {
        isRunning = false;
    }
    
    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
        logger.info("动态路由配置源已关闭，域: {}", domain);
        super.close();
    }
}
