package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.infra.persistence.HikariPoolSettings;
import com.defistrategyarena.shared.infra.persistence.JdbcSettings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public record AppConfig(
        PersistenceMode persistenceMode,
        int httpPort,
        JdbcSettings jdbc,
        HikariPoolSettings hikari,
        long authSessionTtlSeconds) {

    public static final String PERSISTENCE_MODE = "persistence.mode";
    public static final String HTTP_PORT = "http.port";
    public static final String JDBC_DRIVER = "jdbc.driver";
    public static final String JDBC_URL = "jdbc.url";
    public static final String JDBC_USER = "jdbc.user";
    public static final String JDBC_PASSWORD = "jdbc.password";
    public static final String HIKARI_MAXIMUM_POOL_SIZE = "hikari.maximumPoolSize";
    public static final String HIKARI_MINIMUM_IDLE = "hikari.minimumIdle";
    public static final String HIKARI_CONNECTION_TIMEOUT_MS = "hikari.connectionTimeoutMs";
    public static final String HIKARI_IDLE_TIMEOUT_MS = "hikari.idleTimeoutMs";
    public static final String HIKARI_MAX_LIFETIME_MS = "hikari.maxLifetimeMs";
    public static final String HIKARI_POOL_NAME = "hikari.poolName";
    public static final String AUTH_SESSION_TTL_SECONDS = "auth.sessionTtlSeconds";

    private static final String RESOURCE = "/app.properties";
    private static final String RESOURCE_MISSING = "app.properties not found on classpath";
    private static final String RESOURCE_UNREADABLE = "failed to read app.properties";
    private static final String MODE_INVALID = "persistence.mode must be memory or postgres";
    private static final String LOAD_REQUEST_REQUIRED = "load request must not be null";
    private static final String MISSING_CONFIG_PREFIX = "missing config value for ";
    private static final String ENV_PREFIX = "DSA_";
    private static final char ENV_SEPARATOR = '_';
    private static final String PROPERTY_SEGMENT_SPLIT = "\\.";
    private static final Pattern CAMEL_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");
    private static final String CAMEL_REPLACEMENT = "$1_$2";

    public static AppConfig load() {
        return load(LoadRequest.fromEnvironment(System::getenv));
    }

    public static AppConfig load(LoadRequest request) {
        Objects.requireNonNull(request, LOAD_REQUEST_REQUIRED);
        Properties defaults = request.defaultsSupplier().get();
        ResolvedValues values = new ResolvedValues(defaults, request.envLookup());
        return new AppConfig(
                parseMode(new ModeText(values.require(new PropertyKey(PERSISTENCE_MODE)))),
                Integer.parseInt(values.require(new PropertyKey(HTTP_PORT))),
                JdbcSettings.create(
                        new JdbcSettings(
                                values.require(new PropertyKey(JDBC_DRIVER)),
                                values.require(new PropertyKey(JDBC_URL)),
                                values.require(new PropertyKey(JDBC_USER)),
                                values.require(new PropertyKey(JDBC_PASSWORD)))),
                HikariPoolSettings.create(
                        new HikariPoolSettings(
                                Integer.parseInt(values.require(new PropertyKey(HIKARI_MAXIMUM_POOL_SIZE))),
                                Integer.parseInt(values.require(new PropertyKey(HIKARI_MINIMUM_IDLE))),
                                Long.parseLong(values.require(new PropertyKey(HIKARI_CONNECTION_TIMEOUT_MS))),
                                Long.parseLong(values.require(new PropertyKey(HIKARI_IDLE_TIMEOUT_MS))),
                                Long.parseLong(values.require(new PropertyKey(HIKARI_MAX_LIFETIME_MS))),
                                values.require(new PropertyKey(HIKARI_POOL_NAME)))),
                Long.parseLong(values.require(new PropertyKey(AUTH_SESSION_TTL_SECONDS))));
    }

    public static AppConfig create(AppConfig draft) {
        return new AppConfig(
                draft.persistenceMode(),
                draft.httpPort(),
                draft.jdbc(),
                draft.hikari(),
                draft.authSessionTtlSeconds());
    }

    private static PersistenceMode parseMode(ModeText text) {
        try {
            return PersistenceMode.valueOf(text.value().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(MODE_INVALID, ex);
        }
    }

    public enum PersistenceMode {
        MEMORY,
        POSTGRES
    }

    public record LoadRequest(
            Function<String, String> envLookup, Supplier<Properties> defaultsSupplier) {

        public static LoadRequest fromEnvironment(Function<String, String> envLookup) {
            return new LoadRequest(envLookup, ClasspathProperties::load);
        }
    }

    private record ModeText(String value) {}

    private record PropertyKey(String value) {}

    private record CamelSegment(String value) {}

    static final class ClasspathProperties {
        private ClasspathProperties() {}

        static Properties load() {
            return loadFrom(new ResourceStream(RESOURCE));
        }

        static Properties loadFrom(ResourceStream resource) {
            try (InputStream stream = open(resource)) {
                Properties properties = new Properties();
                properties.load(stream);
                return properties;
            } catch (IOException ex) {
                throw new UncheckedIOException(RESOURCE_UNREADABLE, ex);
            }
        }

        private static InputStream open(ResourceStream resource) throws IOException {
            InputStream stream = AppConfig.class.getResourceAsStream(resource.path());
            if (stream == null) {
                throw new IOException(RESOURCE_MISSING);
            }
            return stream;
        }

        record ResourceStream(String path) {}
    }

    private static final class ResolvedValues {
        private final Properties defaults;
        private final Function<String, String> envLookup;

        private ResolvedValues(Properties defaults, Function<String, String> envLookup) {
            this.defaults = defaults;
            this.envLookup = envLookup;
        }

        private String require(PropertyKey propertyKey) {
            String resolved = resolve(propertyKey);
            if (resolved == null || resolved.isBlank()) {
                throw new IllegalStateException(MISSING_CONFIG_PREFIX + propertyKey.value());
            }
            return resolved;
        }

        private String resolve(PropertyKey propertyKey) {
            String fromEnv = envLookup.apply(toEnvName(propertyKey));
            if (fromEnv != null && !fromEnv.isBlank()) {
                return fromEnv;
            }
            return defaults.getProperty(propertyKey.value());
        }

        private static String toEnvName(PropertyKey propertyKey) {
            CamelSegment[] segments =
                    Arrays.stream(propertyKey.value().split(PROPERTY_SEGMENT_SPLIT))
                            .map(CamelSegment::new)
                            .toArray(CamelSegment[]::new);
            return new EnvNameBuilder(segments).build();
        }
    }

    private record EnvNameBuilder(CamelSegment[] segments) {

        private String build() {
            StringBuilder builder = new StringBuilder(ENV_PREFIX);
            boolean firstSegment = true;
            for (CamelSegment segment : segments) {
                if (!firstSegment) {
                    builder.append(ENV_SEPARATOR);
                }
                firstSegment = false;
                builder.append(transformSegment(segment));
            }
            return builder.toString();
        }

        private static String transformSegment(CamelSegment segment) {
            return CAMEL_BOUNDARY
                    .matcher(segment.value())
                    .replaceAll(CAMEL_REPLACEMENT)
                    .toUpperCase(Locale.ROOT);
        }
    }
}
