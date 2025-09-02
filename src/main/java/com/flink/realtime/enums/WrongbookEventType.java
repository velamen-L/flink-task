package com.flink.realtime.enums;

/**
 * 错题本事件类型枚举
 * 支持主类型和子类型的层级管理
 * 
 * @author AI代码生成器
 * @date 2024-12-27
 */
public enum WrongbookEventType {
    
    // 错题添加事件
    WRONGBOOK_ADD_MANUAL("wrongbook_add", "manual", "手动添加错题"),
    WRONGBOOK_ADD_AUTO("wrongbook_add", "auto", "自动添加错题"),
    WRONGBOOK_ADD_IMPORT("wrongbook_add", "import", "批量导入错题"),
    
    // 错题订正事件
    WRONGBOOK_FIX_PRACTICE("wrongbook_fix", "practice", "练习订正"),
    WRONGBOOK_FIX_EXAM("wrongbook_fix", "exam", "考试订正"),
    WRONGBOOK_FIX_HOMEWORK("wrongbook_fix", "homework", "作业订正"),
    
    // 错题删除事件
    WRONGBOOK_DELETE_USER("wrongbook_delete", "user", "用户删除"),
    WRONGBOOK_DELETE_SYSTEM("wrongbook_delete", "system", "系统删除"),
    WRONGBOOK_DELETE_BATCH("wrongbook_delete", "batch", "批量删除");
    
    private final String type;
    private final String subType;
    private final String description;
    
    WrongbookEventType(String type, String subType, String description) {
        this.type = type;
        this.subType = subType;
        this.description = description;
    }
    
    public String getType() {
        return type;
    }
    
    public String getSubType() {
        return subType;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取完整的路由键
     */
    public String getRoutingKey() {
        return "wrongbook:" + type + ":" + subType;
    }
    
    /**
     * 获取处理器类名
     */
    public String getProcessorClass() {
        String className = toCamelCase(type) + toCamelCase(subType) + "Processor";
        return "com.flink.realtime.topics.wrongbook.processor." + className;
    }
    
    /**
     * 根据type和subType查找枚举
     */
    public static WrongbookEventType fromTypeAndSubType(String type, String subType) {
        for (WrongbookEventType eventType : values()) {
            if (eventType.type.equals(type) && eventType.subType.equals(subType)) {
                return eventType;
            }
        }
        throw new IllegalArgumentException("未找到事件类型: " + type + ":" + subType);
    }
    
    /**
     * 根据路由键查找枚举
     */
    public static WrongbookEventType fromRoutingKey(String routingKey) {
        for (WrongbookEventType eventType : values()) {
            if (eventType.getRoutingKey().equals(routingKey)) {
                return eventType;
            }
        }
        throw new IllegalArgumentException("未找到路由键: " + routingKey);
    }
    
    /**
     * 获取所有主类型
     */
    public static String[] getAllTypes() {
        return java.util.Arrays.stream(values())
                .map(WrongbookEventType::getType)
                .distinct()
                .toArray(String[]::new);
    }
    
    /**
     * 获取指定主类型的所有子类型
     */
    public static String[] getSubTypes(String type) {
        return java.util.Arrays.stream(values())
                .filter(eventType -> eventType.getType().equals(type))
                .map(WrongbookEventType::getSubType)
                .toArray(String[]::new);
    }
    
    /**
     * 转换为驼峰命名
     */
    private String toCamelCase(String str) {
        String[] parts = str.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            result.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }
}
