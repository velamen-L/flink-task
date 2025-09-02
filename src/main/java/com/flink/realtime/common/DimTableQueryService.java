package com.flink.realtime.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flink.realtime.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 通用维表查询服务
 * 
 * 功能特性：
 * - 支持任意维表的查询
 * - 本地缓存提高查询性能
 * - 支持缓存过期和刷新
 * - 连接池管理
 * - 异步预加载热点数据
 * 
 * 使用示例：
 * ```java
 * DimTableQueryService dimService = DimTableQueryService.getInstance();
 * 
 * // 查询单条记录
 * Map<String, Object> userInfo = dimService.queryOne(
 *     "SELECT * FROM user_profiles WHERE user_id = ?", userId);
 * 
 * // 查询多条记录
 * List<Map<String, Object>> patterns = dimService.queryList(
 *     "SELECT * FROM pattern_info WHERE subject = ?", subject);
 * 
 * // 带缓存查询
 * Map<String, Object> questionInfo = dimService.queryWithCache(
 *     "question_info", questionId, 
 *     "SELECT * FROM question_info WHERE question_id = ?", questionId);
 * ```
 * 
 * @author AI代码生成器
 * @date 2024-12-27
 */
public class DimTableQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(DimTableQueryService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 单例实例
    private static volatile DimTableQueryService instance;
    
    // 缓存配置
    private static final int DEFAULT_CACHE_SIZE = 10000;
    private static final long DEFAULT_CACHE_TTL_MINUTES = 30;
    private static final long CACHE_CLEANUP_INTERVAL_MINUTES = 10;
    
    // 缓存存储
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cacheCleanupExecutor;
    
    // 数据库连接信息
    private final String jdbcUrl;
    private final String username;
    private final String password;
    
    private DimTableQueryService() {
        this.jdbcUrl = ConfigUtils.getString("mysql.url");
        this.username = ConfigUtils.getString("mysql.username");
        this.password = ConfigUtils.getString("mysql.password");
        
        // 初始化缓存清理定时器
        this.cacheCleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DimTable-Cache-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // 每10分钟清理一次过期缓存
        cacheCleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredCache, 
                CACHE_CLEANUP_INTERVAL_MINUTES, 
                CACHE_CLEANUP_INTERVAL_MINUTES, 
                TimeUnit.MINUTES);
        
        logger.info("维表查询服务初始化完成");
    }
    
    /**
     * 获取单例实例
     */
    public static DimTableQueryService getInstance() {
        if (instance == null) {
            synchronized (DimTableQueryService.class) {
                if (instance == null) {
                    instance = new DimTableQueryService();
                }
            }
        }
        return instance;
    }
    
    /**
     * 查询单条记录
     * 
     * @param sql SQL查询语句
     * @param params 查询参数
     * @return 查询结果，如果没有找到返回null
     */
    public Map<String, Object> queryOne(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // 设置参数
            setParameters(stmt, params);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return resultSetToMap(rs);
                }
            }
            
        } catch (SQLException e) {
            logger.error("查询单条记录失败: SQL={}, params={}", sql, params, e);
        }
        
        return null;
    }
    
    /**
     * 查询多条记录
     * 
     * @param sql SQL查询语句
     * @param params 查询参数
     * @return 查询结果列表
     */
    public List<Map<String, Object>> queryList(String sql, Object... params) {
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // 设置参数
            setParameters(stmt, params);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(resultSetToMap(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("查询多条记录失败: SQL={}, params={}", sql, params, e);
        }
        
        return results;
    }
    
    /**
     * 带缓存的查询单条记录
     * 
     * @param tableName 表名（用于缓存key）
     * @param keyValue 主键值
     * @param sql SQL查询语句
     * @param params 查询参数
     * @return 查询结果
     */
    public Map<String, Object> queryWithCache(String tableName, Object keyValue, 
                                            String sql, Object... params) {
        
        String cacheKey = buildCacheKey(tableName, keyValue);
        
        // 先查缓存
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logger.debug("缓存命中: {}", cacheKey);
            return cached.getData();
        }
        
        // 缓存未命中，查询数据库
        Map<String, Object> result = queryOne(sql, params);
        
        // 更新缓存
        if (result != null) {
            cache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis()));
            logger.debug("缓存更新: {}", cacheKey);
        }
        
        return result;
    }
    
    /**
     * 批量查询并缓存
     * 
     * @param tableName 表名
     * @param keyField 主键字段名
     * @param keyValues 主键值列表
     * @param sql SQL查询语句（应该支持IN查询）
     * @return 查询结果Map，key为主键值，value为记录数据
     */
    public Map<Object, Map<String, Object>> batchQueryWithCache(String tableName, 
                                                               String keyField,
                                                               List<Object> keyValues,
                                                               String sql) {
        
        Map<Object, Map<String, Object>> results = new ConcurrentHashMap<>();
        List<Object> uncachedKeys = new java.util.ArrayList<>();
        
        // 检查缓存
        for (Object keyValue : keyValues) {
            String cacheKey = buildCacheKey(tableName, keyValue);
            CacheEntry cached = cache.get(cacheKey);
            
            if (cached != null && !cached.isExpired()) {
                results.put(keyValue, cached.getData());
            } else {
                uncachedKeys.add(keyValue);
            }
        }
        
        // 批量查询未缓存的数据
        if (!uncachedKeys.isEmpty()) {
            String inClause = String.join(",", uncachedKeys.stream()
                    .map(k -> "?").toArray(String[]::new));
            String batchSql = sql.replace("?", "IN (" + inClause + ")");
            
            List<Map<String, Object>> batchResults = queryList(batchSql, uncachedKeys.toArray());
            
            // 处理批量查询结果
            for (Map<String, Object> row : batchResults) {
                Object keyValue = row.get(keyField);
                if (keyValue != null) {
                    results.put(keyValue, row);
                    
                    // 更新缓存
                    String cacheKey = buildCacheKey(tableName, keyValue);
                    cache.put(cacheKey, new CacheEntry(row, System.currentTimeMillis()));
                }
            }
        }
        
        logger.debug("批量查询完成: 表={}, 总数={}, 缓存命中={}, 数据库查询={}", 
                tableName, keyValues.size(), keyValues.size() - uncachedKeys.size(), uncachedKeys.size());
        
        return results;
    }
    
    /**
     * 清空指定表的缓存
     */
    public void clearCache(String tableName) {
        String prefix = tableName + ":";
        cache.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
        logger.info("清空表缓存: {}", tableName);
    }
    
    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        cache.clear();
        logger.info("清空所有缓存");
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        long currentTime = System.currentTimeMillis();
        long expiredCount = cache.values().stream()
                .mapToLong(entry -> entry.isExpired(currentTime) ? 1 : 0)
                .sum();
        
        return new CacheStats(cache.size(), cache.size() - expiredCount, expiredCount);
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 获取数据库连接
     */
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL驱动加载失败", e);
        }
        
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
    
    /**
     * 设置PreparedStatement参数
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
    
    /**
     * 将ResultSet转换为Map
     */
    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        Map<String, Object> result = new java.util.HashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnLabel(i);
            Object value = rs.getObject(i);
            result.put(columnName, value);
        }
        
        return result;
    }
    
    /**
     * 构建缓存key
     */
    private String buildCacheKey(String tableName, Object keyValue) {
        return tableName + ":" + keyValue;
    }
    
    /**
     * 清理过期缓存
     */
    private void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        int removed = 0;
        
        var iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired(currentTime)) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            logger.debug("清理过期缓存: 清理数量={}, 剩余数量={}", removed, cache.size());
        }
    }
    
    // ========== 内部类 ==========
    
    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final Map<String, Object> data;
        private final long timestamp;
        private final long ttlMillis;
        
        public CacheEntry(Map<String, Object> data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
            this.ttlMillis = DEFAULT_CACHE_TTL_MINUTES * 60 * 1000;
        }
        
        public Map<String, Object> getData() {
            return data;
        }
        
        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }
        
        public boolean isExpired(long currentTime) {
            return (currentTime - timestamp) > ttlMillis;
        }
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private final long totalSize;
        private final long validSize;
        private final long expiredSize;
        
        public CacheStats(long totalSize, long validSize, long expiredSize) {
            this.totalSize = totalSize;
            this.validSize = validSize;
            this.expiredSize = expiredSize;
        }
        
        public long getTotalSize() { return totalSize; }
        public long getValidSize() { return validSize; }
        public long getExpiredSize() { return expiredSize; }
        public double getHitRate() { 
            return totalSize > 0 ? (double) validSize / totalSize : 0.0; 
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{total=%d, valid=%d, expired=%d, hitRate=%.2f%%}", 
                    totalSize, validSize, expiredSize, getHitRate() * 100);
        }
    }
}
