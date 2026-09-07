package com.neo4j.loopy;

/**
 * Defines the Neo4j transaction mode to use when executing queries.
 *
 * <ul>
 *   <li>{@link #AUTO_COMMIT} — {@code session.run()}. Single query per call, no retry,
 *       minimal overhead. The driver sends whatever result (or error) it gets.</li>
 *   <li>{@link #EXPLICIT} — {@code session.beginTransaction()} / {@code tx.commit()}.
 *       Full programmatic control; multiple queries can share one transaction.
 *       No driver-level retry — the application decides what to do on error.</li>
 *   <li>{@link #MANAGED_READ} — {@code session.executeRead(cb)}. Driver-managed retry
 *       on transient errors; routes to a read server.</li>
 *   <li>{@link #MANAGED_WRITE} — {@code session.executeWrite(cb)}. Driver-managed retry
 *       on transient errors; routes to a write server.</li>
 *   <li>{@link #EXECUTE_QUERY} — {@code driver.executeQuery()}. Convenience API
 *       (available since driver 5.8) that creates a managed transaction with automatic
 *       routing detection. Functionally equivalent to managed-write, but uses a different
 *       code path — useful for benchmarking API overhead.</li>
 * </ul>
 */
public enum TransactionMode {

    AUTO_COMMIT("auto-commit"),
    EXPLICIT("explicit"),
    MANAGED_READ("managed-read"),
    MANAGED_WRITE("managed-write"),
    EXECUTE_QUERY("execute-query");

    private final String value;

    TransactionMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parse a transaction mode from a string value, case-insensitive.
     * Returns {@link #AUTO_COMMIT} when {@code value} is {@code null}.
     *
     * @throws IllegalArgumentException if the value does not match any known mode
     */
    public static TransactionMode fromString(String value) {
        if (value == null) {
            return AUTO_COMMIT;
        }
        for (TransactionMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException(
            "Unknown transaction mode: '" + value + "'. Valid modes: " +
            "auto-commit, explicit, managed-read, managed-write, execute-query"
        );
    }

    @Override
    public String toString() {
        return value;
    }
}
