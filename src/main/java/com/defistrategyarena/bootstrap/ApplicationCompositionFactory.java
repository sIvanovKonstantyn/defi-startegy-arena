package com.defistrategyarena.bootstrap;

import com.defistrategyarena.identity.adapter.crypto.Pbkdf2PasswordHasher;
import com.defistrategyarena.identity.adapter.crypto.SecureSessionTokenFactory;
import com.defistrategyarena.identity.adapter.persistence.InMemorySessionRepository;
import com.defistrategyarena.identity.adapter.persistence.InMemoryUserRepository;
import com.defistrategyarena.identity.adapter.persistence.JooqSessionRepository;
import com.defistrategyarena.identity.adapter.persistence.JooqUserRepository;
import com.defistrategyarena.identity.adapter.web.IdentityRestAdapter;
import com.defistrategyarena.identity.application.GetCurrentUser;
import com.defistrategyarena.identity.application.LoginWithPassword;
import com.defistrategyarena.identity.application.Logout;
import com.defistrategyarena.identity.application.PasswordHasher;
import com.defistrategyarena.identity.application.RegisterWithPassword;
import com.defistrategyarena.identity.application.SessionIssuer;
import com.defistrategyarena.identity.application.SessionRepository;
import com.defistrategyarena.identity.application.SessionTokenFactory;
import com.defistrategyarena.identity.application.UserRepository;
import com.defistrategyarena.shared.events.strategy.StrategyEventTypes;
import com.defistrategyarena.shared.infra.messaging.JacksonDomainEventCodec;
import com.defistrategyarena.shared.infra.messaging.JooqInboxStore;
import com.defistrategyarena.shared.infra.messaging.JooqOutboxStore;
import com.defistrategyarena.shared.infra.messaging.JooqTransactionRunner;
import com.defistrategyarena.shared.infra.persistence.FlywayMigrator;
import com.defistrategyarena.shared.infra.persistence.HikariDataSourceFactory;
import com.defistrategyarena.shared.infra.persistence.JooqDslContextFactory;
import com.defistrategyarena.shared.infra.http.WebSocketBinding;
import com.defistrategyarena.shared.messaging.DomainEventCodec;
import com.defistrategyarena.shared.messaging.DomainEventListenerRegistry;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.shared.messaging.ImmediateTransactionRunner;
import com.defistrategyarena.shared.messaging.InMemoryInboxStore;
import com.defistrategyarena.shared.messaging.InMemoryOutboxStore;
import com.defistrategyarena.shared.messaging.InboxStore;
import com.defistrategyarena.shared.messaging.OutboxRelay;
import com.defistrategyarena.shared.messaging.OutboxStore;
import com.defistrategyarena.shared.messaging.TransactionRunner;
import com.defistrategyarena.shared.messaging.TransactionalOutboxPublisher;
import com.defistrategyarena.integration.InMemoryUserSessionHub;
import com.defistrategyarena.integration.StrategyResponseDeliveryListeners;
import com.defistrategyarena.strategy.adapter.messaging.StrategyRequestListeners;
import com.defistrategyarena.strategy.adapter.persistence.InMemoryStrategyRepository;
import com.defistrategyarena.strategy.adapter.persistence.JooqStrategyRepository;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.DeleteStrategy;
import com.defistrategyarena.strategy.application.GetStrategy;
import com.defistrategyarena.strategy.application.ListStrategies;
import com.defistrategyarena.strategy.application.StrategyRepository;
import com.defistrategyarena.strategy.application.StrategyUseCases;
import com.defistrategyarena.strategy.application.UpdateStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import java.util.Optional;
import org.jooq.DSLContext;

enum ApplicationCompositionFactory {
    ;

    static ApplicationComposition createMemory(AppConfig config) {
        MessagingStack messaging =
                messagingStack(
                        new MessagingStores(
                                new InMemoryOutboxStore(),
                                new InMemoryInboxStore(),
                                new ImmediateTransactionRunner()));
        return wire(
                new CompositionSeed(
                        new InMemoryStrategyRepository(),
                        identitySeed(new IdentitySeedConfig(config.authSessionTtlSeconds(), Optional.empty())),
                        messaging,
                        Optional.empty()));
    }

    static ApplicationComposition createPostgres(AppConfig config) {
        HikariDataSource dataSource =
                HikariDataSourceFactory.create(
                        new HikariDataSourceFactory.DataSourceFactoryInput(
                                config.jdbc(), config.hikari()));
        FlywayMigrator.migrate(dataSource);
        DSLContext dsl = JooqDslContextFactory.create(dataSource);
        MessagingStack messaging =
                messagingStack(
                        new MessagingStores(
                                new JooqOutboxStore(new JooqOutboxStore.JooqOutboxStoreDeps(dsl)),
                                new JooqInboxStore(new JooqInboxStore.JooqInboxStoreDeps(dsl)),
                                new JooqTransactionRunner(
                                        new JooqTransactionRunner.JooqTransactionRunnerDeps(dsl))));
        return wire(
                new CompositionSeed(
                        new JooqStrategyRepository(dsl),
                        identitySeed(
                                new IdentitySeedConfig(
                                        config.authSessionTtlSeconds(), Optional.of(dsl))),
                        messaging,
                        Optional.of(dataSource)));
    }

