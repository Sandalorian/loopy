package com.neo4j.loopy;

import com.neo4j.loopy.config.CypherWorkloadConfig;
import com.neo4j.loopy.config.CypherWorkloadValidator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Main application class for Loopy - Neo4j load generator
 */
@Command(
    name = "loopy",
    description = "Neo4j load generator for testing database performance",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    subcommands = {
        com.neo4j.loopy.commands.RunCommand.class,
        com.neo4j.loopy.commands.ValidateCommand.class,
        com.neo4j.loopy.commands.BenchmarkCommand.class,
        com.neo4j.loopy.commands.TestConnectionCommand.class,
        com.neo4j.loopy.commands.SetupCommand.class,
        com.neo4j.loopy.commands.ConfigCommand.class,
        com.neo4j.loopy.commands.ReportCommand.class,
        com.neo4j.loopy.commands.SecurityCommand.class
    }
)
public class LoopyApplication implements Callable<Integer> {
    
    @Option(names = {"--config", "-c"}, description = "Configuration file path")
    private String configFile;
    
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
    
    @Option(names = {"--threads", "-t"}, 
            description = "Number of worker threads (1-100)",
            defaultValue = "${LOOPY_THREADS:-4}")
    private Integer threads;
    
    @Option(names = {"--duration", "-d"}, 
            description = "Test duration in seconds (minimum 1)",
            defaultValue = "${LOOPY_DURATION:-300}")
    private Integer duration;
    
    @Option(names = {"--write-ratio", "-w"}, 
            description = "Write operation ratio (0.0-1.0)",
            defaultValue = "0.7")
    private Double writeRatio;
    
    @Option(names = {"--batch-size", "-b"}, 
            description = "Batch size for operations (minimum 1)",
            defaultValue = "100")
    private Integer batchSize;
    
    @Option(names = {"--node-labels", "-n"}, description = "Comma-separated node labels")
    private String nodeLabels;
    
    @Option(names = {"--relationship-types", "-r"}, description = "Comma-separated relationship types")
    private String relationshipTypes;
    
    @Option(names = {"--property-size"}, 
            description = "Property size in bytes (minimum 1)",
            defaultValue = "1024")
    private Integer propertySize;
    
    @Option(names = {"--report-interval"}, 
            description = "Statistics reporting interval in seconds (minimum 1)",
            defaultValue = "10")
    private Integer reportInterval;
    
    @Option(names = {"--csv-logging"}, description = "Enable CSV logging")
    private Boolean csvLogging;
    
    @Option(names = {"--csv-file"}, description = "CSV output file path")
    private String csvFile;
    
    @Option(names = {"--quiet", "-q"}, description = "Quiet mode - minimal output")
    private boolean quiet = false;
    
    @Option(names = {"--verbose", "-v"}, description = "Verbose mode - detailed output")
    private boolean verbose = false;
    
    // YAML-based Cypher workload options
    @Option(names = {"--cypher-file", "-f"}, 
            description = "Path to YAML workload file containing Cypher queries")
    private String cypherFile;
    
    @Option(names = {"--verbose-stats"}, 
            description = "Enable per-query statistics (default: aggregated only)")
    private boolean verboseStats = false;
    
    @Option(names = {"--dry-run"}, 
            description = "Validate YAML and test connection without executing workload")
    private boolean dryRun = false;
    
    @Option(names = {"--fail-fast"}, 
            description = "Abort on first query failure (default: continue with next query)")
    private boolean failFast = false;
    
    @Option(names = {"--stats-format"}, 
            description = "Statistics output format: summary, detailed, json",
            defaultValue = "summary")
    private String statsFormat;
    
    // Application state
    private LoopyConfig config;
    private CypherWorkloadConfig workloadConfig;
    private LoopyStats stats;
    private ExecutorService executorService;
    private ScheduledExecutorService reportingService;
    private List<Worker> workers;
    private volatile boolean running = false;
    
