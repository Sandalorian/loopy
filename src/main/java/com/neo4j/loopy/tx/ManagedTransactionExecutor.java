package com.neo4j.loopy.tx;

import com.neo4j.loopy.LoopyStats;
import com.neo4j.loopy.config.CypherWorkloadConfig.QueryDefinition;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes queries using a managed transaction ({@code session.executeRead()} or
 * {@code session.executeWrite()}). The driver automatically retries the callback
 * on transient errors; each retry attempt is recorded in {@link LoopyStats}.
 *
 * <p>All queries in the unit are executed inside the same managed-transaction
 * callback, so they all benefit from the driver's retry mechanism.
 *
 * <p>Response time is measured over the total duration (including any retries)
 * and split evenly across the queries in the unit.
 */
public class ManagedTransactionExecutor implements TransactionExecutor {

    private final boolean useReadRouting;

    /**
     * @param useReadRouting {@code true} to use {@code session.executeRead()}
     *                       (read routing), {@code false} for {@code session.executeWrite()}
     */
    public ManagedTransactionExecutor(boolean useReadRouting) {
        this.useReadRouting = useReadRouting;
    }

    @Override
    public void execute(Session session, Driver driver, QueryUnit unit, LoopyStats stats) {
        long start = System.currentTimeMillis();
        AtomicInteger invocations = new AtomicInteger(0);

        try {
            TransactionCallback<Void> callback = tx -> {
                // Every invocation after the first is a driver-initiated retry
                if (invocations.incrementAndGet() > 1) {
                    for (QueryDefinition q : unit.getQueries()) {
                        stats.recordRetryAttempt(q.getId());
                    }
                }
                for (QueryDefinition query : unit.getQueries()) {
                    Map<String, Object> params = query.generateParameters();
                    Result result = tx.run(query.getCypher(), params);
                    while (result.hasNext()) {
                        result.next();
                    }
                }
                return null;
            };

            if (useReadRouting) {
                session.executeRead(callback);
            } else {
                session.executeWrite(callback);
            }

            long elapsed = System.currentTimeMillis() - start;
            long perQuery = Math.max(1L, elapsed / unit.getQueries().size());
            for (QueryDefinition query : unit.getQueries()) {
                stats.recordQuery(query.getId(), perQuery, query.isWrite());
            }
            stats.recordTransaction();

        } catch (Neo4jException e) {
            for (QueryDefinition query : unit.getQueries()) {
                stats.recordQueryError(query.getId(), e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            for (QueryDefinition query : unit.getQueries()) {
                stats.recordQueryError(query.getId(), e.getMessage());
            }
            throw new RuntimeException("Managed transaction failed: " + unit.getId(), e);
        }
    }
}
