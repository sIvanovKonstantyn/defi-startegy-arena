package com.defistrategyarena.shared.infra.persistence;

import java.util.Objects;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

public final class JooqDslContextFactory {

    private static final String DATA_SOURCE_REQUIRED = "data source must not be null";

    private JooqDslContextFactory() {}

    public static DSLContext create(DataSource dataSource) {
        Objects.requireNonNull(dataSource, DATA_SOURCE_REQUIRED);
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
