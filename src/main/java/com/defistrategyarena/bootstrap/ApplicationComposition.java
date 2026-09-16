package com.defistrategyarena.bootstrap;

import com.defistrategyarena.identity.adapter.crypto.Pbkdf2PasswordHasher;
import com.defistrategyarena.identity.adapter.crypto.SecureSessionTokenFactory;
import com.defistrategyarena.identity.adapter.persistence.InMemorySessionRepository;
import com.defistrategyarena.identity.adapter.persistence.InMemoryUserRepository;
import com.defistrategyarena.identity.adapter.web.IdentityRestAdapter;
import com.defistrategyarena.identity.application.GetCurrentUser;
import com.defistrategyarena.identity.application.LoginWithPassword;
import com.defistrategyarena.identity.application.Logout;
import com.defistrategyarena.identity.application.PasswordHasher;
import com.defistrategyarena.identity.application.RegisterWithPassword;
import com.defistrategyarena.identity.application.SessionIssuer;
import com.defistrategyarena.identity.application.SessionRepository;
import com.defistrategyarena.identity.application.UserRepository;
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
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public final class ApplicationComposition implements AutoCloseable {

    private static final String CONFIG_REQUIRED = "app config must not be null";

    private final StrategyRepository strategies;
    private final DomainEventPublisher events;
    private final StrategyRestAdapter strategyHttp;
    private final IdentityRestAdapter identityHttp;
    private final Optional<HikariDataSource> dataSource;

    private ApplicationComposition(CompositionParts parts) {
        this.strategies = parts.strategies();
        this.events = parts.events();
        this.strategyHttp = parts.strategyHttp();
        this.identityHttp = parts.identityHttp();
        this.dataSource = parts.dataSource();
    }

    public static ApplicationComposition createDefault() {
        return create(AppConfig.load(AppConfig.LoadRequest.fromEnvironment(key -> null)));
    }

    public static ApplicationComposition create(AppConfig config) {
        Objects.requireNonNull(config, CONFIG_REQUIRED);
        return switch (config.persistenceMode()) {
            case MEMORY -> createMemory(config);
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

    public IdentityRestAdapter identityHttp() {
        return identityHttp;
    }

    @Override
    public void close() {
        dataSource.ifPresent(HikariDataSource::close);
    }

    private static ApplicationComposition createMemory(AppConfig config) {
        return wire(
                new CompositionSeed(
                        new InMemoryStrategyRepository(),
                        identitySeed(new IdentitySeedConfig(config.authSessionTtlSeconds())),
                        Optional.empty()));
    }

    private static ApplicationComposition createPostgres(AppConfig config) {
        HikariDataSource dataSource =
                HikariDataSourceFactory.create(
                        new HikariDataSourceFactory.DataSourceFactoryInput(
                                config.jdbc(), config.hikari()));
        FlywayMigrator.migrate(dataSource);
        StrategyRepository strategies =
                new JooqStrategyRepository(JooqDslContextFactory.create(dataSource));
        return wire(
                new CompositionSeed(
                        strategies,
                        identitySeed(new IdentitySeedConfig(config.authSessionTtlSeconds())),
                        Optional.of(dataSource)));
    }

    private static IdentitySeed identitySeed(IdentitySeedConfig config) {
        UserRepository users = new InMemoryUserRepository();
        SessionRepository sessions = new InMemorySessionRepository();
        PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        com.defistrategyarena.identity.application.SessionTokenFactory tokens =
                new SecureSessionTokenFactory();
        Clock clock = Clock.systemUTC();
        return new IdentitySeed(
                users, sessions, passwordHasher, tokens, clock, config.sessionTtlSeconds());
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
        IdentityRestAdapter identityHttp = wireIdentity(seed.identity());
        return new ApplicationComposition(
                new CompositionParts(
                        seed.strategies(),
                        events,
                        strategyHttp,
                        identityHttp,
                        seed.dataSource()));
    }

    private static IdentityRestAdapter wireIdentity(IdentitySeed identity) {
        SessionIssuer sessionIssuer =
                new SessionIssuer(
                        new SessionIssuer.SessionIssuerDeps(
                                identity.sessions(),
                                identity.tokens(),
                                identity.clock(),
                                identity.sessionTtlSeconds()));
        RegisterWithPassword registerWithPassword =
                new RegisterWithPassword(
                        new RegisterWithPassword.RegisterWithPasswordDeps(
                                identity.users(), identity.passwordHasher(), sessionIssuer));
        LoginWithPassword loginWithPassword =
                new LoginWithPassword(
                        new LoginWithPassword.LoginWithPasswordDeps(
                                identity.users(), identity.passwordHasher(), sessionIssuer));
        Logout logout =
                new Logout(
                        new Logout.LogoutDeps(
                                identity.sessions(), identity.tokens(), identity.clock()));
        GetCurrentUser getCurrentUser =
                new GetCurrentUser(
                        new GetCurrentUser.GetCurrentUserDeps(
                                identity.sessions(),
                                identity.users(),
                                identity.tokens(),
                                identity.clock()));
        return new IdentityRestAdapter(
                new IdentityRestAdapter.IdentityRestAdapterDeps(
                        registerWithPassword, loginWithPassword, logout, getCurrentUser));
    }

    private record IdentitySeed(
            UserRepository users,
            SessionRepository sessions,
            PasswordHasher passwordHasher,
            com.defistrategyarena.identity.application.SessionTokenFactory tokens,
            Clock clock,
            long sessionTtlSeconds) {}

    private record IdentitySeedConfig(long sessionTtlSeconds) {}

    private record CompositionSeed(
            StrategyRepository strategies,
            IdentitySeed identity,
            Optional<HikariDataSource> dataSource) {}

    private record CompositionParts(
            StrategyRepository strategies,
            DomainEventPublisher events,
            StrategyRestAdapter strategyHttp,
            IdentityRestAdapter identityHttp,
            Optional<HikariDataSource> dataSource) {}
}
