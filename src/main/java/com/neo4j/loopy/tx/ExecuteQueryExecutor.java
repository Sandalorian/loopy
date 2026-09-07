package com.neo4j.loopy.tx;

import com.neo4j.loopy.LoopyStats;
import com.neo4j.loopy.config.CypherWorkloadConfig.QueryDefinition;
import org.neo4j.driver.Driver;
import org.neo4j.driver.QueryConfig;
import org.neo4j.driver.RoutingControl;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.Map;

/**
 * Executes queries using {@code driver.executeQuery()}, the convenience API
 * introduced in Neo4j Driver 5.8.
 *
 * <p>Under the hood this creates a managed transaction with automatic routing
 * detection — equivalent to {@link ManagedTransactionExecutor} — but uses a
 * different code path. Exposing it as a separate mode lets users benchmark the
 * API overhead difference.
 *
 * <p>Because {@code driver.executeQuery()} always creates its own internal
 * session, no session is needed by this executor. When a unit contains multiple
 * queries each is executed as a separate {@code driver.executeQuery()} call
 * (grouping is not supported by this API).
 *
 * <p>Routing is determined per-query: write queries use {@link RoutingControl#WRITE},
 * read queries use {@link RoutingControl#READ}.
 */
public class ExecuteQueryExecutor implements TransactionExecutor {

    @Override
    public void execute(Session session, Driver driver, QueryUnit unit, LoopyStats stats) {
        for (QueryDefinition query : unit.getQueries()) {
            executeSingle(driver, query, stats);
        }
    }

    private void executeSingle(Driver driver, QueryDefinition query, LoopyStats stats) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> params = query.generateParameters();
            RoutingControl routing = query.isWrite() ? RoutingControl.WRITE : RoutingControl.READ;
            driver.executableQuery(query.getCypher())
                  .withParameters(params)
                  .withConfig(QueryConfig.builder().withRouting(routing).build())
                  .execute();
            stats.recordQuery(query.getId(), System.currentTimeMillis() - start, query.isWrite());
            stats.recordTransaction();
        } catch (Neo4jException e) {
            stats.recordQueryError(query.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            stats.recordQueryError(query.getId(), e.getMessage());
            throw new RuntimeException("driver.executeQuery() failed: " + query.getId(), e);
        }
    }
}
