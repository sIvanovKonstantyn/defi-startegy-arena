package com.defistrategyarena.shared.infra.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Objects;

public final class HikariDataSourceFactory {

    private static final String SETTINGS_REQUIRED = "data source settings must not be null";

    private HikariDataSourceFactory() {}

    public static HikariDataSource create(DataSourceFactoryInput input) {
        Objects.requireNonNull(input, SETTINGS_REQUIRED);
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(input.jdbc().driver());
        config.setJdbcUrl(input.jdbc().url());
        config.setUsername(input.jdbc().user());
        config.setPassword(input.jdbc().password());
        config.setMaximumPoolSize(input.pool().maximumPoolSize());
        config.setMinimumIdle(input.pool().minimumIdle());
        config.setConnectionTimeout(input.pool().connectionTimeoutMs());
        config.setIdleTimeout(input.pool().idleTimeoutMs());
        config.setMaxLifetime(input.pool().maxLifetimeMs());
        config.setPoolName(input.pool().poolName());
        return new HikariDataSource(config);
    }

    public record DataSourceFactoryInput(JdbcSettings jdbc, HikariPoolSettings pool) {}
}
