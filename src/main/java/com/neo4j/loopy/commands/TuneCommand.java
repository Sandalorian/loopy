package com.neo4j.loopy.commands;

import com.neo4j.loopy.LoopyConfig;
import com.neo4j.loopy.optimization.PerformanceAnalyzer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Command for performance tuning and optimization
 */
@Command(name = "tune", 
         description = "Analyze system performance and provide optimization recommendations",
         mixinStandardHelpOptions = true)
public class TuneCommand implements Callable<Integer> {
    
    @Option(names = {"--config", "-c"}, 
            description = "Configuration file to analyze")
    private File configFile;
    
    @Option(names = {"--profile", "-p"}, 
            description = "Load profile to apply (light, medium, heavy, stress)")
    private String profile;
    
    @Option(names = {"--auto-tune"}, 
            description = "Apply automatic tuning recommendations")
    private boolean autoTune;
    
    @Option(names = {"--show-profiles"}, 
            description = "Show all available load profiles")
    private boolean showProfiles;
    
    @Override
    public Integer call() throws Exception {
        if (showProfiles) {
            showLoadProfiles();
            return 0;
        }
        
        if (profile != null) {
            showLoadProfile(profile);
            return 0;
        }
        
        PerformanceAnalyzer analyzer = new PerformanceAnalyzer();
        
        if (autoTune) {
            performAutoTuning(analyzer);
        } else {
            performAnalysis(analyzer);
        }
        
        return 0;
    }
    
    private void showLoadProfiles() {
        System.out.println("\u001B[36m=== Available Load Profiles ===\u001B[0m\n");
        
        PerformanceAnalyzer.LoadProfile.LIGHT.printProfile();
        System.out.println();
        PerformanceAnalyzer.LoadProfile.MEDIUM.printProfile();
        System.out.println();
        PerformanceAnalyzer.LoadProfile.HEAVY.printProfile();
        System.out.println();
        PerformanceAnalyzer.LoadProfile.STRESS.printProfile();
        
        System.out.println("\n\u001B[33mUsage:\u001B[0m");
        System.out.println("  loopy tune --profile light");
        System.out.println("  loopy run --threads 2 --batch-size 100 --duration 30 --write-ratio 0.3");
    }
    
    private void showLoadProfile(String profileName) {
        try {
            PerformanceAnalyzer.LoadProfile profile = PerformanceAnalyzer.getLoadProfile(profileName);
            System.out.println("\u001B[36m=== Load Profile Details ===\u001B[0m\n");
            profile.printProfile();
            
            System.out.println("\n\u001B[33mTo use this profile:\u001B[0m");
            System.out.printf("  loopy run --threads %d --batch-size %d --duration %d --write-ratio %.1f%n", 
                profile.getThreads(), profile.getBatchSize(), 
                profile.getDuration(), profile.getWriteRatio());
        } catch (IllegalArgumentException e) {
            System.err.println("\u001B[31mError: " + e.getMessage() + "\u001B[0m");
            return;
        }
    }
    
    private void performAnalysis(PerformanceAnalyzer analyzer) {
        System.out.println("\u001B[36m=== System Performance Analysis ===\u001B[0m\n");
        
        // Display system information
        Runtime runtime = Runtime.getRuntime();
        System.out.printf("System Information:%n");
        System.out.printf("  CPU Cores: %d%n", runtime.availableProcessors());
        System.out.printf("  Max Memory: %.1f GB%n", runtime.maxMemory() / (1024.0 * 1024.0 * 1024.0));
        System.out.printf("  Total Memory: %.1f GB%n", runtime.totalMemory() / (1024.0 * 1024.0 * 1024.0));
        System.out.printf("  Free Memory: %.1f GB%n", runtime.freeMemory() / (1024.0 * 1024.0 * 1024.0));
        
        System.out.println();
        
        // Get optimization recommendations
        analyzer.analyzeSystem();
        
        System.out.println("\n\u001B[33mTo apply auto-tuning:\u001B[0m");
        System.out.println("  loopy tune --auto-tune");
    }
    
    private void performAutoTuning(PerformanceAnalyzer analyzer) {
        System.out.println("\u001B[36m=== Auto-Tuning Analysis ===\u001B[0m\n");
        
        LoopyConfig config = new LoopyConfig(); // Load current config
        analyzer.autoTune(config);
        
        System.out.println("\n\u001B[32m✅ Auto-tuning analysis complete!\u001B[0m");
        System.out.println("\nTo create an optimized configuration file:");
        System.out.println("  loopy config init --optimized");
    }
}