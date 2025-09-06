package com.flink.business.core.processor;

import com.flink.business.core.config.JobDomainConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理器工厂
 * 
 * 职责：
 * 1. 根据作业域创建对应的处理器实例
 * 2. 管理处理器的缓存和生命周期
 * 3. 支持动态处理器注册和发现
 * 4. 提供处理器的统一创建和配置接口
 * 
 * 设计特点：
 * - 基于Spring容器的处理器管理
 * - 支持按约定自动发现处理器
 * - 缓存处理器实例提高性能
 * - 统一的处理器初始化流程
 */
@Slf4j
@Service
public class ProcessorFactory {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 处理器缓存，避免重复创建
     */
    private final Map<String, AbstractBusinessProcessor> processorCache = new ConcurrentHashMap<>();

    /**
     * 处理器类型映射，用于自定义处理器类
     */
    private final Map<String, Class<? extends AbstractBusinessProcessor>> processorTypes = new ConcurrentHashMap<>();

    /**
     * 创建业务处理器
     */
    public AbstractBusinessProcessor createProcessor(String domainName, JobDomainConfig.DomainConfig domainConfig) {
        log.info("创建业务处理器: {}", domainName);
        
        // 从缓存获取
        String cacheKey = buildCacheKey(domainName, domainConfig);
        AbstractBusinessProcessor cachedProcessor = processorCache.get(cacheKey);
        if (cachedProcessor != null) {
            log.debug("使用缓存的处理器: {}", domainName);
            return cachedProcessor;
        }
        
        // 创建新的处理器实例
        AbstractBusinessProcessor processor = createProcessorInstance(domainName, domainConfig);
        
        // 初始化处理器
        processor.initialize(domainName, domainConfig);
        
        // 缓存处理器
        processorCache.put(cacheKey, processor);
        
        log.info("业务处理器创建完成: {}", domainName);
        return processor;
    }

    /**
     * 创建处理器实例
     */
    private AbstractBusinessProcessor createProcessorInstance(String domainName, JobDomainConfig.DomainConfig domainConfig) {
        // 1. 尝试从自定义类型映射获取
        Class<? extends AbstractBusinessProcessor> processorClass = processorTypes.get(domainName);
        if (processorClass != null) {
            return createProcessorFromClass(processorClass);
        }
        
        // 2. 尝试按约定查找Spring Bean
        AbstractBusinessProcessor processor = findProcessorByConvention(domainName);
        if (processor != null) {
            return processor;
        }
        
        // 3. 根据作业类型创建通用处理器
        return createGenericProcessor(domainConfig);
    }

    /**
     * 按约定查找处理器Bean
     * 约定：域名 + "JobProcessor"，例如 wrongbook -> wrongbookJobProcessor
     */
    private AbstractBusinessProcessor findProcessorByConvention(String domainName) {
        try {
            // 转换域名为Bean名称
            String beanName = convertDomainToBeanName(domainName);
            
            if (applicationContext.containsBean(beanName)) {
                Object bean = applicationContext.getBean(beanName);
                if (bean instanceof AbstractBusinessProcessor) {
                    log.info("找到约定的处理器Bean: {}", beanName);
                    return (AbstractBusinessProcessor) bean;
                }
            }
        } catch (Exception e) {
            log.debug("按约定查找处理器失败: {}", domainName, e);
        }
        
        return null;
    }

    /**
     * 转换域名为Bean名称
     * wrongbook -> wrongbookJobProcessor
     * user-daily-stats -> userDailyStatsJobProcessor
     */
    private String convertDomainToBeanName(String domainName) {
        // 转换短横线为驼峰
        String camelCase = toCamelCase(domainName);
        return camelCase + "JobProcessor";
    }

    /**
     * 转换为驼峰命名
     */
    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        
        for (char c : input.toCharArray()) {
            if (c == '-' || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        
        return result.toString();
    }

    /**
     * 从类创建处理器实例
     */
    private AbstractBusinessProcessor createProcessorFromClass(Class<? extends AbstractBusinessProcessor> processorClass) {
        try {
            return applicationContext.getBean(processorClass);
        } catch (Exception e) {
            log.warn("从Spring容器获取处理器失败，尝试直接创建: {}", processorClass.getSimpleName(), e);
            try {
                return processorClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("无法创建处理器实例: " + processorClass.getSimpleName(), ex);
            }
        }
    }

    /**
     * 创建通用处理器
     */
    private AbstractBusinessProcessor createGenericProcessor(JobDomainConfig.DomainConfig domainConfig) {
        String jobType = domainConfig.getMetadata().getJobType();
        
        return switch (jobType) {
            case "SINGLE_DOMAIN" -> createSingleDomainProcessor();
            case "MULTI_DOMAIN" -> createMultiDomainProcessor();
            case "CROSS_DOMAIN" -> createCrossDomainProcessor();
            default -> createDefaultProcessor();
        };
    }

    /**
     * 创建单域处理器
     */
    private AbstractBusinessProcessor createSingleDomainProcessor() {
        return applicationContext.getBean("singleDomainProcessor", AbstractBusinessProcessor.class);
    }

    /**
     * 创建多域处理器
     */
    private AbstractBusinessProcessor createMultiDomainProcessor() {
        return applicationContext.getBean("multiDomainProcessor", AbstractBusinessProcessor.class);
    }

    /**
     * 创建跨域处理器
     */
    private AbstractBusinessProcessor createCrossDomainProcessor() {
        return applicationContext.getBean("crossDomainProcessor", AbstractBusinessProcessor.class);
    }

    /**
     * 创建默认处理器
     */
    private AbstractBusinessProcessor createDefaultProcessor() {
        return applicationContext.getBean("defaultBusinessProcessor", AbstractBusinessProcessor.class);
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(String domainName, JobDomainConfig.DomainConfig domainConfig) {
        return domainName + ":" + domainConfig.getMetadata().getVersion();
    }

    /**
     * 注册自定义处理器类型
     */
    public void registerProcessorType(String domainName, Class<? extends AbstractBusinessProcessor> processorClass) {
        processorTypes.put(domainName, processorClass);
        log.info("注册自定义处理器类型: {} -> {}", domainName, processorClass.getSimpleName());
    }

    /**
     * 清除处理器缓存
     */
    public void clearCache() {
        processorCache.clear();
        log.info("处理器缓存已清除");
    }

    /**
     * 清除特定域的缓存
     */
    public void clearCache(String domainName) {
        processorCache.entrySet().removeIf(entry -> entry.getKey().startsWith(domainName + ":"));
        log.info("已清除域 {} 的处理器缓存", domainName);
    }
}
