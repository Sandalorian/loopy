package com.neo4j.loopy;

import com.neo4j.loopy.config.CypherWorkloadConfig;
import com.neo4j.loopy.tx.QueryUnit;
import com.neo4j.loopy.tx.TransactionExecutor;
import com.neo4j.loopy.tx.TransactionExecutorFactory;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Worker thread that executes Cypher queries defined in a YAML workload file.
 * Queries are selected based on their configured weights using weighted random selection.
 *
 * <p>Each selected {@link QueryUnit} may be a single query or a transaction group.
 * Execution is delegated to a {@link TransactionExecutor} chosen from the global
 * {@link TransactionMode}; individual queries may override the mode via
 * {@code transactionMode} in the YAML definition.
 */
public class CypherFileWorker implements Worker {
    
    private final Driver driver;
    private final LoopyConfig config;
    private final LoopyStats stats;
    private final CypherWorkloadConfig workloadConfig;
    private final Random random = ThreadLocalRandom.current();
    private final boolean failFast;
    private final TransactionMode globalMode;
    private volatile boolean running = true;

    public CypherFileWorker(LoopyConfig config, LoopyStats stats,
                            CypherWorkloadConfig workloadConfig, boolean failFast) {
        this.config = config;
        this.stats = stats;
        this.workloadConfig = workloadConfig;
        this.failFast = failFast;
        this.globalMode = config.getTransactionModeEnum();
        this.driver = GraphDatabase.driver(
            config.getNeo4jUri(),
            AuthTokens.basic(config.getNeo4jUsername(), config.getNeo4jPassword())
        );
    }
    
    @Override
    public void run() {
        if (globalMode == TransactionMode.EXECUTE_QUERY) {
            // execute-query mode uses the driver directly; no session needed
            try {
                runLoop(null);
            } finally {
                try { driver.close(); } catch (Exception ignored) {}
            }
        } else {
            try (Session session = driver.session()) {
                runLoop(session);
            } catch (Exception e) {
                System.err.println("Failed to create session: " + e.getMessage());
            } finally {
                try { driver.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void runLoop(Session session) {
        while (running) {
            QueryUnit unit = null;
            try {
                unit = workloadConfig.selectQueryUnit();
                TransactionMode effectiveMode = unit.resolveMode(globalMode);
                TransactionExecutor executor = TransactionExecutorFactory.create(effectiveMode);
                executor.execute(session, driver, unit, stats);
                Thread.sleep(1);

            } catch (Neo4jException e) {
                // Executor already recorded per-query error; log and backoff
                String unitId = unit != null ? unit.getId() : "unknown";
                System.err.println("Neo4j error in '" + unitId + "': " + e.getMessage());

                if (failFast) {
                    System.err.println("Fail-fast mode: aborting worker due to error");
                    break;
                }
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
                String unitId = unit != null ? unit.getId() : "unknown";
                System.err.println("Unexpected error in '" + unitId + "': " + e.getMessage());
                if (failFast) {
                    System.err.println("Fail-fast mode: aborting worker due to error");
                    break;
                }
            }
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
