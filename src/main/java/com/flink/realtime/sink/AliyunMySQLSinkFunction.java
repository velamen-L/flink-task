package com.flink.realtime.sink;

import com.flink.realtime.util.ConfigUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 阿里云RDS MySQL结果表写入器
 * 符合阿里云RDS最佳实践的配置
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class AliyunMySQLSinkFunction<T> extends RichSinkFunction<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(AliyunMySQLSinkFunction.class);
    
    private Connection connection;
    private PreparedStatement preparedStatement;
    private final String insertSql;
    private final SqlParameterSetter<T> parameterSetter;
    private int batchCount = 0;
    private final int batchSize;
    
    public AliyunMySQLSinkFunction(String insertSql, SqlParameterSetter<T> parameterSetter) {
        this(insertSql, parameterSetter, 100); // 默认批大小100
    }
    
    public AliyunMySQLSinkFunction(String insertSql, SqlParameterSetter<T> parameterSetter, int batchSize) {
        this.insertSql = insertSql;
        this.parameterSetter = parameterSetter;
        this.batchSize = batchSize;
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        
        // 建立阿里云RDS MySQL连接
        String url = ConfigUtils.getString("mysql.url");
        String username = ConfigUtils.getString("mysql.username");
        String password = ConfigUtils.getString("mysql.password");
        
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        
        // 阿里云RDS推荐的连接配置
        props.setProperty("useSSL", "true");
        props.setProperty("useUnicode", "true");
        props.setProperty("characterEncoding", "utf8mb4");
        props.setProperty("serverTimezone", "UTC");
        props.setProperty("rewriteBatchedStatements", "true"); // 批量写入优化
        props.setProperty("cachePrepStmts", "true"); // 预编译语句缓存
        props.setProperty("prepStmtCacheSize", "250");
        props.setProperty("prepStmtCacheSqlLimit", "2048");
        
        // 连接池相关配置
        props.setProperty("useConnectionPooling", "true");
        props.setProperty("maxReconnects", "3");
        props.setProperty("autoReconnect", "true");
        props.setProperty("failOverReadOnly", "false");
        
        // 超时配置
        props.setProperty("connectTimeout", "10000");
        props.setProperty("socketTimeout", "30000");
        
        // 阿里云RDS特有的性能优化
        props.setProperty("useLocalSessionState", "true");
        props.setProperty("useLocalTransactionState", "true");
        props.setProperty("maintainTimeStats", "false");
        
        connection = DriverManager.getConnection(url, props);
        connection.setAutoCommit(false); // 使用手动提交以支持批量操作
        
        // 预编译SQL
        preparedStatement = connection.prepareStatement(insertSql);
        
        logger.info("阿里云RDS MySQL连接建立成功，预编译SQL: {}", insertSql);
    }
    
    @Override
    public void invoke(T value, Context context) throws Exception {
        try {
            // 设置参数
            parameterSetter.setParameters(preparedStatement, value);
            
            // 添加到批次
            preparedStatement.addBatch();
            batchCount++;
            
            // 达到批次大小时执行批量插入
            if (batchCount >= batchSize) {
                executeBatch();
            }
            
        } catch (SQLException e) {
            logger.error("数据写入MySQL失败: {}", value, e);
            connection.rollback();
            throw e;
        }
    }
    
    /**
     * 执行批量插入
     */
    private void executeBatch() throws SQLException {
        if (batchCount > 0) {
            preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.clearBatch();
            batchCount = 0;
            
            logger.debug("批量写入MySQL成功，批次大小: {}", batchSize);
        }
    }
    
    @Override
    public void close() throws Exception {
        try {
            // 关闭前执行剩余的批次
            executeBatch();
        } catch (SQLException e) {
            logger.warn("关闭前执行剩余批次失败", e);
        }
        
        if (preparedStatement != null) {
            preparedStatement.close();
        }
        if (connection != null) {
            connection.close();
        }
        logger.info("阿里云RDS MySQL连接已关闭");
        super.close();
    }
    
    /**
     * SQL参数设置器接口
     * @param <T>
     */
    @FunctionalInterface
    public interface SqlParameterSetter<T> {
        void setParameters(PreparedStatement ps, T value) throws SQLException;
    }
}
