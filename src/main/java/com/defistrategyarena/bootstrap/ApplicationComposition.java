package com.defistrategyarena.bootstrap;

import com.defistrategyarena.identity.adapter.web.IdentityRestAdapter;
import com.defistrategyarena.identity.application.GetCurrentUser;
import com.defistrategyarena.shared.infra.http.WebSocketBinding;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.shared.messaging.OutboxRelay;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.application.StrategyRepository;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Objects;
import java.util.Optional;

public final class ApplicationComposition implements AutoCloseable {

    private static final String CONFIG_REQUIRED = "app config must not be null";

    private final ApplicationServices services;
    private final Optional<HikariDataSource> dataSource;

    ApplicationComposition(CompositionParts parts) {
        this.services =
                new ApplicationServices(
                        parts.strategies(),
                        parts.events(),
                        parts.strategyHttp(),
                        parts.identityHttp(),
                        parts.getCurrentUser(),
                        parts.strategyPublisher(),
                        parts.outboxRelay(),
                        parts.webSocketBinding());
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
        return services.strategies();
    }

    public DomainEventPublisher events() {
        return services.events();
    }

    public StrategyRestAdapter strategyHttp() {
        return services.strategyHttp();
    }

    public IdentityRestAdapter identityHttp() {
        return services.identityHttp();
    }

    public GetCurrentUser getCurrentUser() {
        return services.getCurrentUser();
    }

    public AuthenticatedStrategyPublisher strategyPublisher() {
        return services.strategyPublisher();
    }

    public OutboxRelay outboxRelay() {
        return services.outboxRelay();
    }

    public WebSocketBinding webSocketBinding() {
        return services.webSocketBinding();
    }

    @Override
    public void close() {
        dataSource.ifPresent(HikariDataSource::close);
    }

    private record ApplicationServices(
            StrategyRepository strategies,
            DomainEventPublisher events,
            StrategyRestAdapter strategyHttp,
            IdentityRestAdapter identityHttp,
            GetCurrentUser getCurrentUser,
            AuthenticatedStrategyPublisher strategyPublisher,
            OutboxRelay outboxRelay,
            WebSocketBinding webSocketBinding) {}

    record CompositionParts(
            StrategyRepository strategies,
            DomainEventPublisher events,
            StrategyRestAdapter strategyHttp,
            IdentityRestAdapter identityHttp,
            GetCurrentUser getCurrentUser,
            AuthenticatedStrategyPublisher strategyPublisher,
            OutboxRelay outboxRelay,
            WebSocketBinding webSocketBinding,
            Optional<HikariDataSource> dataSource) {}
}
