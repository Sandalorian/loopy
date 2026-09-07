package com.neo4j.loopy.tx;

import com.neo4j.loopy.TransactionMode;
import com.neo4j.loopy.config.CypherWorkloadConfig.QueryDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a unit of work to be executed within a single transaction.
 * A unit may contain one query (auto-commit / execute-query modes) or multiple
 * queries that should be grouped inside one explicit or managed transaction.
 *
 * <p>The optional {@link #getModeOverride()} takes precedence over the global
 * transaction mode; if absent the global mode is used via {@link #resolveMode}.
 */
public class QueryUnit {

    private final String id;
    private final List<QueryDefinition> queries;
    private final TransactionMode modeOverride;

    private QueryUnit(String id, List<QueryDefinition> queries, TransactionMode modeOverride) {
        this.id = id;
        this.queries = Collections.unmodifiableList(new ArrayList<>(queries));
        this.modeOverride = modeOverride;
    }

    /**
     * Create a single-query unit for a YAML-defined query.
     */
    public static QueryUnit single(QueryDefinition query, TransactionMode modeOverride) {
        return new QueryUnit(query.getId(), List.of(query), modeOverride);
    }

    /**
     * Create a grouped unit for executing multiple queries in the same transaction.
     */
    public static QueryUnit group(String groupId, List<QueryDefinition> queries, TransactionMode modeOverride) {
        if (queries == null || queries.isEmpty()) {
            throw new IllegalArgumentException(
                "Transaction group must have at least one query: " + groupId);
        }
        return new QueryUnit(groupId, queries, modeOverride);
    }

    public String getId() {
        return id;
    }

    public List<QueryDefinition> getQueries() {
        return queries;
    }

    /** Per-query transaction mode override, or {@code null} if the global mode should be used. */
    public TransactionMode getModeOverride() {
        return modeOverride;
    }

    public boolean isSingleQuery() {
        return queries.size() == 1;
    }

    /**
     * Resolve the effective transaction mode, falling back to {@code globalMode} when
     * no per-unit override is set.
     */
    public TransactionMode resolveMode(TransactionMode globalMode) {
        return modeOverride != null ? modeOverride : globalMode;
    }
}