    private void validateParameters() {
        List<String> errors = new ArrayList<>();
        
        // Validate mutual exclusivity: --cypher-file vs --node-labels/--relationship-types
        if (cypherFile != null) {
            if (nodeLabels != null) {
                errors.add("--cypher-file is mutually exclusive with --node-labels");
            }
            if (relationshipTypes != null) {
                errors.add("--cypher-file is mutually exclusive with --relationship-types");
            }
            // Warn about ignored --write-ratio when using --cypher-file
            if (writeRatio != null && writeRatio != 0.7) {
                System.out.println("\u001B[33mWarning: --write-ratio is ignored when using --cypher-file (queries have explicit types)\u001B[0m");
            }
        }
        
        // Validate stats-format
        if (statsFormat != null && !statsFormat.matches("^(summary|detailed|json)$")) {
            errors.add("Invalid stats-format: " + statsFormat + ". Expected: summary, detailed, or json");
        }
        
        // Validate threads
        if (threads != null && (threads < 1 || threads > 100)) {
            errors.add("Threads must be between 1 and 100, got: " + threads);
        }
        
        // Validate duration
        if (duration != null && duration < 1) {
            errors.add("Duration must be at least 1 second, got: " + duration);
        }
        
        // Validate write ratio
        if (writeRatio != null && (writeRatio < 0.0 || writeRatio > 1.0)) {
            errors.add("Write ratio must be between 0.0 and 1.0, got: " + writeRatio);
        }
        
        // Validate batch size
        if (batchSize != null && batchSize < 1) {
            errors.add("Batch size must be at least 1, got: " + batchSize);
        }
        
        // Validate property size
        if (propertySize != null && propertySize < 1) {
            errors.add("Property size must be at least 1 byte, got: " + propertySize);
        }
        
        // Validate report interval
        if (reportInterval != null && reportInterval < 1) {
            errors.add("Report interval must be at least 1 second, got: " + reportInterval);
        }
        
        // Validate Neo4j URI format
        if (neo4jUri != null && !isValidNeo4jUri(neo4jUri)) {
            errors.add("Invalid Neo4j URI format. Expected format: bolt://host:port or neo4j://host:port");
        }
        
        if (!errors.isEmpty()) {
            System.err.println("\u001B[31mValidation Errors:\u001B[0m");
            for (String error : errors) {
                System.err.println("  \u2022 " + error);
            }
            throw new IllegalArgumentException("Invalid parameters provided");
        }
    }
    
    /**
     * Validate and load YAML workload configuration
     */
    private boolean validateAndLoadWorkload() {
        if (cypherFile == null) {
            return true; // No workload file specified, use default mode
        }
        
        if (!quiet) {
            System.out.println("\u001B[36mValidating YAML workload file: " + cypherFile + "\u001B[0m");
        }
        
        CypherWorkloadValidator validator = new CypherWorkloadValidator();
        CypherWorkloadValidator.ValidationResult result = validator.validate(
            cypherFile, neo4jUri, username, password
        );
        
        // Print warnings
        if (!result.getWarnings().isEmpty() && !quiet) {
            System.out.println("\u001B[33mWarnings:\u001B[0m");
            result.printWarnings();
        }
        
        // Check for errors
        if (!result.isValid()) {
            System.err.println("\u001B[31mWorkload validation failed:\u001B[0m");
            result.printErrors();
            return false;
        }
        
        this.workloadConfig = result.getConfig();
        
        if (!quiet) {
            System.out.println("\u001B[32mWorkload validated successfully\u001B[0m");
            if (verbose) {
                System.out.println("  Name: " + workloadConfig.getName());
                System.out.println("  Description: " + workloadConfig.getDescription());
                System.out.println("  Queries: " + workloadConfig.getQueries().size());
                System.out.println("  Total Weight: " + workloadConfig.getTotalWeight());
            }
        }
        
        return true;
    }
    
    private boolean isValidNeo4jUri(String uri) {
        return uri.matches("^(bolt|neo4j|bolt\\+s|neo4j\\+s)://[^\\s]+");
    }
    
    @Override
    public Integer call() throws Exception {
        try {
            // Initialize ANSI colors
            org.fusesource.jansi.AnsiConsole.systemInstall();
            
            // Validate parameters first
            validateParameters();
            
            // Validate and load YAML workload if specified
            if (!validateAndLoadWorkload()) {
                return 1;
            }
            
            // Handle dry-run mode
            if (dryRun) {
                if (!quiet) {
                    System.out.println("\u001B[32mDry run completed successfully. No queries were executed.\u001B[0m");
                }
                return 0;
            }
            
            if (verbose) {
                System.out.println("\u001B[36mVerbose mode enabled\u001B[0m");
            }
            
            // Initialize configuration
            initializeConfig();
            
            // Initialize application components
            this.stats = new LoopyStats(config);
            this.executorService = Executors.newFixedThreadPool(config.getThreads());
            this.reportingService = Executors.newScheduledThreadPool(1);
            this.workers = new ArrayList<>();
            
            // Start the application
            start();
            
            return 0;
            
        } catch (IllegalArgumentException e) {
            if (!quiet) {
                System.err.println("\u001B[31mError: " + e.getMessage() + "\u001B[0m");
            }
            return 1;
        } catch (Exception e) {
            if (!quiet) {
                System.err.println("\u001B[31mUnexpected error: " + e.getMessage() + "\u001B[0m");
                if (verbose) {
                    e.printStackTrace();
                }
            }
            return 1;
        } finally {
            org.fusesource.jansi.AnsiConsole.systemUninstall();
        }
    }
    
