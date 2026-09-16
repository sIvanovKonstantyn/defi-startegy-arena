package com.defistrategyarena.strategy.integration;

import com.defistrategyarena.shared.infra.persistence.FlywayMigrator;
import com.defistrategyarena.shared.infra.persistence.HikariDataSourceFactory;
import com.defistrategyarena.shared.infra.persistence.HikariPoolSettings;
import com.defistrategyarena.shared.infra.persistence.JdbcSettings;
import com.defistrategyarena.shared.infra.persistence.JooqDslContextFactory;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.jooq.DSLContext;

public final class H2PostgresModeSupport {

    public static final String DRIVER = "org.h2.Driver";
    public static final String USER = "sa";
    public static final String PASSWORD = "";
    private static final String URL_PREFIX =
            "jdbc:h2:mem:";
    private static final String URL_SUFFIX =
            ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
    private static final int POOL_SIZE = 2;
    private static final int MIN_IDLE = 1;
    private static final long TIMEOUT_MS = 5000L;
    private static final long IDLE_MS = 600000L;
    private static final long LIFE_MS = 1800000L;
    private static final String POOL_NAME = "dsa-h2-it";

    private H2PostgresModeSupport() {}

    public static JdbcSettings jdbcSettings(DatabaseName name) {
        return JdbcSettings.create(
                new JdbcSettings(DRIVER, URL_PREFIX + name.value() + URL_SUFFIX, USER, PASSWORD));
    }

    public static HikariDataSource dataSource(DatabaseName name) {
        return HikariDataSourceFactory.create(
                new HikariDataSourceFactory.DataSourceFactoryInput(
                        jdbcSettings(name),
                        HikariPoolSettings.create(
                                new HikariPoolSettings(
                                        POOL_SIZE, MIN_IDLE, TIMEOUT_MS, IDLE_MS, LIFE_MS, POOL_NAME))));
    }

    public static DSLContext migratedDsl(DataSource dataSource) {
        FlywayMigrator.migrate(dataSource);
        return JooqDslContextFactory.create(dataSource);
    }

    public record DatabaseName(String value) {}
}
