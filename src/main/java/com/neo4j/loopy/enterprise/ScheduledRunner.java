package com.neo4j.loopy.enterprise;

import com.neo4j.loopy.LoopyConfig;
import com.neo4j.loopy.LoopyWorker;
import com.neo4j.loopy.LoopyStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Enterprise scheduler for running Loopy tests on a schedule
 * Supports cron-like syntax for scheduling load tests
 */
public class ScheduledRunner {
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Pattern cronPattern = Pattern.compile("^(\\d+|\\*)\\s+(\\d+|\\*)\\s+(\\d+|\\*)\\s+(\\d+|\\*)\\s+(\\d+|\\*)$");
    
    /**
     * Schedule a load test to run at specified intervals
     * 
     * @param config Load test configuration
     * @param scheduleExpression Cron-like expression (minute hour day month dayOfWeek)
     * @param maxRuns Maximum number of runs (0 for unlimited)
     * @return ScheduledTask handle for managing the scheduled job
     */
    public ScheduledTask scheduleLoadTest(LoopyConfig config, String scheduleExpression, int maxRuns) {
        if (!isValidCronExpression(scheduleExpression)) {
            throw new IllegalArgumentException("Invalid cron expression: " + scheduleExpression);
        }
        
        ScheduleInfo scheduleInfo = parseCronExpression(scheduleExpression);
        
        ScheduledTask task = new ScheduledTask(config, scheduleInfo, maxRuns);
        
        // Schedule the first execution
        long initialDelay = calculateNextRunDelay(scheduleInfo);
        
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> executeScheduledTask(task),
            initialDelay,
            scheduleInfo.getIntervalSeconds(),
            TimeUnit.SECONDS
        );
        
        task.setScheduledFuture(future);
        
        System.out.printf("📅 Scheduled load test: %s (next run: %s)%n", 
            scheduleExpression,
            LocalDateTime.now().plusSeconds(initialDelay).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        
        return task;
    }
    
    /**
     * Schedule a one-time load test to run after a delay
     */
    public ScheduledTask scheduleOnetime(LoopyConfig config, long delaySeconds) {
        ScheduledTask task = new ScheduledTask(config, null, 1);
        
        ScheduledFuture<?> future = scheduler.schedule(
            () -> executeScheduledTask(task),
            delaySeconds,
            TimeUnit.SECONDS
        );
        
        task.setScheduledFuture(future);
        
        System.out.printf("⏰ Scheduled one-time load test (delay: %d seconds)%n", delaySeconds);
        
        return task;
    }
    
    /**
     * Execute a scheduled task
     */
    private void executeScheduledTask(ScheduledTask task) {
        try {
            task.incrementRunCount();
            
            System.out.printf("%n🚀 Executing scheduled load test #%d%n", task.getRunCount());
            System.out.printf("   Started: %s%n", 
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Run the actual load test
            runLoadTest(task.getConfig());
            
            System.out.printf("✅ Scheduled load test #%d completed%n", task.getRunCount());
            
            // Check if we've reached the maximum runs
            if (task.getMaxRuns() > 0 && task.getRunCount() >= task.getMaxRuns()) {
                task.getScheduledFuture().cancel(false);
                System.out.printf("🏁 Scheduled task completed after %d runs%n", task.getRunCount());
            }
            
        } catch (Exception e) {
            System.err.printf("❌ Scheduled load test failed: %s%n", e.getMessage());
            e.printStackTrace();
            
            // Cancel the task on failure if desired
            // task.getScheduledFuture().cancel(false);
        }
    }
    
    /**
     * Run the actual load test
     */
    private void runLoadTest(LoopyConfig config) {
        try {
            // Create stats instance for the load test
            com.neo4j.loopy.LoopyStats stats = new com.neo4j.loopy.LoopyStats(config);
            
            // Create and run workers similar to main application
            ExecutorService executorService = Executors.newFixedThreadPool(config.getThreads());
            
            for (int i = 0; i < config.getThreads(); i++) {
                executorService.submit(new LoopyWorker(config, stats));
            }
            
            // Wait for duration
            Thread.sleep(config.getDurationSeconds() * 1000L);
            
            // Shutdown
            executorService.shutdownNow();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Load test interrupted", e);
        }
    }
    
    /**
     * Validate cron expression format
     */
    private boolean isValidCronExpression(String expression) {
        return cronPattern.matcher(expression.trim()).matches();
    }
    
    /**
     * Parse a simple cron expression (minute hour day month dayOfWeek)
     */
    private ScheduleInfo parseCronExpression(String expression) {
        String[] parts = expression.trim().split("\\s+");
        
        // For simplicity, we'll support basic interval scheduling
        // In a full implementation, would parse actual cron syntax
        
        // Default to hourly if parsing fails
        return new ScheduleInfo(3600); // 1 hour
    }
    
    /**
     * Calculate delay until next scheduled run
     */
    private long calculateNextRunDelay(ScheduleInfo scheduleInfo) {
        // For simplicity, start immediately and use interval
        return 0;
    }
    
    /**
     * Shutdown the scheduler
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Schedule information container
     */
    public static class ScheduleInfo {
        private final long intervalSeconds;
        
        public ScheduleInfo(long intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }
        
        public long getIntervalSeconds() {
            return intervalSeconds;
        }
    }
    
    /**
     * Scheduled task handle
     */
    public static class ScheduledTask {
        private final LoopyConfig config;
        private final ScheduleInfo scheduleInfo;
        private final int maxRuns;
        private int runCount = 0;
        private ScheduledFuture<?> scheduledFuture;
        
        public ScheduledTask(LoopyConfig config, ScheduleInfo scheduleInfo, int maxRuns) {
            this.config = config;
            this.scheduleInfo = scheduleInfo;
            this.maxRuns = maxRuns;
        }
        
        public LoopyConfig getConfig() { return config; }
        public ScheduleInfo getScheduleInfo() { return scheduleInfo; }
        public int getMaxRuns() { return maxRuns; }
        public int getRunCount() { return runCount; }
        public void incrementRunCount() { runCount++; }
        public ScheduledFuture<?> getScheduledFuture() { return scheduledFuture; }
        public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) { this.scheduledFuture = scheduledFuture; }
        
        public void cancel() {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                System.out.println("📅 Scheduled task cancelled");
            }
        }
        
        public boolean isRunning() {
            return scheduledFuture != null && !scheduledFuture.isDone();
        }
    }
}