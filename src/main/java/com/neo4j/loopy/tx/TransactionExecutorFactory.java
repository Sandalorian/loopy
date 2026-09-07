package com.neo4j.loopy.tx;

import com.neo4j.loopy.TransactionMode;

/**
 * Factory that creates the appropriate {@link TransactionExecutor} for a given
 * {@link TransactionMode}.
 */
public final class TransactionExecutorFactory {

    private TransactionExecutorFactory() {
    }

    public static TransactionExecutor create(TransactionMode mode) {
        return switch (mode) {
            case AUTO_COMMIT   -> new AutoCommitExecutor();
            case EXPLICIT      -> new ExplicitTransactionExecutor();
            case MANAGED_READ  -> new ManagedTransactionExecutor(true);
            case MANAGED_WRITE -> new ManagedTransactionExecutor(false);
            case EXECUTE_QUERY -> new ExecuteQueryExecutor();
        };
    }
}
