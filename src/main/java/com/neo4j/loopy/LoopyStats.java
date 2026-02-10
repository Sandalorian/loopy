package com.neo4j.loopy;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics collector for Loopy operations.
 * Supports both aggregated and per-query statistics with percentile tracking.
 */
public class LoopyStats {
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong writeOperations = new AtomicLong(0);
    private final AtomicLong readOperations = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    
    // Per-query statistics
    private final ConcurrentHashMap<String, QueryStats> queryStats = new ConcurrentHashMap<>();
    private boolean verboseStats = false;
    private String statsFormat = "summary";
    
    private long lastReportTime = System.currentTimeMillis();
    private long lastWriteCount = 0;
    private long lastReadCount = 0;
    private long lastOperationCount = 0;
    
    private final FileWriter csvWriter;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public LoopyStats(LoopyConfig config) {
        this(config, false, "summary");
    }
    
    public LoopyStats(LoopyConfig config, boolean verboseStats, String statsFormat) {
        this.verboseStats = verboseStats;
        this.statsFormat = statsFormat != null ? statsFormat : "summary";
        
        FileWriter writer = null;
        if (config.isCsvLoggingEnabled()) {
            try {
                writer = new FileWriter(config.getCsvLoggingFile());
                writer.write("timestamp,total_operations,write_ops_per_sec,read_ops_per_sec,avg_response_ms,errors\n");
            } catch (IOException e) {
                System.err.println("Warning: Could not create CSV log file: " + e.getMessage());
            }
        }
        this.csvWriter = writer;
    }
    
    public void recordWrite(long responseTimeMs) {
        writeOperations.incrementAndGet();
        totalOperations.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
    }
    
    public void recordRead(long responseTimeMs) {
        readOperations.incrementAndGet();
        totalOperations.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
    }
    
    /**
     * Record a query execution with per-query tracking
     * @param queryId unique query identifier
     * @param responseTimeMs response time in milliseconds
     * @param isWrite true if this is a write operation
     */
    public void recordQuery(String queryId, long responseTimeMs, boolean isWrite) {
        if (isWrite) {
            recordWrite(responseTimeMs);
        } else {
            recordRead(responseTimeMs);
        }
        
        if (verboseStats && queryId != null) {
            queryStats.computeIfAbsent(queryId, k -> new QueryStats(queryId))
                     .record(responseTimeMs, isWrite);
        }
    }
    
    /**
     * Record a query error with per-query tracking
     * @param queryId unique query identifier
     * @param errorMessage error message
     */
    public void recordQueryError(String queryId, String errorMessage) {
        errors.incrementAndGet();
        
        if (verboseStats && queryId != null) {
            queryStats.computeIfAbsent(queryId, k -> new QueryStats(queryId))
                     .recordError(errorMessage);
        }
    }
    
    public void recordError() {
        errors.incrementAndGet();
    }
    
    public void printStats() {
        switch (statsFormat.toLowerCase()) {
            case "json":
                printJsonStats();
                break;
            case "detailed":
                printDetailedStats();
                break;
            default:
                printSummaryStats();
        }
    }
    
    private void printSummaryStats() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastReportTime;
        
        if (timeDiff < 1000) return; // Avoid division by zero
        
        long currentWrites = writeOperations.get();
        long currentReads = readOperations.get();
        long currentOperations = totalOperations.get();
        
        double writePerSec = (currentWrites - lastWriteCount) / (timeDiff / 1000.0);
        double readPerSec = (currentReads - lastReadCount) / (timeDiff / 1000.0);
        
        long avgResponseTime = currentOperations > 0 ? 
            totalResponseTime.get() / currentOperations : 0;
        
        String timestamp = LocalDateTime.now().format(formatter);
        String output = String.format(
            "[%s] Operations: %,d | Write/sec: %.0f | Read/sec: %.0f | Avg Response: %dms | Errors: %d",
            timestamp, currentOperations, writePerSec, readPerSec, avgResponseTime, errors.get()
        );
        
        System.out.println(output);
        
        // Write to CSV if enabled
        writeToCsv(timestamp, currentOperations, writePerSec, readPerSec, avgResponseTime);
        
