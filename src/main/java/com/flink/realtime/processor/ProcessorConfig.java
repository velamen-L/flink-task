package com.flink.realtime.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件处理器配置注解
 * 用于标记事件处理器支持的事件类型和相关配置
 * 
 * @author AI代码生成器  
 * @date 2024-12-27
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProcessorConfig {
    
    /**
     * 支持的事件类型路由键数组
     * 格式: domain:type:subType 或 domain:type
     * 
     * 示例:
     * - "wrongbook:wrongbook_add:manual"
     * - "wrongbook:wrongbook_fix:practice"  
     * - "user:login" (无子类型)
     */
    String[] eventTypes();
    
    /**
     * 处理器描述信息
     */
    String description() default "";
    
    /**
     * 处理器优先级，数值越大优先级越高
     * 当多个处理器支持同一事件类型时，优先级高的生效
     */
    int priority() default 0;
    
    /**
     * 是否启用该处理器
     */
    boolean enabled() default true;
}