    private static MessagingStack messagingStack(MessagingStores stores) {
        DomainEventCodec codec =
                new JacksonDomainEventCodec(
                        new JacksonDomainEventCodec.JacksonDomainEventCodecDeps(
                                new ObjectMapper(), StrategyEventTypes.catalog()));
        DomainEventListenerRegistry registry = new DomainEventListenerRegistry();
        OutboxRelay relay =
                new OutboxRelay(
                        new OutboxRelay.OutboxRelayDeps(
                                stores.outbox(),
                                stores.inbox(),
                                codec,
                                registry,
                                stores.transactions()));
        DomainEventPublisher events =
                new TransactionalOutboxPublisher(
                        new TransactionalOutboxPublisher.TransactionalOutboxPublisherDeps(
                                stores.outbox(), codec));
        return new MessagingStack(events, relay, registry);
    }

    private static IdentitySeed identitySeed(IdentitySeedConfig config) {
        UserRepository users;
        SessionRepository sessions;
        if (config.dsl().isPresent()) {
            DSLContext dsl = config.dsl().orElseThrow();
            users = new JooqUserRepository(dsl);
            sessions = new JooqSessionRepository(dsl);
        } else {
            users = new InMemoryUserRepository();
            sessions = new InMemorySessionRepository();
        }
        PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        SessionTokenFactory tokens = new SecureSessionTokenFactory();
        Clock clock = Clock.systemUTC();
        return new IdentitySeed(
                users, sessions, passwordHasher, tokens, clock, config.sessionTtlSeconds());
    }

    private static ApplicationComposition wire(CompositionSeed seed) {
        StrategyUseCases useCases = strategyUseCases(seed);
        new StrategyRequestListeners(
                        new StrategyRequestListeners.StrategyRequestListenersDeps(
                                useCases, seed.messaging().events()))
                .register(seed.messaging().registry());
        WiredIdentity identity = wireIdentity(seed.identity());
        InMemoryUserSessionHub sessionHub = new InMemoryUserSessionHub();
        ObjectMapper objectMapper = new ObjectMapper();
        new StrategyResponseDeliveryListeners(
                        new StrategyResponseDeliveryListeners.StrategyResponseDeliveryListenersDeps(
                                sessionHub, objectMapper))
                .register(seed.messaging().registry());
        WebSocketBinding webSocketBinding =
                new WebSocketBinding(
                        new GetCurrentUserWebSocketAuth(
                                new GetCurrentUserWebSocketAuth.GetCurrentUserWebSocketAuthDeps(
                                        identity.getCurrentUser())),
                        sessionHub);
        AuthenticatedStrategyPublisher strategyPublisher =
                new AuthenticatedStrategyPublisher(
                        new AuthenticatedStrategyPublisher.AuthenticatedStrategyPublisherDeps(
                                identity.getCurrentUser(),
                                seed.messaging().events(),
                                seed.messaging().relay()));
        return new ApplicationComposition(
                new ApplicationComposition.CompositionParts(
                        seed.strategies(),
                        seed.messaging().events(),
                        new StrategyRestAdapter(
                                new StrategyRestAdapter.StrategyRestAdapterDeps(useCases)),
                        identity.identityHttp(),
                        identity.getCurrentUser(),
                        strategyPublisher,
                        seed.messaging().relay(),
                        webSocketBinding,
                        seed.dataSource()));
    }

    private static StrategyUseCases strategyUseCases(CompositionSeed seed) {
        return new StrategyUseCases(
                new CreateStrategy(
                        new CreateStrategy.CreateStrategyDeps(
                                seed.strategies(), seed.messaging().events())),
                new ListStrategies(new ListStrategies.ListStrategiesDeps(seed.strategies())),
                new GetStrategy(new GetStrategy.GetStrategyDeps(seed.strategies())),
                new UpdateStrategy(
                        new UpdateStrategy.UpdateStrategyDeps(
                                seed.strategies(), seed.messaging().events())),
                new DeleteStrategy(new DeleteStrategy.DeleteStrategyDeps(seed.strategies())));
    }

    private static WiredIdentity wireIdentity(IdentitySeed identity) {
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
        IdentityRestAdapter identityHttp =
                new IdentityRestAdapter(
                        new IdentityRestAdapter.IdentityRestAdapterDeps(
                                registerWithPassword, loginWithPassword, logout, getCurrentUser));
        return new WiredIdentity(identityHttp, getCurrentUser);
    }

    private record MessagingStores(OutboxStore outbox, InboxStore inbox, TransactionRunner transactions) {}

    private record MessagingStack(
            DomainEventPublisher events,
            OutboxRelay relay,
            DomainEventListenerRegistry registry) {}

    private record WiredIdentity(IdentityRestAdapter identityHttp, GetCurrentUser getCurrentUser) {}

    private record IdentitySeed(
            UserRepository users,
            SessionRepository sessions,
            PasswordHasher passwordHasher,
            SessionTokenFactory tokens,
            Clock clock,
            long sessionTtlSeconds) {}

    private record IdentitySeedConfig(long sessionTtlSeconds, Optional<DSLContext> dsl) {}

    private record CompositionSeed(
            StrategyRepository strategies,
            IdentitySeed identity,
            MessagingStack messaging,
            Optional<HikariDataSource> dataSource) {}
}
