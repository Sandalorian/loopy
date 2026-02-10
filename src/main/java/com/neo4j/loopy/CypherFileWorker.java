package com.neo4j.loopy;

import com.neo4j.loopy.config.CypherWorkloadConfig;
import com.neo4j.loopy.config.CypherWorkloadConfig.QueryDefinition;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Worker thread that executes Cypher queries defined in a YAML workload file.
 * Queries are selected based on their configured weights using weighted random selection.
 */
public class CypherFileWorker implements Worker {
    
    private final Driver driver;
    private final LoopyConfig config;
    private final LoopyStats stats;
    private final CypherWorkloadConfig workloadConfig;
    private final Random random = ThreadLocalRandom.current();
    private final boolean failFast;
    private volatile boolean running = true;
    
    /**
     * Create a new CypherFileWorker
     * @param config application configuration
     * @param stats statistics collector
     * @param workloadConfig YAML workload configuration
     * @param failFast if true, abort on first query failure
     */
    public CypherFileWorker(LoopyConfig config, LoopyStats stats, 
                            CypherWorkloadConfig workloadConfig, boolean failFast) {
        this.config = config;
        this.stats = stats;
        this.workloadConfig = workloadConfig;
        this.failFast = failFast;
        this.driver = GraphDatabase.driver(
            config.getNeo4jUri(),
            AuthTokens.basic(config.getNeo4jUsername(), config.getNeo4jPassword())
        );
    }
    
    @Override
    public void run() {
        try (Session session = driver.session()) {
            while (running) {
                try {
                    // Select a query based on weights
                    QueryDefinition query = workloadConfig.selectWeightedQuery();
                    
                    // Execute the query
                    executeQuery(session, query);
                    
                    // Small delay to prevent overwhelming the database
                    Thread.sleep(1);
                    
                } catch (Neo4jException e) {
                    handleNeo4jError(e, null);
                    
                    if (failFast) {
                        System.err.println("Fail-fast mode: aborting worker due to error");
                        break;
                    }
                    
                    // Retry with exponential backoff
                    try {
                        Thread.sleep(1000 + random.nextInt(2000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    handleUnexpectedError(e, null);
                    
                    if (failFast) {
                        System.err.println("Fail-fast mode: aborting worker due to error");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to create session: " + e.getMessage());
        } finally {
            try {
                driver.close();
            } catch (Exception e) {
                // Ignore close errors
            }
        }
    }
    
    /**
     * Execute a single query with generated parameters
     */
    private void executeQuery(Session session, QueryDefinition query) {
        long startTime = System.currentTimeMillis();
        String queryId = query.getId();
        boolean isWrite = query.isWrite();
        
        try {
            // Generate parameter values
            Map<String, Object> params = query.generateParameters();
            
            // Execute the query
            String cypher = query.getCypher();
            Result result = session.run(cypher, params);
            
            // Consume all results
            while (result.hasNext()) {
                result.next();
            }
            
            // Record statistics
            long responseTime = System.currentTimeMillis() - startTime;
            stats.recordQuery(queryId, responseTime, isWrite);
            
        } catch (Neo4jException e) {
            handleNeo4jError(e, queryId);
            throw e; // Re-throw to trigger retry logic
        } catch (Exception e) {
            handleUnexpectedError(e, queryId);
            throw new RuntimeException("Query execution failed: " + queryId, e);
        }
    }
    
    /**
     * Handle Neo4j-specific errors
     */
    private void handleNeo4jError(Neo4jException e, String queryId) {
        if (queryId != null) {
            stats.recordQueryError(queryId, e.getMessage());
            System.err.println("Neo4j error in query '" + queryId + "': " + e.getMessage());
        } else {
            stats.recordError();
            System.err.println("Neo4j error: " + e.getMessage());
        }
    }
    
    /**
     * Handle unexpected errors
     */
    private void handleUnexpectedError(Exception e, String queryId) {
        if (queryId != null) {
            stats.recordQueryError(queryId, e.getMessage());
            System.err.println("Unexpected error in query '" + queryId + "': " + e.getMessage());
        } else {
            stats.recordError();
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
    
    @Override
    public void stop() {
        running = false;
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
}
