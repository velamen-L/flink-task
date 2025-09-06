package com.flink.ai.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ERUpdateResult {
    private int entitiesAdded;
    private int entitiesUpdated;
    private int relationshipsAdded;
    private List<String> conflicts = new ArrayList<>();
    private List<File> enhancedDiagrams = new ArrayList<>();
    
    public int getEntitiesAdded() { return entitiesAdded; }
    public void setEntitiesAdded(int entitiesAdded) { this.entitiesAdded = entitiesAdded; }
    
    public int getEntitiesUpdated() { return entitiesUpdated; }
    public void setEntitiesUpdated(int entitiesUpdated) { this.entitiesUpdated = entitiesUpdated; }
    
    public int getRelationshipsAdded() { return relationshipsAdded; }
    public void setRelationshipsAdded(int relationshipsAdded) { this.relationshipsAdded = relationshipsAdded; }
    
    public List<String> getConflicts() { return conflicts; }
    public void setConflicts(List<String> conflicts) { this.conflicts = conflicts; }
    
    public List<File> getEnhancedDiagrams() { return enhancedDiagrams; }
    public void setEnhancedDiagrams(List<File> enhancedDiagrams) { this.enhancedDiagrams = enhancedDiagrams; }
}
