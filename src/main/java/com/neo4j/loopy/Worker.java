package com.neo4j.loopy;

/**
 * Interface for worker threads that generate load against Neo4j database.
 * Provides a common contract for different worker implementations
 * (e.g., LoopyWorker for programmatic data generation, CypherFileWorker for YAML-based queries).
 */
public interface Worker extends Runnable {
    
    /**
     * Signal the worker to stop processing.
     * The worker should complete its current operation and exit gracefully.
     */
    void stop();
    
    /**
     * Check if the worker is currently running.
     * @return true if the worker is running, false otherwise
     */
    boolean isRunning();
}
