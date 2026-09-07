package com.neo4j.loopy.tx;

import com.neo4j.loopy.LoopyStats;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

/**
 * Strategy interface for executing a {@link QueryUnit} against Neo4j using a
 * specific transaction mode.
 *
 * <p>Implementations are responsible for consuming all result rows and recording
 * statistics (including errors and retries) via the provided {@link LoopyStats}.
 * After recording the error an implementation should re-throw so that the calling
 * worker can apply backoff / fail-fast logic.
 */
public interface TransactionExecutor {

    /**
     * Execute the given query unit and record statistics.
     *
     * @param session the active session — may be {@code null} when
     *                the mode does not require a session (e.g. execute-query)
     * @param driver  the Neo4j driver, used directly by execute-query mode
     * @param unit    the unit of work to execute
     * @param stats   statistics collector
     */
    void execute(Session session, Driver driver, QueryUnit unit, LoopyStats stats);
}
