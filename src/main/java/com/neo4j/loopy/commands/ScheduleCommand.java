package com.neo4j.loopy.commands;

import com.neo4j.loopy.LoopyConfig;
import com.neo4j.loopy.enterprise.ScheduledRunner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.Scanner;

/**
 * Enterprise command for scheduling load tests
 */
@Command(name = "schedule", 
         description = "Schedule load tests for enterprise environments",
         mixinStandardHelpOptions = true)
public class ScheduleCommand implements Callable<Integer> {
    
    @Option(names = {"--config", "-c"}, 
            description = "Configuration file to use for scheduled tests")
    private File configFile;
    
    @Option(names = {"--cron"}, 
            description = "Cron expression for scheduling (minute hour day month dayOfWeek)")
    private String cronExpression;
    
    @Option(names = {"--delay"}, 
            description = "Delay in seconds for one-time execution")
    private Long delaySeconds;
    
    @Option(names = {"--max-runs"}, 
            description = "Maximum number of runs (0 for unlimited)",
            defaultValue = "0")
    private int maxRuns;
    
    @Option(names = {"--daemon"}, 
            description = "Run as daemon process (keeps running in background)")
    private boolean daemon;
    
    @Option(names = {"--list"}, 
            description = "List all active scheduled tasks")
    private boolean listTasks;
    
    private static ScheduledRunner scheduledRunner;
    
    @Override
    public Integer call() throws Exception {
        if (listTasks) {
            listActiveTasks();
            return 0;
        }
        
        // Load configuration
        LoopyConfig config = loadConfiguration();
        
        // Initialize scheduler if needed
        if (scheduledRunner == null) {
            scheduledRunner = new ScheduledRunner();
        }
        
        ScheduledRunner.ScheduledTask task = null;
        
        if (cronExpression != null) {
            // Schedule with cron expression
            task = scheduledRunner.scheduleLoadTest(config, cronExpression, maxRuns);
        } else if (delaySeconds != null) {
            // Schedule one-time with delay
            task = scheduledRunner.scheduleOnetime(config, delaySeconds);
        } else {
            System.err.println("\u001B[31m❌ Either --cron or --delay must be specified\u001B[0m");
            return 1;
        }
        
        if (daemon) {
            runAsDaemon(task);
        } else {
            runInteractive(task);
        }
        
        return 0;
    }
    
    private LoopyConfig loadConfiguration() {
        if (configFile != null) {
            return new LoopyConfig(configFile.getAbsolutePath());
        } else {
            // Use default configuration
            return new LoopyConfig();
        }
    }
    
    private void listActiveTasks() {
        System.out.println("\u001B[36m=== Active Scheduled Tasks ===\u001B[0m");
        System.out.println("(This would list active tasks in a full implementation)");
        System.out.println("\u001B[33mTip:\u001B[0m Use 'ps' or 'jobs' to see running background processes");
    }
    
    private void runAsDaemon(ScheduledRunner.ScheduledTask task) {
        System.out.println("\u001B[36m📅 Running scheduled task as daemon...\u001B[0m");
        System.out.println("Press Ctrl+C to stop the scheduler");
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🔌 Shutting down scheduler...");
            task.cancel();
            scheduledRunner.shutdown();
        }));
        
        // Keep the process alive
        try {
            while (task.isRunning()) {
                Thread.sleep(5000); // Check every 5 seconds
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            task.cancel();
        }
        
        scheduledRunner.shutdown();
        System.out.println("📅 Scheduler daemon stopped");
    }
    
    private void runInteractive(ScheduledRunner.ScheduledTask task) {
        System.out.println("\u001B[36m📅 Scheduled task created. Interactive mode:\u001B[0m");
        System.out.println("Commands:");
        System.out.println("  status  - Show task status");
        System.out.println("  cancel  - Cancel the scheduled task");
        System.out.println("  quit    - Exit (task continues in background)");
        System.out.println();
        
        Scanner scanner = new Scanner(System.in);
        
        while (task.isRunning()) {
            System.out.print("schedule> ");
            String command = scanner.nextLine().trim().toLowerCase();
            
            switch (command) {
                case "status":
                    showTaskStatus(task);
                    break;
                case "cancel":
                    task.cancel();
                    System.out.println("✅ Task cancelled");
                    scheduledRunner.shutdown();
                    return;
                case "quit":
                case "exit":
                    System.out.println("🚀 Task continues running in background");
                    return;
                case "help":
                    System.out.println("Available commands: status, cancel, quit");
                    break;
                default:
                    System.out.println("Unknown command. Type 'help' for available commands.");
                    break;
            }
        }
        
        System.out.println("📅 Scheduled task completed");
        scheduledRunner.shutdown();
    }
    
    private void showTaskStatus(ScheduledRunner.ScheduledTask task) {
        System.out.println("\u001B[36m📊 Task Status:\u001B[0m");
        System.out.printf("  Running: %s%n", task.isRunning() ? "✅ Yes" : "❌ No");
        System.out.printf("  Runs completed: %d%n", task.getRunCount());
        if (task.getMaxRuns() > 0) {
            System.out.printf("  Max runs: %d%n", task.getMaxRuns());
            System.out.printf("  Remaining: %d%n", task.getMaxRuns() - task.getRunCount());
        } else {
            System.out.printf("  Max runs: unlimited%n");
        }
    }
}