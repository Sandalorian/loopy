package com.neo4j.loopy;

import com.neo4j.loopy.cli.CliOption;
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
    @CliOption(names = {"--neo4j-uri", "-u"}, description = "Neo4j connection URI", envVar = "LOOPY_NEO4J_URI")
    private String neo4jUri;
    
    @CliOption(names = {"--username", "-U"}, description = "Neo4j username", envVar = "LOOPY_USERNAME")
    private String neo4jUsername;
    
    @CliOption(names = {"--password", "-P"}, description = "Neo4j password", envVar = "LOOPY_PASSWORD")
    private String neo4jPassword;
    
    // Load Parameters
    @CliOption(names = {"--threads", "-t"}, description = "Number of worker threads", min = 1, max = 100, envVar = "LOOPY_THREADS")
    private int threads;
    
    @CliOption(names = {"--duration", "-d"}, description = "Test duration in seconds", min = 1, envVar = "LOOPY_DURATION")
    private int durationSeconds;
    
    @CliOption(names = {"--write-ratio", "-w"}, description = "Write operation ratio (0.0-1.0)", min = 0.0, max = 1.0)
    private double writeRatio;
    
    @CliOption(names = {"--batch-size", "-b"}, description = "Batch size for operations", min = 1)
    private int batchSize;
    
    // Data Generation
    @CliOption(names = {"--node-labels", "-n"}, description = "Comma-separated node labels")
    private List<String> nodeLabels;
    
    @CliOption(names = {"--relationship-types", "-r"}, description = "Comma-separated relationship types")
    private List<String> relationshipTypes;
    
    @CliOption(names = {"--property-size"}, description = "Property size in bytes", min = 1)
    private int propertySizeBytes;
    
    // Reporting
    @CliOption(names = {"--report-interval"}, description = "Statistics reporting interval in seconds", min = 1)
    private int reportIntervalSeconds;
    
    @CliOption(names = {"--csv-logging"}, description = "Enable CSV logging")
    private boolean csvLoggingEnabled;
    
    @CliOption(names = {"--csv-file"}, description = "CSV output file path")
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
    
    /**
     * Validate the configuration values
     * @throws IllegalArgumentException if any configuration value is invalid
     */
    public void validate() {
        if (threads < 1 || threads > 100) {
            throw new IllegalArgumentException("Threads must be between 1 and 100, got: " + threads);
        }
        
        if (durationSeconds < 1) {
            throw new IllegalArgumentException("Duration must be at least 1 second, got: " + durationSeconds);
        }
        
        if (writeRatio < 0.0 || writeRatio > 1.0) {
            throw new IllegalArgumentException("Write ratio must be between 0.0 and 1.0, got: " + writeRatio);
        }
        
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be at least 1, got: " + batchSize);
        }
        
        if (propertySizeBytes < 1) {
            throw new IllegalArgumentException("Property size must be at least 1 byte, got: " + propertySizeBytes);
        }
        
        if (reportIntervalSeconds < 1) {
            throw new IllegalArgumentException("Report interval must be at least 1 second, got: " + reportIntervalSeconds);
        }
        
        if (!isValidNeo4jUri(neo4jUri)) {
            throw new IllegalArgumentException("Invalid Neo4j URI format: " + neo4jUri);
        }
    }
    
    private boolean isValidNeo4jUri(String uri) {
        return uri != null && uri.matches("^(bolt|neo4j|bolt\\+s|neo4j\\+s)://[^\\s]+");
    }
    
    /**
     * Export current configuration to properties format
     * @return Properties object with current configuration
     */
    public Properties exportToProperties() {
        Properties props = new Properties();
        props.setProperty("neo4j.uri", neo4jUri);
        props.setProperty("neo4j.username", neo4jUsername);
        props.setProperty("neo4j.password", neo4jPassword);
        props.setProperty("threads", String.valueOf(threads));
        props.setProperty("duration.seconds", String.valueOf(durationSeconds));
        props.setProperty("write.ratio", String.valueOf(writeRatio));
        props.setProperty("batch.size", String.valueOf(batchSize));
        props.setProperty("node.labels", String.join(",", nodeLabels));
        props.setProperty("relationship.types", String.join(",", relationshipTypes));
        props.setProperty("property.size.bytes", String.valueOf(propertySizeBytes));
        props.setProperty("report.interval.seconds", String.valueOf(reportIntervalSeconds));
        props.setProperty("csv.logging.enabled", String.valueOf(csvLoggingEnabled));
        props.setProperty("csv.logging.file", csvLoggingFile);
        return props;
    }
}