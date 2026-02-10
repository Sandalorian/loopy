package com.neo4j.loopy.commands;

import com.neo4j.loopy.LoopyApplication;
import com.neo4j.loopy.LoopyConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Benchmark command - Run predefined benchmark scenarios
 */
@Command(name = "benchmark", 
         description = "Run predefined benchmark scenarios",
         mixinStandardHelpOptions = true)
public class BenchmarkCommand implements Callable<Integer> {
    
    @Parameters(index = "0", 
                description = "Benchmark profile: light, medium, heavy, or stress",
                defaultValue = "medium")
    private String profile;
    
    @Option(names = {"--neo4j-uri", "-u"}, 
            description = "Neo4j connection URI",
            defaultValue = "${LOOPY_NEO4J_URI:-bolt://localhost:7687}")
    private String neo4jUri;
    
    @Option(names = {"--username", "-U"}, 
            description = "Neo4j username",
            defaultValue = "${LOOPY_USERNAME:-neo4j}")
    private String username;
    
    @Option(names = {"--password", "-P"}, 
            description = "Neo4j password", 
            interactive = true,
            defaultValue = "${LOOPY_PASSWORD:-password}")
    private String password;
    
    @Override
    public Integer call() throws Exception {
        System.out.println("\u001B[36mRunning '" + profile + "' benchmark profile...\u001B[0m");
        
        // Define benchmark profiles
        BenchmarkProfile benchProfile = getBenchmarkProfile(profile);
        if (benchProfile == null) {
            System.err.println("\u001B[31mUnknown benchmark profile: " + profile + "\u001B[0m");
            System.err.println("Available profiles: light, medium, heavy, stress");
            return 1;
        }
        
        // Print profile details
        printBenchmarkDetails(benchProfile);
        
        try {
            // Create LoopyApplication with benchmark settings
            LoopyApplication app = new LoopyApplication();
            
            // Set the benchmark configuration
            System.setProperty("neo4j.uri", neo4jUri);
            System.setProperty("neo4j.username", username);
            System.setProperty("neo4j.password", password);
            System.setProperty("threads", String.valueOf(benchProfile.threads));
            System.setProperty("duration.seconds", String.valueOf(benchProfile.duration));
            System.setProperty("write.ratio", String.valueOf(benchProfile.writeRatio));
            System.setProperty("batch.size", String.valueOf(benchProfile.batchSize));
            System.setProperty("report.interval.seconds", "5"); // More frequent reporting for benchmarks
            
            // Execute the benchmark
            return app.call();
            
        } catch (Exception e) {
            System.err.println("\u001B[31mBenchmark failed: " + e.getMessage() + "\u001B[0m");
            return 1;
        }
    }
    
    private BenchmarkProfile getBenchmarkProfile(String profile) {
        switch (profile.toLowerCase()) {
            case "light":
                return new BenchmarkProfile(2, 60, 0.5, 50, 
                    "Light load for development and testing");
            case "medium":
                return new BenchmarkProfile(4, 300, 0.7, 100, 
                    "Medium load for typical testing scenarios");
            case "heavy":
                return new BenchmarkProfile(8, 600, 0.8, 200, 
                    "Heavy load for performance testing");
            case "stress":
                return new BenchmarkProfile(16, 1800, 0.9, 500, 
                    "Stress test for maximum load evaluation");
            default:
                return null;
        }
    }
    
    private void printBenchmarkDetails(BenchmarkProfile profile) {
        System.out.println("\nBenchmark Configuration:");
        System.out.println("  Description: " + profile.description);
        System.out.println("  Threads: " + profile.threads);
        System.out.println("  Duration: " + profile.duration + " seconds");
        System.out.println("  Write Ratio: " + (profile.writeRatio * 100) + "%");
        System.out.println("  Batch Size: " + profile.batchSize);
        System.out.println();
    }
    
    private static class BenchmarkProfile {
        final int threads;
        final int duration;
        final double writeRatio;
        final int batchSize;
        final String description;
        
        BenchmarkProfile(int threads, int duration, double writeRatio, int batchSize, String description) {
            this.threads = threads;
            this.duration = duration;
            this.writeRatio = writeRatio;
            this.batchSize = batchSize;
            this.description = description;
        }
    }
}