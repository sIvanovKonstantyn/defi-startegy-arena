package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.events.strategy.DeleteStrategyRequested;
import com.defistrategyarena.shared.events.strategy.GetStrategyRequested;
import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.messaging.DomainEvent;

public final class PathIdStrategyHttpHandler extends BaseHandler<AcceptedHttpResponse> {

    private final AuthenticatedStrategyPublisher publisher;
    private final PathIdEventKind kind;

    public PathIdStrategyHttpHandler(PathIdStrategyHttpHandlerDeps deps) {
        this.publisher = deps.publisher();
        this.kind = deps.kind();
    }

    @Override
    protected AcceptedHttpResponse execute(HttpRequest request) {
        return PathIdStrategyPublish.publish(
                new PathIdStrategyPublish.PathIdPublishCommand(
                        publisher, request, kind::event));
    }

    @Override
    protected AcceptedHttpResponse badRequestBody() {
        return AcceptedResponses.badRequest();
    }

    public enum PathIdEventKind {
        GET {
            @Override
            DomainEvent event(PathIdStrategyPublish.PathIdEventInput input) {
                return new GetStrategyRequested(
                        input.context().correlationId(),
                        input.context().ownerId(),
                        input.strategyId());
            }
        },
        DELETE {
            @Override
            DomainEvent event(PathIdStrategyPublish.PathIdEventInput input) {
                return new DeleteStrategyRequested(
                        input.context().correlationId(),
                        input.context().ownerId(),
                        input.strategyId());
            }
        };

        abstract DomainEvent event(PathIdStrategyPublish.PathIdEventInput input);
    }

    public record PathIdStrategyHttpHandlerDeps(
            AuthenticatedStrategyPublisher publisher, PathIdEventKind kind) {}
}
