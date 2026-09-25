package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.events.strategy.CreateStrategyRequested;
import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class CreateStrategyHttpHandler extends BaseHandler<AcceptedHttpResponse> {

    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_UNAUTHORIZED = 401;

    private final AuthenticatedStrategyPublisher publisher;

    public CreateStrategyHttpHandler(CreateStrategyHttpHandlerDeps deps) {
        this.publisher = deps.publisher();
    }

    CreateStrategyHttpHandler(AuthenticatedStrategyPublisher publisher, ObjectMapper objectMapper) {
        super(objectMapper);
        this.publisher = publisher;
    }

    @Override
    protected AcceptedHttpResponse execute(HttpRequest request) throws JsonProcessingException {
        CreateStrategyHttpRequest payload =
                readJson(new ReadJsonCommand<>(request.body(), CreateStrategyHttpRequest.class));
        return publisher.publish(
                new AuthenticatedStrategyPublisher.AuthorizedPublish(
                        request,
                        context -> createRequested(new CreateRequestedInput(context, payload)),
                        STATUS_ACCEPTED,
                        STATUS_UNAUTHORIZED));
    }

    private static CreateStrategyRequested createRequested(CreateRequestedInput input) {
        return new CreateStrategyRequested(
                input.context().correlationId(),
                input.context().ownerId(),
                input.payload().name(),
                input.payload().description(),
                BootstrapRulePayloads.fromHttp(
                        new BootstrapRulePayloads.RuleBodies(input.payload().rules())));
    }

    private record CreateRequestedInput(
            AuthenticatedStrategyPublisher.EventAuthContext context,
            CreateStrategyHttpRequest payload) {}

    @Override
    protected AcceptedHttpResponse badRequestBody() {
        return AcceptedResponses.badRequest();
    }

    public record CreateStrategyHttpHandlerDeps(AuthenticatedStrategyPublisher publisher) {}
}
