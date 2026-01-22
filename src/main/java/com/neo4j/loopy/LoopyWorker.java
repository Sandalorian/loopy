package com.neo4j.loopy;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Worker thread that generates load against Neo4j database
 */
public class LoopyWorker implements Runnable {
    private final Driver driver;
    private final LoopyConfig config;
    private final LoopyStats stats;
    private final Random random = ThreadLocalRandom.current();
    private volatile boolean running = true;
    
    public LoopyWorker(LoopyConfig config, LoopyStats stats) {
        this.config = config;
        this.stats = stats;
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
                    if (random.nextDouble() < config.getWriteRatio()) {
                        performWriteOperation(session);
                    } else {
                        performReadOperation(session);
                    }
                    
                    // Small delay to prevent overwhelming the database
                    Thread.sleep(1);
                    
                } catch (Neo4jException e) {
                    stats.recordError();
                    System.err.println("Neo4j error: " + e.getMessage());
                    
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
                    stats.recordError();
                    System.err.println("Unexpected error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to create session: " + e.getMessage());
        } finally {
            driver.close();
        }
    }
    
    private void performWriteOperation(Session session) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (random.nextBoolean()) {
                createNode(session);
            } else {
                createRelationship(session);
            }
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;
            stats.recordWrite(responseTime);
        }
    }
    
    private void performReadOperation(Session session) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (random.nextBoolean()) {
                readNodes(session);
            } else {
                readRelationships(session);
            }
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;
            stats.recordRead(responseTime);
        }
    }
    
    private void createNode(Session session) {
        String label = getRandomNodeLabel();
        Map<String, Object> properties = generateProperties();
        
        String cypher = "CREATE (n:" + label + " $props) RETURN id(n)";
        Map<String, Object> params = new HashMap<>();
        params.put("props", properties);
        session.run(cypher, params);
    }
    
    private void createRelationship(Session session) {
        String relationshipType = getRandomRelationshipType();
        Map<String, Object> properties = generateProperties();
        
        // Create relationship between random existing nodes
        String cypher = "MATCH (a), (b) " +
                "WHERE id(a) <> id(b) " +
                "WITH a, b " +
                "LIMIT 1 " +
                "CREATE (a)-[r:" + relationshipType + " $props]->(b) " +
                "RETURN id(r)";
        
        Map<String, Object> params = new HashMap<>();
        params.put("props", properties);
        Result result = session.run(cypher, params);
        
        // If no existing nodes, create two nodes and connect them
        if (!result.hasNext()) {
            String label1 = getRandomNodeLabel();
            String label2 = getRandomNodeLabel();
            
            String createCypher = "CREATE (a:" + label1 + " $props1)-[r:" + relationshipType + 
                    " $relProps]->(b:" + label2 + " $props2) RETURN id(r)";
            
            Map<String, Object> createParams = new HashMap<>();
            createParams.put("props1", generateProperties());
            createParams.put("props2", generateProperties());
            createParams.put("relProps", properties);
            session.run(createCypher, createParams);
        }
    }
    
    private void readNodes(Session session) {
        String label = getRandomNodeLabel();
        String cypher = "MATCH (n:" + label + ") RETURN n LIMIT " + config.getBatchSize();
        
        Result result = session.run(cypher);
        while (result.hasNext()) {
            result.next(); // Consume the result
        }
    }
    
    private void readRelationships(Session session) {
        String relationshipType = getRandomRelationshipType();
        String cypher = "MATCH ()-[r:" + relationshipType + "]->() RETURN r LIMIT " + config.getBatchSize();
        
        Result result = session.run(cypher);
        while (result.hasNext()) {
            result.next(); // Consume the result
        }
    }
    
    private String getRandomNodeLabel() {
        List<String> labels = config.getNodeLabels();
        return labels.get(random.nextInt(labels.size()));
    }
    
    private String getRandomRelationshipType() {
        List<String> types = config.getRelationshipTypes();
        return types.get(random.nextInt(types.size()));
    }
    
    private Map<String, Object> generateProperties() {
        Map<String, Object> properties = new HashMap<>();
        
        // Generate some realistic properties
        properties.put("id", random.nextLong());
        properties.put("name", "Entity_" + random.nextInt(100000));
        properties.put("timestamp", System.currentTimeMillis());
        properties.put("value", random.nextDouble() * 1000);
        
        // Add a large property to reach the configured size
        int remainingBytes = config.getPropertySizeBytes() - 200; // Rough estimate for other properties
        if (remainingBytes > 0) {
            StringBuilder largeProperty = new StringBuilder();
            for (int i = 0; i < remainingBytes / 10; i++) {
                largeProperty.append("data_").append(i).append("_");
            }
            properties.put("large_data", largeProperty.toString());
        }
        
        return properties;
    }
    
    public void stop() {
        running = false;
    }
}