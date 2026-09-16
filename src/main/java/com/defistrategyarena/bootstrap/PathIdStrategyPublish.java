package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.messaging.DomainEvent;

enum PathIdStrategyPublish {
    ;

    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_UNAUTHORIZED = 401;

    static AcceptedHttpResponse publish(PathIdPublishCommand command) {
        StrategyRouteParams.StrategyRoute route = StrategyRouteParams.from(command.request());
        return command
                .publisher()
                .publish(
                        new AuthenticatedStrategyPublisher.AuthorizedPublish(
                                command.request(),
                                context ->
                                        command.factory()
                                                .create(
                                                        new PathIdEventInput(
                                                                context, route.strategyId().value())),
                                STATUS_ACCEPTED,
                                STATUS_UNAUTHORIZED));
    }

    @FunctionalInterface
    interface PathIdEventFactory {
        DomainEvent create(PathIdEventInput input);
    }

    record PathIdEventInput(
            AuthenticatedStrategyPublisher.EventAuthContext context, String strategyId) {}

    record PathIdPublishCommand(
            AuthenticatedStrategyPublisher publisher,
            HttpRequest request,
            PathIdEventFactory factory) {}
}
