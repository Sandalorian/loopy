package com.neo4j.loopy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main application class for Loopy - Neo4j load generator
 */
public class LoopyApplication {
    private final LoopyConfig config;
    private final LoopyStats stats;
    private final ExecutorService executorService;
    private final ScheduledExecutorService reportingService;
    private final List<LoopyWorker> workers;
    private volatile boolean running = false;
    
    public LoopyApplication(LoopyConfig config) {
        this.config = config;
        this.stats = new LoopyStats(config);
        this.executorService = Executors.newFixedThreadPool(config.getThreads());
        this.reportingService = Executors.newScheduledThreadPool(1);
        this.workers = new ArrayList<>();
    }
    
    public void start() {
        System.out.println("Starting Loopy load generator...");
        System.out.println("Configuration:");
        System.out.println("  Neo4j URI: " + config.getNeo4jUri());
        System.out.println("  Threads: " + config.getThreads());
        System.out.println("  Duration: " + config.getDurationSeconds() + " seconds");
        System.out.println("  Write Ratio: " + (config.getWriteRatio() * 100) + "%");
        System.out.println("  Node Labels: " + config.getNodeLabels());
        System.out.println("  Relationship Types: " + config.getRelationshipTypes());
        System.out.println();
        
        running = true;
        
        // Start worker threads
        for (int i = 0; i < config.getThreads(); i++) {
            LoopyWorker worker = new LoopyWorker(config, stats);
            workers.add(worker);
            executorService.submit(worker);
        }
        
        // Start reporting
        reportingService.scheduleAtFixedRate(
            stats::printStats,
            config.getReportIntervalSeconds(),
            config.getReportIntervalSeconds(),
            TimeUnit.SECONDS
        );
        
        // Setup shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        // Run for specified duration
        try {
            Thread.sleep(config.getDurationSeconds() * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        shutdown();
    }
    
    public void shutdown() {
        if (!running) return;
        
        System.out.println("\nShutting down Loopy...");
        running = false;
        
        // Stop workers
        workers.forEach(LoopyWorker::stop);
        
        // Shutdown thread pools
        executorService.shutdown();
        reportingService.shutdown();
        
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!reportingService.awaitTermination(5, TimeUnit.SECONDS)) {
                reportingService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            reportingService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Print final statistics
        System.out.println("\nFinal Statistics:");
        stats.printStats();
        System.out.println("Total Operations: " + stats.getTotalOperations());
        System.out.println("Total Errors: " + stats.getErrors());
        
        // Close CSV writer if enabled
        stats.close();
        
        System.out.println("Loopy shutdown complete.");
    }
    
    public static void main(String[] args) {
        try {
            LoopyConfig config = new LoopyConfig();
            
            // Check for custom config file
            String configFile = null;
            for (String arg : args) {
                if (arg.startsWith("--config=")) {
                    configFile = arg.substring(9);
                    break;
                }
            }
            
            if (configFile != null) {
                config = new LoopyConfig(configFile);
            }
            
            // Override with command line arguments
            config.overrideFromArgs(args);
            
            LoopyApplication app = new LoopyApplication(config);
            app.start();
            
        } catch (Exception e) {
            System.err.println("Error starting Loopy: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}