        lastReportTime = currentTime;
        lastWriteCount = currentWrites;
        lastReadCount = currentReads;
        lastOperationCount = currentOperations;
    }
    
    private void printDetailedStats() {
        printSummaryStats();
        
        if (verboseStats && !queryStats.isEmpty()) {
            System.out.println("\n  Per-Query Statistics:");
            for (Map.Entry<String, QueryStats> entry : queryStats.entrySet()) {
                QueryStats stats = entry.getValue();
                System.out.printf("    %s: count=%d, avg=%.1fms, p50=%.1fms, p95=%.1fms, p99=%.1fms, errors=%d%n",
                    stats.queryId,
                    stats.getCount(),
                    stats.getAvgResponseTime(),
                    stats.getPercentile(50),
                    stats.getPercentile(95),
                    stats.getPercentile(99),
                    stats.getErrorCount()
                );
            }
        }
    }
    
    private void printJsonStats() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastReportTime;
        
        if (timeDiff < 1000) return;
        
        long currentWrites = writeOperations.get();
        long currentReads = readOperations.get();
        long currentOperations = totalOperations.get();
        
        double writePerSec = (currentWrites - lastWriteCount) / (timeDiff / 1000.0);
        double readPerSec = (currentReads - lastReadCount) / (timeDiff / 1000.0);
        
        long avgResponseTime = currentOperations > 0 ? 
            totalResponseTime.get() / currentOperations : 0;
        
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"timestamp\":\"").append(LocalDateTime.now().format(formatter)).append("\",");
        json.append("\"totalOperations\":").append(currentOperations).append(",");
        json.append("\"writeOpsPerSec\":").append(String.format("%.2f", writePerSec)).append(",");
        json.append("\"readOpsPerSec\":").append(String.format("%.2f", readPerSec)).append(",");
        json.append("\"avgResponseMs\":").append(avgResponseTime).append(",");
        json.append("\"errors\":").append(errors.get());
        
        if (verboseStats && !queryStats.isEmpty()) {
            json.append(",\"queries\":{");
            boolean first = true;
            for (Map.Entry<String, QueryStats> entry : queryStats.entrySet()) {
                if (!first) json.append(",");
                first = false;
                QueryStats stats = entry.getValue();
                json.append("\"").append(stats.queryId).append("\":{");
                json.append("\"count\":").append(stats.getCount()).append(",");
                json.append("\"avgMs\":").append(String.format("%.1f", stats.getAvgResponseTime())).append(",");
                json.append("\"p50\":").append(String.format("%.1f", stats.getPercentile(50))).append(",");
                json.append("\"p95\":").append(String.format("%.1f", stats.getPercentile(95))).append(",");
                json.append("\"p99\":").append(String.format("%.1f", stats.getPercentile(99))).append(",");
                json.append("\"errors\":").append(stats.getErrorCount());
                json.append("}");
            }
            json.append("}");
        }
        
        json.append("}");
        System.out.println(json);
        
        // Write to CSV if enabled
        writeToCsv(LocalDateTime.now().format(formatter), currentOperations, writePerSec, readPerSec, avgResponseTime);
        
        lastReportTime = currentTime;
        lastWriteCount = currentWrites;
        lastReadCount = currentReads;
        lastOperationCount = currentOperations;
    }
    
    private void writeToCsv(String timestamp, long operations, double writePerSec, double readPerSec, long avgResponse) {
        if (csvWriter != null) {
            try {
                csvWriter.write(String.format("%s,%d,%.2f,%.2f,%d,%d\n",
                    timestamp, operations, writePerSec, readPerSec, avgResponse, errors.get()));
                csvWriter.flush();
            } catch (IOException e) {
                System.err.println("Warning: Could not write to CSV: " + e.getMessage());
            }
        }
    }
    
    /**
     * Print final statistics summary
     */
    public void printFinalStats() {
        System.out.println("\n\u001B[36mFinal Statistics:\u001B[0m");
        System.out.println("Total Operations: " + getTotalOperations());
        System.out.println("Total Writes: " + writeOperations.get());
        System.out.println("Total Reads: " + readOperations.get());
        System.out.println("Total Errors: " + getErrors());
        
        if (getTotalOperations() > 0) {
            System.out.printf("Average Response Time: %dms%n", totalResponseTime.get() / getTotalOperations());
        }
        
        if (verboseStats && !queryStats.isEmpty()) {
            System.out.println("\nPer-Query Final Statistics:");
            System.out.println("----------------------------");
            for (Map.Entry<String, QueryStats> entry : queryStats.entrySet()) {
                QueryStats stats = entry.getValue();
                System.out.printf("%s:%n", stats.queryId);
                System.out.printf("  Count: %d (writes: %d, reads: %d)%n", 
                    stats.getCount(), stats.writeCount.get(), stats.readCount.get());
                System.out.printf("  Avg Response: %.1fms%n", stats.getAvgResponseTime());
                System.out.printf("  Percentiles: p50=%.1fms, p95=%.1fms, p99=%.1fms%n",
                    stats.getPercentile(50), stats.getPercentile(95), stats.getPercentile(99));
                System.out.printf("  Errors: %d%n", stats.getErrorCount());
            }
        }
    }
    
    public void close() {
        if (csvWriter != null) {
            try {
                csvWriter.close();
            } catch (IOException e) {
                System.err.println("Warning: Could not close CSV file: " + e.getMessage());
            }
        }
    }
    
    public long getTotalOperations() {
        return totalOperations.get();
    }
    
    public long getErrors() {
        return errors.get();
    }
    
    public Map<String, QueryStats> getQueryStats() {
        return Collections.unmodifiableMap(queryStats);
    }
    
    public void setVerboseStats(boolean verboseStats) {
        this.verboseStats = verboseStats;
    }
    
    public void setStatsFormat(String statsFormat) {
        this.statsFormat = statsFormat;
    }
    
    /**
     * Per-query statistics with percentile tracking using reservoir sampling
     */
    public static class QueryStats {
        private final String queryId;
        private final AtomicLong totalCount = new AtomicLong(0);
        private final AtomicLong writeCount = new AtomicLong(0);
        private final AtomicLong readCount = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        
        // Reservoir sampling for percentile calculation (thread-safe with synchronization)
        private static final int RESERVOIR_SIZE = 1000;
        private final List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>(RESERVOIR_SIZE));
        private final AtomicLong sampleCount = new AtomicLong(0);
        
        public QueryStats(String queryId) {
            this.queryId = queryId;
        }
        
        public void record(long responseTimeMs, boolean isWrite) {
            totalCount.incrementAndGet();
            totalResponseTime.addAndGet(responseTimeMs);
            
            if (isWrite) {
                writeCount.incrementAndGet();
            } else {
                readCount.incrementAndGet();
            }
            
            // Reservoir sampling for percentile calculation
            long count = sampleCount.incrementAndGet();
            if (count <= RESERVOIR_SIZE) {
                responseTimes.add(responseTimeMs);
            } else {
                // Probability of keeping the new sample
                int replaceIndex = (int) (Math.random() * count);
                if (replaceIndex < RESERVOIR_SIZE) {
                    synchronized (responseTimes) {
                        if (replaceIndex < responseTimes.size()) {
                            responseTimes.set(replaceIndex, responseTimeMs);
                        }
                    }
                }
            }
        }
        
        public void recordError(String errorMessage) {
            errorCount.incrementAndGet();
        }
        
        public long getCount() {
            return totalCount.get();
        }
        
        public long getErrorCount() {
            return errorCount.get();
        }
        
        public double getAvgResponseTime() {
            long count = totalCount.get();
            return count > 0 ? (double) totalResponseTime.get() / count : 0;
        }
        
        /**
         * Calculate percentile from reservoir sample
         * @param percentile the percentile to calculate (0-100)
         * @return the percentile value in milliseconds
         */
        public double getPercentile(int percentile) {
            synchronized (responseTimes) {
                if (responseTimes.isEmpty()) {
                    return 0;
                }
                
                List<Long> sorted = new ArrayList<>(responseTimes);
                Collections.sort(sorted);
                
                int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
                index = Math.max(0, Math.min(index, sorted.size() - 1));
                return sorted.get(index);
            }
        }
        
        public String getQueryId() {
            return queryId;
        }
    }
}