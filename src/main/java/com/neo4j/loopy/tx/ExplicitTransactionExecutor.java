package com.neo4j.loopy.tx;

import com.neo4j.loopy.LoopyStats;
import com.neo4j.loopy.config.CypherWorkloadConfig.QueryDefinition;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.Map;

/**
 * Executes queries using an explicit transaction ({@code session.beginTransaction()}).
 *
 * <p>All queries in the unit run within the same transaction; the transaction is
 * committed on success or rolled back on any failure. No driver-level retry is
 * applied — the calling worker is responsible for error handling and backoff.
 *
 * <p>Response time is measured over the full begin-to-commit window and split
 * evenly across the queries in the unit for per-query statistics.
 */
public class ExplicitTransactionExecutor implements TransactionExecutor {

    @Override
    public void execute(Session session, Driver driver, QueryUnit unit, LoopyStats stats) {
        long start = System.currentTimeMillis();
        Transaction tx = session.beginTransaction();
        try {
            for (QueryDefinition query : unit.getQueries()) {
                Map<String, Object> params = query.generateParameters();
                Result result = tx.run(query.getCypher(), params);
                while (result.hasNext()) {
                    result.next();
                }
            }
            tx.commit();

            long elapsed = System.currentTimeMillis() - start;
            long perQuery = Math.max(1L, elapsed / unit.getQueries().size());
            for (QueryDefinition query : unit.getQueries()) {
                stats.recordQuery(query.getId(), perQuery, query.isWrite());
            }
            stats.recordTransaction();

        } catch (Neo4jException e) {
            rollbackQuietly(tx);
            for (QueryDefinition query : unit.getQueries()) {
                stats.recordQueryError(query.getId(), e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            rollbackQuietly(tx);
            for (QueryDefinition query : unit.getQueries()) {
                stats.recordQueryError(query.getId(), e.getMessage());
            }
            throw new RuntimeException("Explicit transaction failed: " + unit.getId(), e);
        }
    }

    private void rollbackQuietly(Transaction tx) {
        try {
            tx.rollback();
        } catch (Exception ignored) {
        }
    }
}
