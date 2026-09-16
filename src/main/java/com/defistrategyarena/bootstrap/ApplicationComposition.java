package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.infra.persistence.FlywayMigrator;
import com.defistrategyarena.shared.infra.persistence.HikariDataSourceFactory;
import com.defistrategyarena.shared.infra.persistence.JooqDslContextFactory;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.shared.messaging.InMemoryDomainEventPublisher;
import com.defistrategyarena.strategy.adapter.persistence.InMemoryStrategyRepository;
import com.defistrategyarena.strategy.adapter.persistence.JooqStrategyRepository;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.DeleteStrategy;
import com.defistrategyarena.strategy.application.GetStrategy;
import com.defistrategyarena.strategy.application.ListStrategies;
import com.defistrategyarena.strategy.application.StrategyRepository;
import com.defistrategyarena.strategy.application.UpdateStrategy;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Objects;
import java.util.Optional;

public final class ApplicationComposition implements AutoCloseable {

    private static final String CONFIG_REQUIRED = "app config must not be null";

    private final StrategyRepository strategies;
    private final DomainEventPublisher events;
    private final StrategyRestAdapter strategyHttp;
    private final Optional<HikariDataSource> dataSource;

    private ApplicationComposition(CompositionParts parts) {
        this.strategies = parts.strategies();
        this.events = parts.events();
        this.strategyHttp = parts.strategyHttp();
        this.dataSource = parts.dataSource();
    }

    public static ApplicationComposition createDefault() {
        return createMemory();
    }

    public static ApplicationComposition create(AppConfig config) {
        Objects.requireNonNull(config, CONFIG_REQUIRED);
        return switch (config.persistenceMode()) {
            case MEMORY -> createMemory();
            case POSTGRES -> createPostgres(config);
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

    @Override
    public void close() {
        dataSource.ifPresent(HikariDataSource::close);
    }

    private static ApplicationComposition createMemory() {
        return wire(new CompositionSeed(new InMemoryStrategyRepository(), Optional.empty()));
    }

    private static ApplicationComposition createPostgres(AppConfig config) {
        HikariDataSource dataSource =
                HikariDataSourceFactory.create(
                        new HikariDataSourceFactory.DataSourceFactoryInput(
                                config.jdbc(), config.hikari()));
        FlywayMigrator.migrate(dataSource);
        StrategyRepository strategies =
                new JooqStrategyRepository(JooqDslContextFactory.create(dataSource));
        return wire(new CompositionSeed(strategies, Optional.of(dataSource)));
    }

    private static ApplicationComposition wire(CompositionSeed seed) {
        DomainEventPublisher events = new InMemoryDomainEventPublisher();
        CreateStrategy createStrategy =
                new CreateStrategy(new CreateStrategy.CreateStrategyDeps(seed.strategies(), events));
        ListStrategies listStrategies =
                new ListStrategies(new ListStrategies.ListStrategiesDeps(seed.strategies()));
        GetStrategy getStrategy = new GetStrategy(new GetStrategy.GetStrategyDeps(seed.strategies()));
        UpdateStrategy updateStrategy =
                new UpdateStrategy(new UpdateStrategy.UpdateStrategyDeps(seed.strategies(), events));
        DeleteStrategy deleteStrategy =
                new DeleteStrategy(new DeleteStrategy.DeleteStrategyDeps(seed.strategies()));
        StrategyRestAdapter strategyHttp =
                new StrategyRestAdapter(
                        new StrategyRestAdapter.StrategyRestAdapterDeps(
                                createStrategy,
                                listStrategies,
                                getStrategy,
                                updateStrategy,
                                deleteStrategy));
        return new ApplicationComposition(
                new CompositionParts(seed.strategies(), events, strategyHttp, seed.dataSource()));
    }

    private record CompositionSeed(
            StrategyRepository strategies, Optional<HikariDataSource> dataSource) {}

    private record CompositionParts(
            StrategyRepository strategies,
            DomainEventPublisher events,
            StrategyRestAdapter strategyHttp,
            Optional<HikariDataSource> dataSource) {}
}
