package com.defistrategyarena.shared.infra.persistence;

import java.util.Objects;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

public final class FlywayMigrator {

    private static final String DATA_SOURCE_REQUIRED = "data source must not be null";
    private static final String LOCATIONS = "classpath:db/migration";

    private FlywayMigrator() {}

    public static void migrate(DataSource dataSource) {
        Objects.requireNonNull(dataSource, DATA_SOURCE_REQUIRED);
        Flyway.configure().dataSource(dataSource).locations(LOCATIONS).load().migrate();
    }
}
