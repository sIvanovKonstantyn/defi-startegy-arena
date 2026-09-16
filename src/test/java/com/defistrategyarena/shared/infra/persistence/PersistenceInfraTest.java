package com.defistrategyarena.shared.infra.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PersistenceInfraTest {

    private static final String DRIVER = "org.postgresql.Driver";
    private static final String URL = "jdbc:postgresql://localhost:5432/dsa";
    private static final String USER = "dsa";
    private static final String PASSWORD = "dsa";
    private static final String POOL = "pool";
    private static final int MAX_POOL = 5;
    private static final int MIN_IDLE = 1;
    private static final long TIMEOUT = 5000L;
    private static final long IDLE = 600000L;
    private static final long LIFE = 1800000L;
    private static final int ZERO = 0;
    private static final int NEGATIVE = -1;

    @Test
    void jdbc_and_hikari_settings_validate_and_copy() {
        JdbcSettings jdbc = JdbcSettings.create(new JdbcSettings(DRIVER, URL, USER, PASSWORD));
        assertEquals(URL, jdbc.url());
        assertEquals(DRIVER, jdbc.driver());
        HikariPoolSettings pool =
                HikariPoolSettings.create(
                        new HikariPoolSettings(MAX_POOL, MIN_IDLE, TIMEOUT, IDLE, LIFE, POOL));
        assertEquals(POOL, pool.poolName());
        assertThrows(
                IllegalArgumentException.class, () -> new JdbcSettings(" ", URL, USER, PASSWORD));
        assertThrows(
                IllegalArgumentException.class, () -> new JdbcSettings(null, URL, USER, PASSWORD));
        assertThrows(
                IllegalArgumentException.class, () -> new JdbcSettings(DRIVER, " ", USER, PASSWORD));
        assertThrows(
                IllegalArgumentException.class, () -> new JdbcSettings(DRIVER, null, USER, PASSWORD));
        assertThrows(
                IllegalArgumentException.class, () -> new JdbcSettings(DRIVER, URL, " ", PASSWORD));
        assertThrows(
                IllegalArgumentException.class, () -> new JdbcSettings(DRIVER, URL, null, PASSWORD));
        assertThrows(
                IllegalArgumentException.class, () -> new JdbcSettings(DRIVER, URL, USER, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HikariPoolSettings(ZERO, MIN_IDLE, TIMEOUT, IDLE, LIFE, POOL));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HikariPoolSettings(MAX_POOL, NEGATIVE, TIMEOUT, IDLE, LIFE, POOL));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HikariPoolSettings(MAX_POOL, MIN_IDLE, 0L, IDLE, LIFE, POOL));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HikariPoolSettings(MAX_POOL, MIN_IDLE, TIMEOUT, 0L, LIFE, POOL));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HikariPoolSettings(MAX_POOL, MIN_IDLE, TIMEOUT, IDLE, 0L, POOL));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HikariPoolSettings(MAX_POOL, MIN_IDLE, TIMEOUT, IDLE, LIFE, " "));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HikariPoolSettings(MAX_POOL, MIN_IDLE, TIMEOUT, IDLE, LIFE, null));
    }

    @Test
    void factories_reject_null_inputs() {
        assertThrows(NullPointerException.class, () -> HikariDataSourceFactory.create(null));
        assertThrows(NullPointerException.class, () -> JooqDslContextFactory.create(null));
        assertThrows(NullPointerException.class, () -> FlywayMigrator.migrate(null));
    }
}
