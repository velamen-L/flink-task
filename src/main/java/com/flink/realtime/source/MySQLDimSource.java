package com.flink.realtime.source;

import com.flink.realtime.util.ConfigUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * MySQL维表数据源
 * 用于定期加载维表数据到内存中
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class MySQLDimSource extends RichSourceFunction<Map<String, Object>> {
    
    private static final Logger logger = LoggerFactory.getLogger(MySQLDimSource.class);
    
    private volatile boolean isRunning = true;
    private Connection connection;
    private final String querySql;
    private final String keyField;
    private final long refreshInterval;
    private final String customUrl;
    private final String customUsername;
    private final String customPassword;
    
    public MySQLDimSource(String querySql, String keyField, long refreshInterval) {
        this.querySql = querySql;
        this.keyField = keyField;
        this.refreshInterval = refreshInterval;
        this.customUrl = null;
        this.customUsername = null;
        this.customPassword = null;
    }
    
    public MySQLDimSource(String querySql, String keyField, String url, String username, String password, long refreshInterval) {
        this.querySql = querySql;
        this.keyField = keyField;
        this.refreshInterval = refreshInterval;
        this.customUrl = url;
        this.customUsername = username;
        this.customPassword = password;
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        
        // 建立MySQL连接
        Class.forName(ConfigUtils.getString("mysql.driver", "com.mysql.cj.jdbc.Driver"));
        
        String url, username, password;
        if (customUrl != null) {
            url = customUrl;
            username = customUsername;
            password = customPassword;
        } else {
            url = ConfigUtils.getString("mysql.url");
            username = ConfigUtils.getString("mysql.username");
            password = ConfigUtils.getString("mysql.password");
        }
        
        connection = DriverManager.getConnection(url, username, password);
        logger.info("MySQL维表连接建立成功: {}", url);
    }
    
    @Override
    public void run(SourceContext<Map<String, Object>> ctx) throws Exception {
        while (isRunning) {
            try {
                loadDimensionData(ctx);
                Thread.sleep(refreshInterval);
            } catch (Exception e) {
                logger.error("加载维表数据失败", e);
                Thread.sleep(5000); // 出错后等待5秒重试
            }
        }
    }
    
    private void loadDimensionData(SourceContext<Map<String, Object>> ctx) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(querySql);
             ResultSet resultSet = statement.executeQuery()) {
            
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                
                // 添加所有字段
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                
                // 添加键字段标识
                row.put("_key", resultSet.getObject(keyField));
                row.put("_table", getTableName());
                row.put("_timestamp", System.currentTimeMillis());
                
                ctx.collect(row);
            }
            
            logger.debug("维表数据加载完成");
        }
    }
    
    private String getTableName() {
        // 从SQL中提取表名（简单实现）
        String[] parts = querySql.toLowerCase().split("from");
        if (parts.length > 1) {
            String tablePart = parts[1].trim().split("\\s+")[0];
            return tablePart.replace("`", "");
        }
        return "unknown";
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
        logger.info("MySQL维表连接已关闭");
        super.close();
    }
}
