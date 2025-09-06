package com.flink.business.core.service;

import com.flink.business.core.config.JobDomainConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 配置加载器
 * 
 * 职责：
 * 1. 从多种来源加载作业域配置
 * 2. 支持YAML和JSON格式配置文件
 * 3. 提供配置文件的验证和解析
 * 4. 支持配置热加载和监听
 * 
 * 配置加载优先级：
 * 1. jobdomain/{domain}/config/{domain}-job.yml
 * 2. src/main/resources/job-domains/{domain}.yml
 * 3. 环境变量配置
 * 4. 默认配置
 */
@Slf4j
@Service
public class ConfigLoader {

    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;

    public ConfigLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.jsonMapper = new ObjectMapper();
    }

    /**
     * 加载作业域配置
     */
    public JobDomainConfig.DomainConfig loadDomainConfig(String domainName) {
        log.info("加载作业域配置: {}", domainName);

        // 尝试从多个位置加载配置
        JobDomainConfig.DomainConfig config = null;

        // 1. 从jobdomain目录加载
        config = loadFromJobDomainDirectory(domainName);
        if (config != null) {
            log.info("从jobdomain目录加载配置成功: {}", domainName);
            return config;
        }

        // 2. 从resources目录加载
        config = loadFromResourcesDirectory(domainName);
        if (config != null) {
            log.info("从resources目录加载配置成功: {}", domainName);
            return config;
        }

        // 3. 从环境变量加载
        config = loadFromEnvironment(domainName);
        if (config != null) {
            log.info("从环境变量加载配置成功: {}", domainName);
            return config;
        }

        log.warn("未找到作业域配置: {}", domainName);
        return null;
    }

    /**
     * 从jobdomain目录加载配置
     */
    private JobDomainConfig.DomainConfig loadFromJobDomainDirectory(String domainName) {
        try {
            // 尝试YAML格式
            Path yamlPath = Paths.get("jobdomain", domainName, "config", domainName + "-job.yml");
            if (Files.exists(yamlPath)) {
                String content = Files.readString(yamlPath);
                return parseYamlConfig(content);
            }

            // 尝试JSON格式
            Path jsonPath = Paths.get("jobdomain", domainName, "config", domainName + "-job.json");
            if (Files.exists(jsonPath)) {
                String content = Files.readString(jsonPath);
                return parseJsonConfig(content);
            }

        } catch (Exception e) {
            log.debug("从jobdomain目录加载配置失败: {}", domainName, e);
        }

        return null;
    }

    /**
     * 从resources目录加载配置
     */
    private JobDomainConfig.DomainConfig loadFromResourcesDirectory(String domainName) {
        try {
            // 尝试YAML格式
            String yamlResource = "/job-domains/" + domainName + ".yml";
            InputStream yamlStream = getClass().getResourceAsStream(yamlResource);
            if (yamlStream != null) {
                String content = new String(yamlStream.readAllBytes());
                return parseYamlConfig(content);
            }

            // 尝试JSON格式
            String jsonResource = "/job-domains/" + domainName + ".json";
            InputStream jsonStream = getClass().getResourceAsStream(jsonResource);
            if (jsonStream != null) {
                String content = new String(jsonStream.readAllBytes());
                return parseJsonConfig(content);
            }

        } catch (Exception e) {
            log.debug("从resources目录加载配置失败: {}", domainName, e);
        }

        return null;
    }

    /**
     * 从环境变量加载配置
     */
    private JobDomainConfig.DomainConfig loadFromEnvironment(String domainName) {
        try {
            String envKey = "JOB_DOMAIN_" + domainName.toUpperCase().replace("-", "_") + "_CONFIG";
            String configContent = System.getenv(envKey);
            
            if (configContent != null && !configContent.trim().isEmpty()) {
                // 尝试解析为YAML或JSON
                if (configContent.trim().startsWith("{")) {
                    return parseJsonConfig(configContent);
                } else {
                    return parseYamlConfig(configContent);
                }
            }

        } catch (Exception e) {
            log.debug("从环境变量加载配置失败: {}", domainName, e);
        }

        return null;
    }

    /**
     * 解析YAML配置
     */
    private JobDomainConfig.DomainConfig parseYamlConfig(String yamlContent) {
        try {
            return yamlMapper.readValue(yamlContent, JobDomainConfig.DomainConfig.class);
        } catch (Exception e) {
            log.error("解析YAML配置失败", e);
            throw new RuntimeException("解析YAML配置失败", e);
        }
    }

    /**
     * 解析JSON配置
     */
    private JobDomainConfig.DomainConfig parseJsonConfig(String jsonContent) {
        try {
            return jsonMapper.readValue(jsonContent, JobDomainConfig.DomainConfig.class);
        } catch (Exception e) {
            log.error("解析JSON配置失败", e);
            throw new RuntimeException("解析JSON配置失败", e);
        }
    }

    /**
     * 验证配置完整性
     */
    public boolean validateConfig(JobDomainConfig.DomainConfig config) {
        if (config == null) {
            return false;
        }

        // 验证元数据
        if (config.getMetadata() == null || 
            config.getMetadata().getJobName() == null) {
            log.warn("配置缺少必要的元数据信息");
            return false;
        }

        // 验证事件源
        if (config.getEventSources() == null || config.getEventSources().isEmpty()) {
            log.warn("配置缺少事件源定义");
            return false;
        }

        // 验证输出配置
        if (config.getOutputs() == null || config.getOutputs().isEmpty()) {
            log.warn("配置缺少输出定义");
            return false;
        }

        return true;
    }

    /**
     * 保存配置到文件
     */
    public void saveConfig(String domainName, JobDomainConfig.DomainConfig config) {
        try {
            // 确保目录存在
            Path configDir = Paths.get("jobdomain", domainName, "config");
            Files.createDirectories(configDir);

            // 保存为YAML格式
            Path configFile = configDir.resolve(domainName + "-job.yml");
            String yamlContent = yamlMapper.writeValueAsString(config);
            Files.writeString(configFile, yamlContent);

            log.info("配置保存成功: {}", configFile);

        } catch (Exception e) {
            log.error("保存配置失败: {}", domainName, e);
            throw new RuntimeException("保存配置失败", e);
        }
    }

    /**
     * 检查配置文件是否存在
     */
    public boolean configExists(String domainName) {
        // 检查jobdomain目录
        Path yamlPath = Paths.get("jobdomain", domainName, "config", domainName + "-job.yml");
        Path jsonPath = Paths.get("jobdomain", domainName, "config", domainName + "-job.json");
        
        if (Files.exists(yamlPath) || Files.exists(jsonPath)) {
            return true;
        }

        // 检查resources目录
        String yamlResource = "/job-domains/" + domainName + ".yml";
        String jsonResource = "/job-domains/" + domainName + ".json";
        
        return getClass().getResourceAsStream(yamlResource) != null ||
               getClass().getResourceAsStream(jsonResource) != null;
    }

    /**
     * 获取配置文件路径
     */
    public String getConfigPath(String domainName) {
        Path yamlPath = Paths.get("jobdomain", domainName, "config", domainName + "-job.yml");
        if (Files.exists(yamlPath)) {
            return yamlPath.toString();
        }

        Path jsonPath = Paths.get("jobdomain", domainName, "config", domainName + "-job.json");
        if (Files.exists(jsonPath)) {
            return jsonPath.toString();
        }

        return "resources:/job-domains/" + domainName + ".yml";
    }
}
