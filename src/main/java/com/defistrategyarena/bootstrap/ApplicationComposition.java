package com.defistrategyarena.bootstrap;

import com.defistrategyarena.identity.adapter.web.IdentityRestAdapter;
import com.defistrategyarena.identity.application.GetCurrentUser;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.shared.messaging.OutboxRelay;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.application.StrategyRepository;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Objects;
import java.util.Optional;

public final class ApplicationComposition implements AutoCloseable {

    private static final String CONFIG_REQUIRED = "app config must not be null";

    private final StrategyRepository strategies;
    private final DomainEventPublisher events;
    private final StrategyRestAdapter strategyHttp;
    private final IdentityRestAdapter identityHttp;
    private final GetCurrentUser getCurrentUser;
    private final AuthenticatedStrategyPublisher strategyPublisher;
    private final OutboxRelay outboxRelay;
    private final Optional<HikariDataSource> dataSource;

    ApplicationComposition(CompositionParts parts) {
        this.strategies = parts.strategies();
        this.events = parts.events();
        this.strategyHttp = parts.strategyHttp();
        this.identityHttp = parts.identityHttp();
        this.getCurrentUser = parts.getCurrentUser();
        this.strategyPublisher = parts.strategyPublisher();
        this.outboxRelay = parts.outboxRelay();
        this.dataSource = parts.dataSource();
    }

    public static ApplicationComposition createDefault() {
        return create(AppConfig.load(AppConfig.LoadRequest.fromEnvironment(key -> null)));
    }

    public static ApplicationComposition create(AppConfig config) {
        Objects.requireNonNull(config, CONFIG_REQUIRED);
        return switch (config.persistenceMode()) {
            case MEMORY -> ApplicationCompositionFactory.createMemory(config);
            case POSTGRES -> ApplicationCompositionFactory.createPostgres(config);
        };
    }

    public StrategyRepository strategies() {
        return strategies;
    }

    public DomainEventPublisher events() {
        return events;
    }

    public StrategyRestAdapter strategyHttp() {
        return strategyHttp;
    }

    public IdentityRestAdapter identityHttp() {
        return identityHttp;
    }

    public GetCurrentUser getCurrentUser() {
        return getCurrentUser;
    }

    public AuthenticatedStrategyPublisher strategyPublisher() {
        return strategyPublisher;
    }

    public OutboxRelay outboxRelay() {
        return outboxRelay;
    }

    @Override
    public void close() {
        dataSource.ifPresent(HikariDataSource::close);
    }

    record CompositionParts(
            StrategyRepository strategies,
            DomainEventPublisher events,
            StrategyRestAdapter strategyHttp,
            IdentityRestAdapter identityHttp,
            GetCurrentUser getCurrentUser,
            AuthenticatedStrategyPublisher strategyPublisher,
            OutboxRelay outboxRelay,
            Optional<HikariDataSource> dataSource) {}
}
