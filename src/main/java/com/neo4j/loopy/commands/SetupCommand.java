package com.neo4j.loopy.commands;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Console;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Interactive setup command - Guide users through Loopy configuration
 */
@Command(name = "setup",
         description = "Interactive setup wizard for Loopy configuration",
         mixinStandardHelpOptions = true)
public class SetupCommand implements Callable<Integer> {
    
    @Option(names = {"--output", "-o"}, 
            description = "Output configuration file path",
            defaultValue = "config.properties")
    private String outputPath;
    
    @Option(names = {"--skip-test"}, 
            description = "Skip connection testing during setup")
    private boolean skipTest;
    
    private Scanner scanner;
    private Console console;
    
    @Override
    public Integer call() throws Exception {
        scanner = new Scanner(System.in);
        console = System.console();
        
        System.out.println("\u001B[36m" + 
            "╔══════════════════════════════════════════════════════════════╗\n" +
            "║               Loopy Interactive Setup Wizard                 ║\n" +
            "║          Neo4j Load Testing Configuration Generator          ║\n" +
            "╚══════════════════════════════════════════════════════════════╝" +
            "\u001B[0m\n");
            
        System.out.println("This wizard will guide you through creating a Loopy configuration.");
        System.out.println("Press Enter to accept default values shown in [brackets].\n");
        
        try {
            // Collect configuration
            Properties config = collectConfiguration();
            
            // Test connection if requested
            if (!skipTest && confirmConnectionTest()) {
                if (!testConnection(config)) {
                    System.out.println("\n\u001B[33mConnection test failed. Continue anyway? (y/N): \u001B[0m");
                    String answer = scanner.nextLine().trim().toLowerCase();
                    if (!answer.equals("y") && !answer.equals("yes")) {
                        System.out.println("\u001B[31mSetup cancelled.\u001B[0m");
                        return 1;
                    }
                }
            }
            
            // Save configuration
            if (saveConfiguration(config)) {
                printSetupSummary(config);
                return 0;
            } else {
                return 1;
            }
            
        } catch (Exception e) {
            System.err.println("\u001B[31mSetup failed: " + e.getMessage() + "\u001B[0m");
            return 1;
        }
    }
    
    private Properties collectConfiguration() {
        System.out.println("\u001B[36m=== Neo4j Connection Settings ===\u001B[0m");
        
        String uri = promptWithDefault("Neo4j URI", "bolt://localhost:7687");
        String username = promptWithDefault("Username", "neo4j");
        String password = promptPassword("Password");
        
        System.out.println("\n\u001B[36m=== Load Testing Parameters ===\u001B[0m");
        
        int threads = Integer.parseInt(promptWithDefault("Number of threads", "4"));
        int duration = Integer.parseInt(promptWithDefault("Duration (seconds)", "300"));
        double writeRatio = Double.parseDouble(promptWithDefault("Write ratio (0.0-1.0)", "0.7"));
        int batchSize = Integer.parseInt(promptWithDefault("Batch size", "100"));
        
        System.out.println("\n\u001B[36m=== Data Configuration ===\u001B[0m");
        
        String nodeLabels = promptWithDefault("Node labels (comma-separated)", "Person,Company");
        String relTypes = promptWithDefault("Relationship types (comma-separated)", "WORKS_FOR,KNOWS");
        int propertySize = Integer.parseInt(promptWithDefault("Property size (bytes)", "100"));
        
        System.out.println("\n\u001B[36m=== Reporting Configuration ===\u001B[0m");
        
        int reportInterval = Integer.parseInt(promptWithDefault("Report interval (seconds)", "10"));
        boolean csvLogging = promptYesNo("Enable CSV logging", true);
        String csvFile = csvLogging ? promptWithDefault("CSV file path", "loopy-results.csv") : "";
        
        // Create configuration object for validation only
        Properties configProps = new Properties();
        configProps.setProperty("neo4j.uri", uri);
        configProps.setProperty("neo4j.username", username);
        configProps.setProperty("neo4j.password", password);
        configProps.setProperty("threads", String.valueOf(threads));
        configProps.setProperty("duration.seconds", String.valueOf(duration));
        configProps.setProperty("write.ratio", String.valueOf(writeRatio));
        configProps.setProperty("batch.size", String.valueOf(batchSize));
        configProps.setProperty("node.labels", nodeLabels);
        configProps.setProperty("relationship.types", relTypes);
        configProps.setProperty("property.size.bytes", String.valueOf(propertySize));
        configProps.setProperty("report.interval.seconds", String.valueOf(reportInterval));
        configProps.setProperty("csv.logging.enabled", String.valueOf(csvLogging));
        configProps.setProperty("csv.logging.file", csvFile);
        
        return configProps;
    }
    
