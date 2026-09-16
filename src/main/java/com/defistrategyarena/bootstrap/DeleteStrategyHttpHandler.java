package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.strategy.adapter.web.DeleteStrategyHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;

public final class DeleteStrategyHttpHandler extends BaseHandler<DeleteStrategyHttpResponse> {

    private final StrategyRestAdapter strategyHttp;

    public DeleteStrategyHttpHandler(DeleteStrategyHttpHandlerDeps deps) {
        this.strategyHttp = deps.strategyHttp();
    }

    @Override
    protected DeleteStrategyHttpResponse execute(HttpRequest request) {
        OwnerStrategyRouteParams.OwnerStrategyRoute route = OwnerStrategyRouteParams.from(request);
        return strategyHttp.delete(
                new StrategyRestAdapter.DeleteStrategyHttpInput(route.ownerId(), route.strategyId()));
    }

    @Override
    protected DeleteStrategyHttpResponse badRequestBody() {
        return new DeleteStrategyHttpResponse(STATUS_BAD_REQUEST, OwnerStrategyRouteParams.EMPTY);
    }

    public record DeleteStrategyHttpHandlerDeps(StrategyRestAdapter strategyHttp) {}
}
