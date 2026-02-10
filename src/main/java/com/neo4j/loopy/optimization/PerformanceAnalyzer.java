package com.neo4j.loopy.optimization;

import com.neo4j.loopy.LoopyConfig;

/**
 * Performance analyzer that provides automatic tuning recommendations
 * based on system resources and Neo4j configuration.
 */
public class PerformanceAnalyzer {
    
    /**
     * Analyze system resources and provide optimization recommendations
     */
    public OptimizationRecommendations analyzeSystem() {
        OptimizationRecommendations recommendations = new OptimizationRecommendations();
        
        // Analyze system resources
        Runtime runtime = Runtime.getRuntime();
        int availableProcessors = runtime.availableProcessors();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        
        // Calculate optimal thread count
        int recommendedThreads = calculateOptimalThreads(availableProcessors);
        recommendations.setRecommendedThreads(recommendedThreads);
        
        // Calculate optimal batch size based on available memory
        int recommendedBatchSize = calculateOptimalBatchSize(maxMemory);
        recommendations.setRecommendedBatchSize(recommendedBatchSize);
        
        // Memory recommendations
        if (freeMemory < maxMemory * 0.2) { // Less than 20% memory available
            recommendations.addWarning("Low available memory detected. Consider reducing batch size or thread count.");
        }
        
        // CPU recommendations
        if (availableProcessors >= 8) {
            recommendations.addTip("Multi-core system detected. Consider enabling parallel execution mode.");
        }
        
        return recommendations;
    }
    
    /**
     * Auto-tune configuration based on system analysis
     */
    public LoopyConfig autoTune(LoopyConfig originalConfig) {
        OptimizationRecommendations recommendations = analyzeSystem();
        
        // Create a copy of the original config
        LoopyConfig tunedConfig = originalConfig; // In real implementation, would create a copy
        
        // Apply recommendations (would need setter methods or rebuild config)
        System.out.printf("🔧 Auto-tuning recommendations:%n");
        System.out.printf("   Optimal threads: %d%n", recommendations.getRecommendedThreads());
        System.out.printf("   Optimal batch size: %d%n", recommendations.getRecommendedBatchSize());
        
        for (String warning : recommendations.getWarnings()) {
            System.out.printf("   ⚠️ Warning: %s%n", warning);
        }
        
        for (String tip : recommendations.getTips()) {
            System.out.printf("   💡 Tip: %s%n", tip);
        }
        
        return tunedConfig;
    }
    
    /**
     * Calculate optimal thread count based on CPU cores
     */
    private int calculateOptimalThreads(int cores) {
        // For I/O intensive operations like database operations,
        // typically use more threads than cores
        if (cores <= 2) {
            return cores * 2;
        } else if (cores <= 4) {
            return cores * 3;
        } else {
            return Math.min(cores * 2, 32); // Cap at 32 threads
        }
    }
    
    /**
     * Calculate optimal batch size based on available memory
     */
    private int calculateOptimalBatchSize(long maxMemoryBytes) {
        // Convert to MB for easier calculation
        long maxMemoryMB = maxMemoryBytes / (1024 * 1024);
        
        if (maxMemoryMB < 512) {
            return 100;
        } else if (maxMemoryMB < 1024) {
            return 500;
        } else if (maxMemoryMB < 2048) {
            return 1000;
        } else {
            return 2000;
        }
    }
    
    /**
     * Performance profile for different usage scenarios
     */
    public static class LoadProfile {
        public static final LoadProfile LIGHT = new LoadProfile("light", 2, 100, 30, 0.3);
        public static final LoadProfile MEDIUM = new LoadProfile("medium", 4, 500, 120, 0.5);
        public static final LoadProfile HEAVY = new LoadProfile("heavy", 8, 1000, 300, 0.7);
        public static final LoadProfile STRESS = new LoadProfile("stress", 16, 2000, 600, 0.8);
        
        private final String name;
        private final int threads;
        private final int batchSize;
        private final int duration;
        private final double writeRatio;
        
        public LoadProfile(String name, int threads, int batchSize, int duration, double writeRatio) {
            this.name = name;
            this.threads = threads;
            this.batchSize = batchSize;
            this.duration = duration;
            this.writeRatio = writeRatio;
        }
        
        // Getters
        public String getName() { return name; }
        public int getThreads() { return threads; }
        public int getBatchSize() { return batchSize; }
        public int getDuration() { return duration; }
        public double getWriteRatio() { return writeRatio; }
        
        public void printProfile() {
            System.out.printf("Load Profile: %s%n", name.toUpperCase());
            System.out.printf("  Threads: %d%n", threads);
            System.out.printf("  Batch Size: %d%n", batchSize);
            System.out.printf("  Duration: %d seconds%n", duration);
            System.out.printf("  Write Ratio: %.1f%%%n", writeRatio * 100);
        }
    }
    
    /**
     * Get predefined load profile by name
     */
    public static LoadProfile getLoadProfile(String profileName) {
        switch (profileName.toLowerCase()) {
            case "light": return LoadProfile.LIGHT;
            case "medium": return LoadProfile.MEDIUM;
            case "heavy": return LoadProfile.HEAVY;
            case "stress": return LoadProfile.STRESS;
            default:
                throw new IllegalArgumentException("Unknown load profile: " + profileName + 
                    ". Available profiles: light, medium, heavy, stress");
        }
    }
}