    private String promptWithDefault(String prompt, String defaultValue) {
        System.out.print(prompt + " [" + defaultValue + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }
    
    private String promptPassword(String prompt) {
        if (console != null) {
            char[] passwordChars = console.readPassword(prompt + ": ");
            return new String(passwordChars);
        } else {
            // Fallback for non-console environments
            System.out.print(prompt + ": ");
            return scanner.nextLine();
        }
    }
    
    private boolean promptYesNo(String prompt, boolean defaultValue) {
        String defaultStr = defaultValue ? "Y/n" : "y/N";
        System.out.print(prompt + " [" + defaultStr + "]: ");
        String input = scanner.nextLine().trim().toLowerCase();
        
        if (input.isEmpty()) {
            return defaultValue;
        }
        
        return input.equals("y") || input.equals("yes");
    }
    
    private boolean confirmConnectionTest() {
        return promptYesNo("Test Neo4j connection now", true);
    }
    
    private boolean testConnection(Properties config) {
        System.out.println("\n\u001B[36m=== Testing Neo4j Connection ===\u001B[0m");
        
        try (Driver driver = GraphDatabase.driver(config.getProperty("neo4j.uri"), 
                AuthTokens.basic(config.getProperty("neo4j.username"), config.getProperty("neo4j.password")))) {
            
            System.out.print("  • Testing connectivity... ");
            driver.verifyConnectivity();
            System.out.println("\u001B[32m✓\u001B[0m");
            
            try (Session session = driver.session()) {
                System.out.print("  • Checking database version... ");
                Result result = session.run("CALL dbms.components() YIELD name, versions, edition");
                if (result.hasNext()) {
                    var record = result.next();
                    String name = record.get("name").asString();
                    String version = record.get("versions").asList().get(0).toString();
                    String edition = record.get("edition").asString();
                    System.out.println("\u001B[32m✓\u001B[0m");
                    System.out.println("    " + name + " " + version + " (" + edition + ")");
                } else {
                    System.out.println("\u001B[33m?\u001B[0m");
                }
                
                System.out.print("  • Testing read permissions... ");
                session.run("MATCH (n) RETURN count(n) LIMIT 1");
                System.out.println("\u001B[32m✓\u001B[0m");
                
                System.out.print("  • Testing write permissions... ");
                session.run("CREATE (test:LoopySetupTest {timestamp: timestamp()}) DELETE test");
                System.out.println("\u001B[32m✓\u001B[0m");
                
                System.out.println("\n\u001B[32m✓ Connection test successful!\u001B[0m");
                return true;
            }
            
        } catch (Exception e) {
            System.out.println("\u001B[31m✗\u001B[0m");
            System.err.println("Connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean saveConfiguration(Properties config) {
        try {
            File outputFile = new File(outputPath);
            
            // Create directory if it doesn't exist
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // Save configuration
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                writer.write("# Loopy Configuration File\n");
                writer.write("# Generated by Setup Wizard on " + java.time.LocalDateTime.now() + "\n\n");
                
                writer.write("# Neo4j Connection\n");
                writer.write("neo4j.uri=" + config.getProperty("neo4j.uri") + "\n");
                writer.write("neo4j.username=" + config.getProperty("neo4j.username") + "\n");
                writer.write("neo4j.password=" + config.getProperty("neo4j.password") + "\n\n");
                
                writer.write("# Load Testing Parameters\n");
                writer.write("threads=" + config.getProperty("threads") + "\n");
                writer.write("duration.seconds=" + config.getProperty("duration.seconds") + "\n");
                writer.write("write.ratio=" + config.getProperty("write.ratio") + "\n");
                writer.write("batch.size=" + config.getProperty("batch.size") + "\n\n");
                
                writer.write("# Data Configuration\n");
                writer.write("node.labels=" + config.getProperty("node.labels") + "\n");
                writer.write("relationship.types=" + config.getProperty("relationship.types") + "\n");
                writer.write("property.size.bytes=" + config.getProperty("property.size.bytes") + "\n\n");
                
                writer.write("# Reporting\n");
                writer.write("report.interval.seconds=" + config.getProperty("report.interval.seconds") + "\n");
                writer.write("csv.logging.enabled=" + config.getProperty("csv.logging.enabled") + "\n");
                if (Boolean.parseBoolean(config.getProperty("csv.logging.enabled"))) {
                    writer.write("csv.logging.file=" + config.getProperty("csv.logging.file") + "\n");
                }
            }
            
            System.out.println("\n\u001B[32m✓ Configuration saved to: " + outputFile.getAbsolutePath() + "\u001B[0m");
            return true;
            
        } catch (IOException e) {
            System.err.println("\u001B[31mFailed to save configuration: " + e.getMessage() + "\u001B[0m");
            return false;
        }
    }
    
    private void printSetupSummary(Properties config) {
        System.out.println("\n\u001B[36m=== Setup Complete! ===\u001B[0m");
        System.out.println("\nYour Loopy configuration:");
        System.out.println("  • Configuration file: " + outputPath);
        System.out.println("  • Neo4j URI: " + config.getProperty("neo4j.uri"));
        System.out.println("  • Load test duration: " + config.getProperty("duration.seconds") + " seconds");
        System.out.println("  • Worker threads: " + config.getProperty("threads"));
        System.out.println("  • Write ratio: " + Math.round(Double.parseDouble(config.getProperty("write.ratio")) * 100) + "%");
        
        System.out.println("\n\u001B[36mNext steps:\u001B[0m");
        System.out.println("  1. Review the configuration file: " + outputPath);
        System.out.println("  2. Run a test: java -jar loopy.jar --config " + outputPath);
        System.out.println("  3. Use benchmark profiles: java -jar loopy.jar benchmark light");
        System.out.println("  4. Validate anytime: java -jar loopy.jar validate-config " + outputPath);
        
        System.out.println("\n\u001B[32mHappy load testing! \u001B[0m🚀");
    }
}