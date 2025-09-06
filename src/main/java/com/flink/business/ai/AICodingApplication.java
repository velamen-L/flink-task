package com.flink.business.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Coding 平台启动类
 * 
 * FlinkSQL AI编程平台主应用
 * 提供智能化的FlinkSQL开发、质量检查、优化建议等功能
 * 
 * 核心特性：
 * - 自然语言转FlinkSQL
 * - 智能模板匹配
 * - 实时质量检查
 * - 性能优化建议
 * - 企业知识库管理
 * - 协作开发支持
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {"com.flink.business"})
public class AICodingApplication {

    public static void main(String[] args) {
        try {
            log.info("正在启动FlinkSQL AI Coding平台...");
            
            // 设置系统属性
            System.setProperty("spring.application.name", "flinksql-ai-coding-platform");
            System.setProperty("server.port", "8080");
            
            // 启动Spring Boot应用
            SpringApplication application = new SpringApplication(AICodingApplication.class);
            
            // 设置默认配置
            application.setDefaultProperties(getDefaultProperties());
            
            // 启动应用
            var context = application.run(args);
            
            // 输出启动信息
            printStartupInfo(context);
            
            log.info("FlinkSQL AI Coding平台启动成功！");
            
        } catch (Exception e) {
            log.error("FlinkSQL AI Coding平台启动失败", e);
            System.exit(1);
        }
    }

    /**
     * 获取默认配置属性
     */
    private static java.util.Properties getDefaultProperties() {
        java.util.Properties properties = new java.util.Properties();
        
        // 应用基础配置
        properties.setProperty("spring.application.name", "flinksql-ai-coding-platform");
        properties.setProperty("server.port", "8080");
        properties.setProperty("server.servlet.context-path", "/ai-coding");
        
        // 数据库配置
        properties.setProperty("spring.datasource.url", "jdbc:h2:mem:testdb");
        properties.setProperty("spring.datasource.driverClassName", "org.h2.Driver");
        properties.setProperty("spring.datasource.username", "sa");
        properties.setProperty("spring.datasource.password", "password");
        
        // JPA配置
        properties.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");
        properties.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        properties.setProperty("spring.jpa.show-sql", "false");
        
        // Redis配置（如果可用）
        properties.setProperty("spring.redis.host", "localhost");
        properties.setProperty("spring.redis.port", "6379");
        properties.setProperty("spring.redis.timeout", "2000ms");
        
        // 日志配置
        properties.setProperty("logging.level.com.flink.business.ai", "INFO");
        properties.setProperty("logging.level.org.springframework", "WARN");
        properties.setProperty("logging.pattern.console", 
            "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        
        // AI平台特定配置
        properties.setProperty("ai.platform.enabled", "true");
        properties.setProperty("ai.platform.debug-mode", "true");
        properties.setProperty("ai.llm.default-model", "gpt-4");
        properties.setProperty("ai.llm.temperature", "0.1");
        properties.setProperty("ai.quality-check.enabled", "true");
        properties.setProperty("ai.knowledge-base.auto-update", "true");
        
        return properties;
    }

    /**
     * 打印启动信息
     */
    private static void printStartupInfo(org.springframework.context.ConfigurableApplicationContext context) {
        String port = context.getEnvironment().getProperty("server.port", "8080");
        String contextPath = context.getEnvironment().getProperty("server.servlet.context-path", "");
        
        log.info("\n" +
            "================================================================================\n" +
            "  FlinkSQL AI Coding Platform 启动成功！\n" +
            "================================================================================\n" +
            "  应用名称: FlinkSQL AI编程平台\n" +
            "  版本信息: v1.0.0\n" +
            "  访问地址: http://localhost:{}{}\n" +
            "  API文档: http://localhost:{}{}/swagger-ui.html\n" +
            "  健康检查: http://localhost:{}{}/actuator/health\n" +
            "  管理界面: http://localhost:{}{}/actuator\n" +
            "================================================================================\n" +
            "  核心功能:\n" +
            "  ✓ 自然语言转FlinkSQL\n" +
            "  ✓ 智能模板匹配和推荐\n" +
            "  ✓ 实时质量检查和优化\n" +
            "  ✓ 企业知识库管理\n" +
            "  ✓ 协作开发和版本控制\n" +
            "  ✓ 性能监控和告警\n" +
            "================================================================================\n",
            port, contextPath, port, contextPath, port, contextPath, port, contextPath);
    }
}
