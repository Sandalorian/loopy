package com.neo4j.loopy.commands;

import com.neo4j.loopy.LoopyConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Validate command - Validate configuration without running
 */
@Command(name = "validate-config", 
         description = "Validate configuration file without running the load test",
         mixinStandardHelpOptions = true)
public class ValidateCommand implements Callable<Integer> {
    
    @Parameters(index = "0", 
                description = "Configuration file to validate", 
                defaultValue = "src/main/resources/config.properties")
    private File configFile;
    
    @Override
    public Integer call() throws Exception {
        try {
            System.out.println("\u001B[36mValidating configuration: " + configFile.getPath() + "\u001B[0m");
            
            // Check if file exists
            if (!configFile.exists()) {
                System.err.println("\u001B[31mError: Configuration file not found: " + configFile.getPath() + "\u001B[0m");
                return 1;
            }
            
            // Load and validate configuration
            LoopyConfig config = new LoopyConfig(configFile.getPath());
            
            // Validate configuration values
            validateConfiguration(config);
            
            System.out.println("\u001B[32m\u2713 Configuration is valid\u001B[0m");
            
            // Print configuration summary
            printConfigSummary(config);
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("\u001B[31mValidation failed: " + e.getMessage() + "\u001B[0m");
            return 1;
        }
    }
    
    private void validateConfiguration(LoopyConfig config) {
        if (config.getThreads() < 1 || config.getThreads() > 100) {
            throw new IllegalArgumentException("Invalid threads value: " + config.getThreads() + " (must be 1-100)");
        }
        
        if (config.getDurationSeconds() < 1) {
            throw new IllegalArgumentException("Invalid duration: " + config.getDurationSeconds() + " (must be >= 1)");
        }
        
        if (config.getWriteRatio() < 0.0 || config.getWriteRatio() > 1.0) {
            throw new IllegalArgumentException("Invalid write ratio: " + config.getWriteRatio() + " (must be 0.0-1.0)");
        }
        
        if (config.getBatchSize() < 1) {
            throw new IllegalArgumentException("Invalid batch size: " + config.getBatchSize() + " (must be >= 1)");
        }
        
        if (!isValidNeo4jUri(config.getNeo4jUri())) {
            throw new IllegalArgumentException("Invalid Neo4j URI format: " + config.getNeo4jUri());
        }
    }
    
    private boolean isValidNeo4jUri(String uri) {
        return uri.matches("^(bolt|neo4j|bolt\\+s|neo4j\\+s)://[^\\s]+");
    }
    
    private void printConfigSummary(LoopyConfig config) {
        System.out.println("\nConfiguration Summary:");
        System.out.println("  Neo4j URI: " + config.getNeo4jUri());
        System.out.println("  Username: " + config.getNeo4jUsername());
        System.out.println("  Threads: " + config.getThreads());
        System.out.println("  Duration: " + config.getDurationSeconds() + " seconds");
        System.out.println("  Write Ratio: " + (config.getWriteRatio() * 100) + "%");
        System.out.println("  Batch Size: " + config.getBatchSize());
        System.out.println("  Node Labels: " + config.getNodeLabels());
        System.out.println("  Relationship Types: " + config.getRelationshipTypes());
        System.out.println("  CSV Logging: " + (config.isCsvLoggingEnabled() ? "Enabled" : "Disabled"));
    }
}