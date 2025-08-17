package com.flink.realtime.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flink.realtime.util.ConfigUtils;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 路由配置管理工具
 * 提供配置的增删改查功能
 * 
 * @author yangfanlin
 * @date 2025-01-17
 */
public class RoutingConfigManager {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 添加或更新路由配置
     */
    public static void upsertRoutingConfig(String domain, String eventType, 
                                         String processorClass, Map<String, Object> outputConfig) 
            throws SQLException {
        
        String sql = """
            INSERT INTO flink_routing_config (domain, event_type, processor_class, output_config, is_enabled)
            VALUES (?, ?, ?, ?, true)
            ON DUPLICATE KEY UPDATE 
                processor_class = VALUES(processor_class),
                output_config = VALUES(output_config),
                is_enabled = VALUES(is_enabled),
                update_time = CURRENT_TIMESTAMP
            """;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, domain);
            stmt.setString(2, eventType);
            stmt.setString(3, processorClass);
            stmt.setString(4, objectMapper.writeValueAsString(outputConfig));
            
            stmt.executeUpdate();
            System.out.println("路由配置已更新: " + domain + ":" + eventType);
        } catch (Exception e) {
            throw new SQLException("更新路由配置失败", e);
        }
    }
    
    /**
     * 禁用路由配置
     */
    public static void disableRoutingConfig(String domain, String eventType) throws SQLException {
        String sql = "UPDATE flink_routing_config SET is_enabled = false, update_time = CURRENT_TIMESTAMP WHERE domain = ? AND event_type = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, domain);
            stmt.setString(2, eventType);
            
            int affected = stmt.executeUpdate();
            System.out.println("路由配置已禁用: " + domain + ":" + eventType + ", 影响行数: " + affected);
        }
    }
    
    /**
     * 启用路由配置
     */
    public static void enableRoutingConfig(String domain, String eventType) throws SQLException {
        String sql = "UPDATE flink_routing_config SET is_enabled = true, update_time = CURRENT_TIMESTAMP WHERE domain = ? AND event_type = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, domain);
            stmt.setString(2, eventType);
            
            int affected = stmt.executeUpdate();
            System.out.println("路由配置已启用: " + domain + ":" + eventType + ", 影响行数: " + affected);
        }
    }
    
    /**
     * 删除路由配置
     */
    public static void deleteRoutingConfig(String domain, String eventType) throws SQLException {
        String sql = "DELETE FROM flink_routing_config WHERE domain = ? AND event_type = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, domain);
            stmt.setString(2, eventType);
            
            int affected = stmt.executeUpdate();
            System.out.println("路由配置已删除: " + domain + ":" + eventType + ", 影响行数: " + affected);
        }
    }
    
    /**
     * 查询路由配置
     */
    public static void listRoutingConfigs(String domain) throws SQLException {
        String sql = domain != null ? 
            "SELECT * FROM flink_routing_config WHERE domain = ? ORDER BY priority DESC, update_time DESC" :
            "SELECT * FROM flink_routing_config ORDER BY domain, priority DESC, update_time DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (domain != null) {
                stmt.setString(1, domain);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("========== " + (domain != null ? domain + " 域" : "全部") + "路由配置 ==========");
                System.out.printf("%-15s %-25s %-50s %-50s %-8s %-5s%n", 
                        "Domain", "EventType", "ProcessorClass", "OutputConfig", "Enabled", "Priority");
                System.out.println("-".repeat(150));
                
                while (rs.next()) {
                    System.out.printf("%-15s %-25s %-50s %-50s %-8s %-5d%n",
                            rs.getString("domain"),
                            rs.getString("event_type"),
                            rs.getString("processor_class"),
                            rs.getString("output_config"),
                            rs.getBoolean("is_enabled") ? "启用" : "禁用",
                            rs.getInt("priority"));
                }
            }
        }
    }
    
    /**
     * 创建配置表
     */
    public static void createConfigTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS flink_routing_config (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                domain VARCHAR(50) NOT NULL COMMENT '业务域',
                event_type VARCHAR(100) NOT NULL COMMENT '事件类型',
                processor_class VARCHAR(200) NOT NULL COMMENT '处理器类名',
                output_config JSON COMMENT '输出配置',
                is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
                priority INT DEFAULT 100 COMMENT '优先级',
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                UNIQUE KEY uk_domain_type (domain, event_type),
                INDEX idx_domain (domain),
                INDEX idx_update_time (update_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Flink动态路由配置表'
            """;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(sql);
            System.out.println("配置表创建完成");
        }
    }
    
    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL驱动加载失败", e);
        }
        
        return DriverManager.getConnection(
                ConfigUtils.getString("mysql.url"),
                ConfigUtils.getString("mysql.username"),
                ConfigUtils.getString("mysql.password"));
    }
    
    // 示例使用方法
    public static void main(String[] args) throws SQLException {
        if (args.length == 0) {
            System.out.println("使用方法：");
            System.out.println("  java RoutingConfigManager create-table");
            System.out.println("  java RoutingConfigManager list [domain]");
            System.out.println("  java RoutingConfigManager add <domain> <eventType> <processorClass>");
            System.out.println("  java RoutingConfigManager enable <domain> <eventType>");
            System.out.println("  java RoutingConfigManager disable <domain> <eventType>");
            System.out.println("  java RoutingConfigManager delete <domain> <eventType>");
            return;
        }
        
        String action = args[0];
        
        try {
            switch (action) {
                case "create-table":
                    createConfigTable();
                    break;
                case "list":
                    String domain = args.length > 1 ? args[1] : null;
                    listRoutingConfigs(domain);
                    break;
                case "add":
                    if (args.length < 4) {
                        System.out.println("参数不足：add <domain> <eventType> <processorClass>");
                        return;
                    }
                    Map<String, Object> config = new HashMap<>();
                    config.put("sinks", java.util.Arrays.asList(args[1] + "_wide_table"));
                    upsertRoutingConfig(args[1], args[2], args[3], config);
                    break;
                case "enable":
                    if (args.length < 3) {
                        System.out.println("参数不足：enable <domain> <eventType>");
                        return;
                    }
                    enableRoutingConfig(args[1], args[2]);
                    break;
                case "disable":
                    if (args.length < 3) {
                        System.out.println("参数不足：disable <domain> <eventType>");
                        return;
                    }
                    disableRoutingConfig(args[1], args[2]);
                    break;
                case "delete":
                    if (args.length < 3) {
                        System.out.println("参数不足：delete <domain> <eventType>");
                        return;
                    }
                    deleteRoutingConfig(args[1], args[2]);
                    break;
                default:
                    System.out.println("未知操作：" + action);
            }
        } catch (SQLException e) {
            System.err.println("操作失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
