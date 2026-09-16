package com.defistrategyarena.shared.infra.messaging;

import com.defistrategyarena.shared.messaging.TransactionRunner;
import java.util.Objects;
import org.jooq.DSLContext;

public final class JooqTransactionRunner implements TransactionRunner {

    private static final String DEPS_REQUIRED = "jooq transaction runner deps must not be null";

    private final DSLContext dsl;

    public JooqTransactionRunner(JooqTransactionRunnerDeps deps) {
        Objects.requireNonNull(deps, DEPS_REQUIRED);
        this.dsl = deps.dsl();
    }

    @Override
    public void run(UnitOfWork work) {
        dsl.transaction(configuration -> work.execute());
    }

    public record JooqTransactionRunnerDeps(DSLContext dsl) {}
}
