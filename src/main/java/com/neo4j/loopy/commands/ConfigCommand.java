package com.neo4j.loopy.commands;

import com.neo4j.loopy.config.ConfigManager;
import com.neo4j.loopy.config.ConfigManager.ValidationResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Configuration management commands
 */
@Command(name = "config",
         description = "Configuration management commands",
         mixinStandardHelpOptions = true,
         subcommands = {
             ConfigCommand.InitCommand.class,
             ConfigCommand.ValidateConfigCommand.class,
             ConfigCommand.ShowCommand.class,
             ConfigCommand.EditCommand.class
         })
public class ConfigCommand implements Callable<Integer> {
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'loopy config --help' to see available subcommands:");
        System.out.println("  init      - Generate default configuration");
        System.out.println("  validate  - Validate configuration file");
        System.out.println("  show      - Display current configuration");
        System.out.println("  edit      - Open configuration in editor");
        return 0;
    }
    
    /**
     * Initialize default configuration
     */
    @Command(name = "init",
             description = "Generate default configuration file")
    static class InitCommand implements Callable<Integer> {
        
        @Parameters(index = "0",
                    description = "Output configuration file path",
                    defaultValue = "config.properties")
        private String outputPath;
        
        @Override
        public Integer call() throws Exception {
            ConfigManager manager = new ConfigManager();
            
            System.out.println("Generating default configuration...");
            
            if (manager.generateDefaultConfig(outputPath)) {
                System.out.println("\u001B[32m✓ Default configuration generated: " + outputPath + "\u001B[0m");
                System.out.println();
                System.out.println("Next steps:");
                System.out.println("  1. Edit the configuration: loopy config edit " + outputPath);
                System.out.println("  2. Validate the configuration: loopy config validate " + outputPath);
                System.out.println("  3. Run a load test: loopy --config " + outputPath);
                return 0;
            } else {
                System.err.println("\u001B[31mFailed to generate default configuration\u001B[0m");
                return 1;
            }
        }
    }
    
    /**
     * Validate configuration file (separate from existing ValidateCommand for clarity)
     */
    @Command(name = "validate",
             description = "Validate configuration file")
    static class ValidateConfigCommand implements Callable<Integer> {
        
        @Parameters(index = "0",
                    description = "Configuration file to validate",
                    defaultValue = "config.properties")
        private String configPath;
        
        @Override
        public Integer call() throws Exception {
            ConfigManager manager = new ConfigManager();
            
            System.out.println("Validating configuration: " + configPath);
            System.out.println();
            
            ValidationResult result = manager.validateConfig(configPath);
            
            // Print results
            if (result.hasErrors()) {
                System.err.println("\u001B[31m✗ Validation failed:\u001B[0m");
                System.err.println(result.getErrors());
                System.err.println();
            }
            
            if (result.hasWarnings()) {
                System.out.println("\u001B[33m⚠ Warnings:\u001B[0m");
                System.out.println(result.getWarnings());
                System.out.println();
            }
            
            if (result.hasInfo()) {
                System.out.println("\u001B[36mℹ Information:\u001B[0m");
                System.out.println(result.getInfo());
                System.out.println();
            }
            
            if (result.isValid() && !result.hasErrors()) {
                System.out.println("\u001B[32m✓ Configuration is valid\u001B[0m");
                return 0;
            } else {
                return 1;
            }
        }
    }
    
    /**
     * Show current configuration
     */
    @Command(name = "show",
             description = "Display current effective configuration")
    static class ShowCommand implements Callable<Integer> {
        
        @Parameters(index = "0",
                    description = "Configuration file to display",
                    defaultValue = "config.properties")
        private String configPath;
        
        @Override
        public Integer call() throws Exception {
            ConfigManager manager = new ConfigManager();
            manager.showConfig(configPath);
            return 0;
        }
    }
    
    /**
     * Edit configuration in default editor
     */
    @Command(name = "edit",
             description = "Open configuration file in default editor")
    static class EditCommand implements Callable<Integer> {
        
        @Parameters(index = "0",
                    description = "Configuration file to edit",
                    defaultValue = "config.properties")
        private String configPath;
        
        @Override
        public Integer call() throws Exception {
            ConfigManager manager = new ConfigManager();
            
            if (manager.editConfig(configPath)) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}