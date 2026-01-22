package com.neo4j.loopy;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics collector for Loopy operations
 */
public class LoopyStats {
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong writeOperations = new AtomicLong(0);
    private final AtomicLong readOperations = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    
    private long lastReportTime = System.currentTimeMillis();
    private long lastWriteCount = 0;
    private long lastReadCount = 0;
    private long lastOperationCount = 0;
    
    private final FileWriter csvWriter;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public LoopyStats(LoopyConfig config) {
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
    
    public void recordError() {
        errors.incrementAndGet();
    }
    
    public void printStats() {
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
        if (csvWriter != null) {
            try {
                csvWriter.write(String.format("%s,%d,%.2f,%.2f,%d,%d\n",
                    timestamp, currentOperations, writePerSec, readPerSec, avgResponseTime, errors.get()));
                csvWriter.flush();
            } catch (IOException e) {
                System.err.println("Warning: Could not write to CSV: " + e.getMessage());
            }
        }
        
        lastReportTime = currentTime;
        lastWriteCount = currentWrites;
        lastReadCount = currentReads;
        lastOperationCount = currentOperations;
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
}