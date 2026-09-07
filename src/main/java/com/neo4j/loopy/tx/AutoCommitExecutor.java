package com.neo4j.loopy.tx;

import com.neo4j.loopy.LoopyStats;
import com.neo4j.loopy.config.CypherWorkloadConfig.QueryDefinition;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.Map;

/**
 * Executes each query using {@code session.run()} — an auto-commit transaction.
 * Each call is independent: no begin/commit overhead, no driver-level retry.
 * The database provides whatever result (or error) it gets.
 *
 * <p>When a unit contains multiple queries they are each executed as separate
 * auto-commit transactions (order preserved).
 */
public class AutoCommitExecutor implements TransactionExecutor {

    @Override
    public void execute(Session session, Driver driver, QueryUnit unit, LoopyStats stats) {
        for (QueryDefinition query : unit.getQueries()) {
            executeSingle(session, query, stats);
        }
    }

    private void executeSingle(Session session, QueryDefinition query, LoopyStats stats) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> params = query.generateParameters();
            Result result = session.run(query.getCypher(), params);
            while (result.hasNext()) {
                result.next();
            }
            stats.recordQuery(query.getId(), System.currentTimeMillis() - start, query.isWrite());
            stats.recordTransaction();
        } catch (Neo4jException e) {
            stats.recordQueryError(query.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            stats.recordQueryError(query.getId(), e.getMessage());
            throw new RuntimeException("Auto-commit query failed: " + query.getId(), e);
        }
    }
}
