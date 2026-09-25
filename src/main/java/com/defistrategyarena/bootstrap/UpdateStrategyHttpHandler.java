package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.events.strategy.UpdateStrategyRequested;
import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.strategy.adapter.web.UpdateStrategyHttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;

public final class UpdateStrategyHttpHandler extends BaseHandler<AcceptedHttpResponse> {

    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_UNAUTHORIZED = 401;

    private final AuthenticatedStrategyPublisher publisher;

    public UpdateStrategyHttpHandler(UpdateStrategyHttpHandlerDeps deps) {
        this.publisher = deps.publisher();
    }

    @Override
    protected AcceptedHttpResponse execute(HttpRequest request) throws JsonProcessingException {
        String strategyId = StrategyRouteParams.from(request).strategyId().value();
        UpdateStrategyHttpRequest payload =
                readJson(new ReadJsonCommand<>(request.body(), UpdateStrategyHttpRequest.class));
        return publisher.publish(
                new AuthenticatedStrategyPublisher.AuthorizedPublish(
                        request,
                        context ->
                                updateRequested(
                                        new UpdateRequestedInput(context, strategyId, payload)),
                        STATUS_ACCEPTED,
                        STATUS_UNAUTHORIZED));
    }

    private static UpdateStrategyRequested updateRequested(UpdateRequestedInput input) {
        return new UpdateStrategyRequested(
                input.context().correlationId(),
                input.context().ownerId(),
                input.strategyId(),
                input.payload().description(),
                BootstrapRulePayloads.fromHttp(
                        new BootstrapRulePayloads.RuleBodies(input.payload().rules())));
    }

    private record UpdateRequestedInput(
            AuthenticatedStrategyPublisher.EventAuthContext context,
            String strategyId,
            UpdateStrategyHttpRequest payload) {}

    @Override
    protected AcceptedHttpResponse badRequestBody() {
        return AcceptedResponses.badRequest();
    }

    public record UpdateStrategyHttpHandlerDeps(AuthenticatedStrategyPublisher publisher) {}
}
