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

/**
 * MySQL结果表写入器
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class MySQLSinkFunction<T> extends RichSinkFunction<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(MySQLSinkFunction.class);
    
    private Connection connection;
    private PreparedStatement preparedStatement;
    private final String insertSql;
    private final SqlParameterSetter<T> parameterSetter;
    
    public MySQLSinkFunction(String insertSql, SqlParameterSetter<T> parameterSetter) {
        this.insertSql = insertSql;
        this.parameterSetter = parameterSetter;
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        
        // 加载MySQL驱动
        Class.forName(ConfigUtils.getString("mysql.driver", "com.mysql.cj.jdbc.Driver"));
        
        // 建立连接
        String url = ConfigUtils.getString("mysql.url");
        String username = ConfigUtils.getString("mysql.username");
        String password = ConfigUtils.getString("mysql.password");
        
        connection = DriverManager.getConnection(url, username, password);
        connection.setAutoCommit(false);
        
        // 预编译SQL
        preparedStatement = connection.prepareStatement(insertSql);
        
        logger.info("MySQL连接建立成功，预编译SQL: {}", insertSql);
    }
    
    @Override
    public void invoke(T value, Context context) throws Exception {
        try {
            // 设置参数
            parameterSetter.setParameters(preparedStatement, value);
            
            // 执行插入
            preparedStatement.executeUpdate();
            connection.commit();
            
            logger.debug("数据写入MySQL成功: {}", value);
        } catch (SQLException e) {
            logger.error("数据写入MySQL失败: {}", value, e);
            connection.rollback();
            throw e;
        }
    }
    
    @Override
    public void close() throws Exception {
        if (preparedStatement != null) {
            preparedStatement.close();
        }
        if (connection != null) {
            connection.close();
        }
        logger.info("MySQL连接已关闭");
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
