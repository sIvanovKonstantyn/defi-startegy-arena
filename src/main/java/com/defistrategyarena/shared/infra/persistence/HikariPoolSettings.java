package com.defistrategyarena.shared.infra.persistence;

public record HikariPoolSettings(
        int maximumPoolSize,
        int minimumIdle,
        long connectionTimeoutMs,
        long idleTimeoutMs,
        long maxLifetimeMs,
        String poolName) {

    private static final String POOL_NAME_REQUIRED = "hikari pool name must not be blank";
    private static final String MAX_POOL_INVALID = "hikari maximum pool size must be at least 1";
    private static final String MIN_IDLE_INVALID = "hikari minimum idle must be zero or greater";
    private static final String CONNECTION_TIMEOUT_INVALID = "hikari connection timeout must be positive";
    private static final String IDLE_TIMEOUT_INVALID = "hikari idle timeout must be positive";
    private static final String MAX_LIFETIME_INVALID = "hikari max lifetime must be positive";
    private static final int MIN_POOL_SIZE = 1;
    private static final int MIN_IDLE = 0;
    private static final long MIN_TIMEOUT_MS = 1L;

    public HikariPoolSettings {
        if (maximumPoolSize < MIN_POOL_SIZE) {
            throw new IllegalArgumentException(MAX_POOL_INVALID);
        }
        if (minimumIdle < MIN_IDLE) {
            throw new IllegalArgumentException(MIN_IDLE_INVALID);
        }
        if (connectionTimeoutMs < MIN_TIMEOUT_MS) {
            throw new IllegalArgumentException(CONNECTION_TIMEOUT_INVALID);
        }
        if (idleTimeoutMs < MIN_TIMEOUT_MS) {
            throw new IllegalArgumentException(IDLE_TIMEOUT_INVALID);
        }
        if (maxLifetimeMs < MIN_TIMEOUT_MS) {
            throw new IllegalArgumentException(MAX_LIFETIME_INVALID);
        }
        if (poolName == null || poolName.isBlank()) {
            throw new IllegalArgumentException(POOL_NAME_REQUIRED);
        }
    }

    public static HikariPoolSettings create(HikariPoolSettings draft) {
        return new HikariPoolSettings(
                draft.maximumPoolSize(),
                draft.minimumIdle(),
                draft.connectionTimeoutMs(),
                draft.idleTimeoutMs(),
                draft.maxLifetimeMs(),
                draft.poolName());
    }
}