    private void initializeConfig() {
        // Start with file-based config if specified
        if (configFile != null) {
            this.config = new LoopyConfig(configFile);
        } else {
            this.config = new LoopyConfig();
        }
        
        // Override with CLI parameters
        overrideConfigWithCliOptions();
    }
    
    private void overrideConfigWithCliOptions() {
        // Build argument array for backward compatibility
        List<String> args = new ArrayList<>();
        
        if (neo4jUri != null) args.addAll(List.of("--neo4j.uri=" + neo4jUri));
        if (username != null) args.addAll(List.of("--neo4j.username=" + username));
        if (password != null) args.addAll(List.of("--neo4j.password=" + password));
        if (threads != null) args.addAll(List.of("--threads=" + threads));
        if (duration != null) args.addAll(List.of("--duration.seconds=" + duration));
        if (writeRatio != null) args.addAll(List.of("--write.ratio=" + writeRatio));
        if (batchSize != null) args.addAll(List.of("--batch.size=" + batchSize));
        if (nodeLabels != null) args.addAll(List.of("--node.labels=" + nodeLabels));
        if (relationshipTypes != null) args.addAll(List.of("--relationship.types=" + relationshipTypes));
        if (propertySize != null) args.addAll(List.of("--property.size.bytes=" + propertySize));
        if (reportInterval != null) args.addAll(List.of("--report.interval.seconds=" + reportInterval));
        if (csvLogging != null) args.addAll(List.of("--csv.logging.enabled=" + csvLogging));
        if (csvFile != null) args.addAll(List.of("--csv.logging.file=" + csvFile));
        
        // Use existing override mechanism
        config.overrideFromArgs(args.toArray(new String[0]));
    }
    
    /**
     * Create a worker based on the current configuration mode.
     * Uses factory pattern to select appropriate worker implementation.
     */
    private Worker createWorker() {
        if (workloadConfig != null) {
            // YAML-based Cypher workload mode
            return new CypherFileWorker(config, stats, workloadConfig, failFast);
        } else {
            // Default programmatic data generation mode
            return new LoopyWorker(config, stats);
        }
    }
    
    public void start() {
        if (!quiet) {
            System.out.println("\u001B[36mStarting Loopy load generator...\u001B[0m");
            System.out.println("Configuration:");
            System.out.println("  Neo4j URI: " + config.getNeo4jUri());
            System.out.println("  Threads: " + config.getThreads());
            System.out.println("  Duration: " + config.getDurationSeconds() + " seconds");
            
            // Show appropriate mode information
            if (workloadConfig != null) {
                System.out.println("  Mode: YAML Cypher Workload");
                System.out.println("  Workload: " + workloadConfig.getName());
                System.out.println("  Queries: " + workloadConfig.getQueries().size());
                if (verbose) {
                    System.out.println("  Description: " + workloadConfig.getDescription());
                    System.out.println("  Verbose Stats: " + verboseStats);
                    System.out.println("  Fail Fast: " + failFast);
                    System.out.println("  Stats Format: " + statsFormat);
                }
            } else {
                System.out.println("  Mode: Programmatic Data Generation");
                System.out.println("  Write Ratio: " + (config.getWriteRatio() * 100) + "%");
                if (verbose) {
                    System.out.println("  Node Labels: " + config.getNodeLabels());
                    System.out.println("  Relationship Types: " + config.getRelationshipTypes());
                    System.out.println("  Batch Size: " + config.getBatchSize());
                    System.out.println("  Property Size: " + config.getPropertySizeBytes() + " bytes");
                }
            }
            
            if (verbose) {
                System.out.println("  Report Interval: " + config.getReportIntervalSeconds() + " seconds");
                System.out.println("  CSV Logging: " + (config.isCsvLoggingEnabled() ? "Enabled" : "Disabled"));
            }
            System.out.println();
        }
        
        running = true;
        
        // Configure stats based on CLI options
        stats.setVerboseStats(verboseStats);
        stats.setStatsFormat(statsFormat);
        
        // Start worker threads using factory pattern
        for (int i = 0; i < config.getThreads(); i++) {
            Worker worker = createWorker();
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
        
        if (!quiet) {
            System.out.println("\n\u001B[33mShutting down Loopy...\u001B[0m");
        }
        running = false;
        
        // Stop workers (using Worker interface)
        workers.forEach(Worker::stop);
        
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
        if (!quiet) {
            stats.printFinalStats();
        }
        
        // Close CSV writer if enabled
        stats.close();
        
        if (!quiet) {
            System.out.println("\u001B[32mLoopy shutdown complete.\u001B[0m");
        }
    }
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new LoopyApplication()).execute(args);
        System.exit(exitCode);
    }
}