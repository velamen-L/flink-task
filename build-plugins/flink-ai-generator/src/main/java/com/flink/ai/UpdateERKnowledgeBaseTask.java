package com.flink.ai;

import com.flink.ai.service.ERKnowledgeBaseService;
import com.flink.ai.model.ERUpdateResult;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.io.File;

@CacheableTask
public abstract class UpdateERKnowledgeBaseTask extends DefaultTask {
    
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getRequestFile();
    
    @OutputDirectory
    public abstract DirectoryProperty getKnowledgeBaseDir();
    
    @TaskAction
    public void updateERKnowledgeBase() {
        try {
            File requestFile = getRequestFile().get().getAsFile();
            File knowledgeBaseDir = getKnowledgeBaseDir().get().getAsFile();
            
            getLogger().lifecycle("Updating ER knowledge base from: {}", requestFile.getAbsolutePath());
            
            // 创建知识库目录
            knowledgeBaseDir.mkdirs();
            
            // 执行ER知识库更新
            ERKnowledgeBaseService service = new ERKnowledgeBaseService();
            ERUpdateResult result = service.updateKnowledgeBase(requestFile, knowledgeBaseDir);
            
            // 输出结果
            getLogger().lifecycle("✅ ER knowledge base updated successfully!");
            getLogger().lifecycle("📊 Entities: {} added, {} updated", 
                    result.getEntitiesAdded(), result.getEntitiesUpdated());
            getLogger().lifecycle("🔗 Relationships: {} added", result.getRelationshipsAdded());
            
            if (!result.getConflicts().isEmpty()) {
                getLogger().warn("⚠️  Conflicts detected:");
                for (String conflict : result.getConflicts()) {
                    getLogger().warn("   - {}", conflict);
                }
            }
            
            if (!result.getEnhancedDiagrams().isEmpty()) {
                getLogger().lifecycle("📈 Enhanced diagrams generated:");
                for (File diagram : result.getEnhancedDiagrams()) {
                    getLogger().lifecycle("   - {}", diagram.getName());
                }
            }
            
        } catch (Exception e) {
            getLogger().error("❌ Failed to update ER knowledge base", e);
            throw new TaskExecutionException(this, e);
        }
    }
}
