package com.neo4j.loopy;

import com.neo4j.loopy.config.CypherWorkloadConfig.DynamicQueryDefinition;
import com.neo4j.loopy.config.CypherWorkloadConfig.QueryDefinition;
import com.neo4j.loopy.config.CypherWorkloadConfig.QueryType;
import com.neo4j.loopy.tx.QueryUnit;
import com.neo4j.loopy.tx.TransactionExecutor;
import com.neo4j.loopy.tx.TransactionExecutorFactory;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Worker thread that generates load against Neo4j database
 * using programmatic data generation (nodes, relationships, properties).
 *
 * <p>Each operation is wrapped in a {@link QueryUnit} and executed via
 * the {@link TransactionExecutor} corresponding to the configured
 * {@link TransactionMode}, allowing side-by-side comparison of all four
 * Neo4j transaction APIs.
 */
public class LoopyWorker implements Worker {
    private final Driver driver;
    private final LoopyConfig config;
    private final LoopyStats stats;
    private final TransactionMode globalMode;
    private final TransactionExecutor executor;
    private final Random random = ThreadLocalRandom.current();
    private volatile boolean running = true;
    
    public LoopyWorker(LoopyConfig config, LoopyStats stats) {
        this.config = config;
        this.stats = stats;
        this.globalMode = config.getTransactionModeEnum();
        this.executor = TransactionExecutorFactory.create(globalMode);
        this.driver = GraphDatabase.driver(
            config.getNeo4jUri(),
            AuthTokens.basic(config.getNeo4jUsername(), config.getNeo4jPassword())
        );
    }
    
    @Override
    public void run() {
        if (globalMode == TransactionMode.EXECUTE_QUERY) {
            // execute-query mode uses the driver directly; no session required
            try {
                runLoop(null);
            } finally {
                driver.close();
            }
        } else {
            try (Session session = driver.session()) {
                runLoop(session);
            } catch (Exception e) {
                System.err.println("Failed to create session: " + e.getMessage());
            } finally {
                driver.close();
            }
        }
    }

    private void runLoop(Session session) {
        while (running) {
            try {
                QueryUnit unit = buildNextUnit();
                executor.execute(session, driver, unit, stats);
                Thread.sleep(1);
            } catch (Neo4jException e) {
                // Executor already recorded the per-query error; just backoff
                System.err.println("Neo4j error: " + e.getMessage());
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
                System.err.println("Unexpected error: " + e.getMessage());
            }
        }
    }

    // ── Query unit builders ───────────────────────────────────────────────────

    /**
     * Build one operation (read or write query definition) according to the
     * configured write ratio.
     */
    private QueryDefinition buildQueryDefinition() {
        boolean isWrite = random.nextDouble() < config.getWriteRatio();
        return isWrite ? buildWriteQueryDefinition() : buildReadQueryDefinition();
    }

    private QueryDefinition buildWriteQueryDefinition() {
        return random.nextBoolean() ? buildCreateNodeQuery() : buildCreateRelationshipQuery();
    }

    private QueryDefinition buildReadQueryDefinition() {
        return random.nextBoolean() ? buildReadNodesQuery() : buildReadRelationshipsQuery();
    }

    private QueryDefinition buildCreateNodeQuery() {
        String label = getRandomNodeLabel();
        String cypher = "CREATE (n:" + label + " $props) RETURN id(n)";
        return new DynamicQueryDefinition("create-node-" + label, cypher, QueryType.WRITE,
            () -> Map.of("props", generateProperties()));
    }

    private QueryDefinition buildCreateRelationshipQuery() {
        String label1 = getRandomNodeLabel();
        String label2 = getRandomNodeLabel();
        String relType = getRandomRelationshipType();
        String cypher = "CREATE (a:" + label1 + " $props1)-[r:" + relType +
                        " $relProps]->(b:" + label2 + " $props2) RETURN id(r)";
        return new DynamicQueryDefinition("create-rel-" + relType, cypher, QueryType.WRITE,
            () -> {
                Map<String, Object> params = new HashMap<>();
                params.put("props1", generateProperties());
                params.put("props2", generateProperties());
                params.put("relProps", generateProperties());
                return params;
            });
    }

    private QueryDefinition buildReadNodesQuery() {
        String label = getRandomNodeLabel();
        String cypher = "MATCH (n:" + label + ") RETURN n LIMIT " + config.getBatchSize();
        return new DynamicQueryDefinition("read-nodes-" + label, cypher, QueryType.READ,
            Collections::emptyMap);
    }

    private QueryDefinition buildReadRelationshipsQuery() {
        String relType = getRandomRelationshipType();
        String cypher = "MATCH ()-[r:" + relType + "]->() RETURN r LIMIT " + config.getBatchSize();
        return new DynamicQueryDefinition("read-rels-" + relType, cypher, QueryType.READ,
            Collections::emptyMap);
    }

    /**
     * Build the next unit of work. When {@code --transaction-group-size} is greater
     * than 1, groups that many same-type operations (all reads or all writes) into a
     * single {@link QueryUnit} so they execute inside one explicit/managed transaction.
     */
    private QueryUnit buildNextUnit() {
        int groupSize = config.getTransactionGroupSize();
        if (groupSize <= 1) {
            return QueryUnit.single(buildQueryDefinition(), null);
        }

        boolean isWrite = random.nextDouble() < config.getWriteRatio();
        List<QueryDefinition> group = new java.util.ArrayList<>(groupSize);
        for (int i = 0; i < groupSize; i++) {
            group.add(isWrite ? buildWriteQueryDefinition() : buildReadQueryDefinition());
        }
        return QueryUnit.group("generated-group-" + (isWrite ? "write" : "read"), group, null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
        properties.put("id", random.nextLong());
        properties.put("name", "Entity_" + random.nextInt(100000));
        properties.put("timestamp", System.currentTimeMillis());
        properties.put("value", random.nextDouble() * 1000);
        
        int remainingBytes = config.getPropertySizeBytes() - 200;
        if (remainingBytes > 0) {
            StringBuilder largeProperty = new StringBuilder();
            for (int i = 0; i < remainingBytes / 10; i++) {
                largeProperty.append("data_").append(i).append("_");
            }
            properties.put("large_data", largeProperty.toString());
        }
        return properties;
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