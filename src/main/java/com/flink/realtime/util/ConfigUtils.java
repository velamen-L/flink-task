package com.flink.realtime.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置文件工具类
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class ConfigUtils {
    
    private static Properties properties = new Properties();
    
    static {
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        try (InputStream inputStream = ConfigUtils.class.getResourceAsStream("/application.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败", e);
        }
    }
    
    /**
     * 获取配置值
     * 
     * @param key 配置键
     * @return 配置值
     */
    public static String getString(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * 获取配置值，如果不存在则返回默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * 获取整型配置值
     * 
     * @param key 配置键
     * @return 配置值
     */
    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
    
    /**
     * 获取整型配置值，如果不存在则返回默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }
    
    /**
     * 获取布尔型配置值
     * 
     * @param key 配置键
     * @return 配置值
     */
    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }
    
    /**
     * 获取布尔型配置值，如果不存在则返回默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    /**
     * 获取长整型配置值
     * 
     * @param key 配置键
     * @return 配置值
     */
    public static long getLong(String key) {
        return Long.parseLong(properties.getProperty(key));
    }
    
    /**
     * 获取长整型配置值，如果不存在则返回默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static long getLong(String key, long defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Long.parseLong(value) : defaultValue;
    }
}
