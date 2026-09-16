package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.adapter.web.UpdateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.UpdateStrategyHttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

public final class UpdateStrategyHttpHandler extends BaseHandler<UpdateStrategyHttpResponse> {

    private static final int EMPTY_VERSION = 0;

    private final StrategyRestAdapter strategyHttp;

    public UpdateStrategyHttpHandler(UpdateStrategyHttpHandlerDeps deps) {
        this.strategyHttp = deps.strategyHttp();
    }

    @Override
    protected UpdateStrategyHttpResponse execute(HttpRequest request) throws JsonProcessingException {
        OwnerStrategyRouteParams.OwnerStrategyRoute route = OwnerStrategyRouteParams.from(request);
        UpdateStrategyHttpRequest payload =
                readJson(new ReadJsonCommand<>(request.body(), UpdateStrategyHttpRequest.class));
        return strategyHttp.update(
                new StrategyRestAdapter.UpdateStrategyHttpInput(
                        route.ownerId(), route.strategyId(), payload));
    }

    @Override
    protected UpdateStrategyHttpResponse badRequestBody() {
        return new UpdateStrategyHttpResponse(
                STATUS_BAD_REQUEST, OwnerStrategyRouteParams.EMPTY, EMPTY_VERSION);
    }

    public record UpdateStrategyHttpHandlerDeps(StrategyRestAdapter strategyHttp) {}
}
