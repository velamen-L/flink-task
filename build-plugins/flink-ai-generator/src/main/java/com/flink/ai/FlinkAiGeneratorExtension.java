package com.flink.ai;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

public abstract class FlinkAiGeneratorExtension {
    
    /**
     * 输入请求文件路径
     * 例如: job/wrongbook/flink-sql-request.md
     */
    public abstract RegularFileProperty getRequestFile();
    
    /**
     * 输出目录
     * 例如: build/flink-ai-output
     */
    public abstract DirectoryProperty getOutputDir();
    
    /**
     * ER图知识库目录
     * 例如: er-knowledge-base
     */
    public abstract DirectoryProperty getKnowledgeBaseDir();
    
    /**
     * 业务域名称
     * 例如: wrongbook
     */
    public abstract Property<String> getDomain();
    
    /**
     * 是否启用详细日志
     */
    public abstract Property<Boolean> getVerbose();
    
    /**
     * 是否跳过数据验证
     */
    public abstract Property<Boolean> getSkipValidation();
    
    /**
     * 是否强制更新ER知识库（忽略冲突）
     */
    public abstract Property<Boolean> getForceERUpdate();
    
    /**
     * 自定义Flink SQL模板路径
     */
    public abstract RegularFileProperty getSqlTemplate();
    
    public FlinkAiGeneratorExtension() {
        // 设置默认值
        getVerbose().convention(false);
        getSkipValidation().convention(false);
        getForceERUpdate().convention(false);
    }
}
