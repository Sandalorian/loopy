package com.neo4j.loopy.optimization;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for performance optimization recommendations
 */
public class OptimizationRecommendations {
    private int recommendedThreads;
    private int recommendedBatchSize;
    private List<String> warnings = new ArrayList<>();
    private List<String> tips = new ArrayList<>();
    
    public int getRecommendedThreads() {
        return recommendedThreads;
    }
    
    public void setRecommendedThreads(int recommendedThreads) {
        this.recommendedThreads = recommendedThreads;
    }
    
    public int getRecommendedBatchSize() {
        return recommendedBatchSize;
    }
    
    public void setRecommendedBatchSize(int recommendedBatchSize) {
        this.recommendedBatchSize = recommendedBatchSize;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public List<String> getTips() {
        return tips;
    }
    
    public void addTip(String tip) {
        this.tips.add(tip);
    }
}