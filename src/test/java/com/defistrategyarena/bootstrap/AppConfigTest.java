package com.defistrategyarena.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.bootstrap.AppConfig.LoadRequest;
import com.defistrategyarena.bootstrap.AppConfig.PersistenceMode;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    private static final String MODE_POSTGRES = "postgres";
    private static final String PORT_9090 = "9090";
    private static final int PORT_9090_INT = 9090;
    private static final int DEFAULT_PORT = 8080;
    private static final String JDBC_DRIVER = "org.postgresql.Driver";
    private static final String JDBC_URL = "jdbc:postgresql://db:5432/dsa";
    private static final String JDBC_USER = "compose-user";
    private static final String JDBC_PASSWORD = "compose-pass";
    private static final String POOL_SIZE = "8";
    private static final int POOL_SIZE_INT = 8;
    private static final int DEFAULT_POOL = 5;
    private static final int DEFAULT_IDLE = 1;
    private static final long DEFAULT_TIMEOUT = 5000L;
    private static final long DEFAULT_IDLE_MS = 600000L;
    private static final long DEFAULT_LIFE_MS = 1800000L;

    @Test
    void loads_defaults_from_classpath_properties() {
        AppConfig config = AppConfig.load(LoadRequest.fromEnvironment(key -> null));
        assertEquals(PersistenceMode.MEMORY, config.persistenceMode());
        assertEquals(DEFAULT_PORT, config.httpPort());
        assertEquals(JDBC_DRIVER, config.jdbc().driver());
        assertEquals("jdbc:postgresql://localhost:5432/dsa", config.jdbc().url());
        assertEquals("dsa", config.jdbc().user());
        assertEquals("dsa", config.jdbc().password());
        assertEquals(DEFAULT_POOL, config.hikari().maximumPoolSize());
        assertEquals(DEFAULT_IDLE, config.hikari().minimumIdle());
        assertEquals(DEFAULT_TIMEOUT, config.hikari().connectionTimeoutMs());
        assertEquals(DEFAULT_IDLE_MS, config.hikari().idleTimeoutMs());
        assertEquals(DEFAULT_LIFE_MS, config.hikari().maxLifetimeMs());
        assertEquals("dsa-strategy", config.hikari().poolName());
    }

    @Test
    void load_without_request_uses_environment() {
        AppConfig config = AppConfig.load();
        assertEquals(PersistenceMode.MEMORY, config.persistenceMode());
    }

    @Test
    void env_overrides_win_over_properties() {
        Map<String, String> env = new HashMap<>();
        env.put("DSA_PERSISTENCE_MODE", MODE_POSTGRES);
        env.put("DSA_HTTP_PORT", PORT_9090);
        env.put("DSA_JDBC_DRIVER", JDBC_DRIVER);
        env.put("DSA_JDBC_URL", JDBC_URL);
        env.put("DSA_JDBC_USER", JDBC_USER);
        env.put("DSA_JDBC_PASSWORD", JDBC_PASSWORD);
        env.put("DSA_HIKARI_MAXIMUM_POOL_SIZE", POOL_SIZE);
        AppConfig config = AppConfig.load(LoadRequest.fromEnvironment(env::get));
        assertEquals(PersistenceMode.POSTGRES, config.persistenceMode());
        assertEquals(PORT_9090_INT, config.httpPort());
        assertEquals(JDBC_URL, config.jdbc().url());
        assertEquals(JDBC_USER, config.jdbc().user());
        assertEquals(JDBC_PASSWORD, config.jdbc().password());
        assertEquals(POOL_SIZE_INT, config.hikari().maximumPoolSize());
    }

    @Test
    void blank_env_does_not_override() {
        AppConfig config = AppConfig.load(LoadRequest.fromEnvironment(key -> " "));
        assertEquals(PersistenceMode.MEMORY, config.persistenceMode());
    }

    @Test
    void rejects_unknown_persistence_mode() {
        Map<String, String> env = Map.of("DSA_PERSISTENCE_MODE", "oracle");
        assertThrows(
                IllegalArgumentException.class,
                () -> AppConfig.load(LoadRequest.fromEnvironment(env::get)));
    }

    @Test
    void rejects_missing_property_value() {
        Properties empty = new Properties();
        assertThrows(
                IllegalStateException.class,
                () ->
                        AppConfig.load(
                                new LoadRequest(key -> null, () -> empty)));
    }

    @Test
    void rejects_blank_property_value() {
        Properties blankMode = new Properties();
        blankMode.setProperty(AppConfig.PERSISTENCE_MODE, " ");
        assertThrows(
                IllegalStateException.class,
                () -> AppConfig.load(new LoadRequest(key -> null, () -> blankMode)));
    }

    @Test
    void classpath_properties_reject_missing_resource() {
        assertThrows(
                UncheckedIOException.class,
                () ->
                        AppConfig.ClasspathProperties.loadFrom(
                                new AppConfig.ClasspathProperties.ResourceStream("/missing-app.properties")));
    }

    @Test
    void create_copies_draft() {
        AppConfig draft = AppConfig.load(LoadRequest.fromEnvironment(key -> null));
        AppConfig copied = AppConfig.create(draft);
        assertEquals(draft, copied);
    }
}
