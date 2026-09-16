package com.defistrategyarena.bootstrap;

import com.defistrategyarena.identity.application.AccessTokenQuery;
import com.defistrategyarena.identity.application.GetCurrentUser;
import com.defistrategyarena.identity.application.UnauthorizedException;
import com.defistrategyarena.identity.domain.User;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.messaging.DomainEvent;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.shared.messaging.OutboxRelay;
import java.util.Objects;
import java.util.UUID;

final class AuthenticatedStrategyPublisher {

    private static final String DEPS_REQUIRED = "authenticated strategy publisher deps must not be null";

    private final GetCurrentUser getCurrentUser;
    private final DomainEventPublisher events;
    private final OutboxRelay outboxRelay;

    AuthenticatedStrategyPublisher(AuthenticatedStrategyPublisherDeps deps) {
        Objects.requireNonNull(deps, DEPS_REQUIRED);
        this.getCurrentUser = deps.getCurrentUser();
        this.events = deps.events();
        this.outboxRelay = deps.outboxRelay();
    }

    AcceptedHttpResponse publish(AuthorizedPublish command) {
        try {
            User user =
                    getCurrentUser.execute(
                            new AccessTokenQuery(
                                    BearerAccessToken.requireValid(command.request()).accessToken()));
            String correlationId = UUID.randomUUID().toString();
            events.publish(command.eventFactory().create(new EventAuthContext(correlationId, user.id().value())));
            outboxRelay.drain();
            return new AcceptedHttpResponse(command.acceptedStatus(), correlationId);
        } catch (UnauthorizedException | IllegalArgumentException exception) {
            return AcceptedHttpResponse.empty(new AcceptedHttpResponse.StatusCode(command.unauthorizedStatus()));
        }
    }

    @FunctionalInterface
    interface EventFactory {
        DomainEvent create(EventAuthContext context);
    }

    record EventAuthContext(String correlationId, String ownerId) {}

    record AuthorizedPublish(
            HttpRequest request,
            EventFactory eventFactory,
            int acceptedStatus,
            int unauthorizedStatus) {}

    record AuthenticatedStrategyPublisherDeps(
            GetCurrentUser getCurrentUser, DomainEventPublisher events, OutboxRelay outboxRelay) {}
}
