package com.neo4j.loopy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Configuration management for Loopy application
 */
public class LoopyConfig {
    private Properties properties;
    
    // Neo4j Connection
    private String neo4jUri;
    private String neo4jUsername;
    private String neo4jPassword;
    
    // Load Parameters
    private int threads;
    private int durationSeconds;
    private double writeRatio;
    private int batchSize;
    
    // Data Generation
    private List<String> nodeLabels;
    private List<String> relationshipTypes;
    private int propertySizeBytes;
    
    // Reporting
    private int reportIntervalSeconds;
    private boolean csvLoggingEnabled;
    private String csvLoggingFile;
    
    public LoopyConfig() {
        loadDefaultConfig();
    }
    
    public LoopyConfig(String configFile) {
        loadDefaultConfig();
        loadConfigFromFile(configFile);
    }
    
    private void loadDefaultConfig() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load default config: " + e.getMessage());
        }
        parseProperties();
    }
    
    private void loadConfigFromFile(String configFile) {
        Properties fileProps = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (input != null) {
                fileProps.load(input);
                properties.putAll(fileProps);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load config file " + configFile + ": " + e.getMessage());
        }
        parseProperties();
    }
    
    public void overrideFromArgs(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    properties.setProperty(parts[0].replace("-", "."), parts[1]);
                }
            }
        }
        parseProperties();
    }
    
    private void parseProperties() {
        neo4jUri = properties.getProperty("neo4j.uri", "bolt://localhost:7687");
        neo4jUsername = properties.getProperty("neo4j.username", "neo4j");
        neo4jPassword = properties.getProperty("neo4j.password", "password");
        
        threads = Integer.parseInt(properties.getProperty("threads", "4"));
        durationSeconds = Integer.parseInt(properties.getProperty("duration.seconds", "300"));
        writeRatio = Double.parseDouble(properties.getProperty("write.ratio", "0.7"));
        batchSize = Integer.parseInt(properties.getProperty("batch.size", "100"));
        
        nodeLabels = Arrays.asList(properties.getProperty("node.labels", "Person,Product,Order").split(","));
        relationshipTypes = Arrays.asList(properties.getProperty("relationship.types", "KNOWS,PURCHASED,CONTAINS").split(","));
        propertySizeBytes = Integer.parseInt(properties.getProperty("property.size.bytes", "1024"));
        
        reportIntervalSeconds = Integer.parseInt(properties.getProperty("report.interval.seconds", "10"));
        csvLoggingEnabled = Boolean.parseBoolean(properties.getProperty("csv.logging.enabled", "false"));
        csvLoggingFile = properties.getProperty("csv.logging.file", "loopy-stats.csv");
    }
    
    // Getters
    public String getNeo4jUri() { return neo4jUri; }
    public String getNeo4jUsername() { return neo4jUsername; }
    public String getNeo4jPassword() { return neo4jPassword; }
    public int getThreads() { return threads; }
    public int getDurationSeconds() { return durationSeconds; }
    public double getWriteRatio() { return writeRatio; }
    public int getBatchSize() { return batchSize; }
    public List<String> getNodeLabels() { return nodeLabels; }
    public List<String> getRelationshipTypes() { return relationshipTypes; }
    public int getPropertySizeBytes() { return propertySizeBytes; }
    public int getReportIntervalSeconds() { return reportIntervalSeconds; }
    public boolean isCsvLoggingEnabled() { return csvLoggingEnabled; }
    public String getCsvLoggingFile() { return csvLoggingFile; }
}