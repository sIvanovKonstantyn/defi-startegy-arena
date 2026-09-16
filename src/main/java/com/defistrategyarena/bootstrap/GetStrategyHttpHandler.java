package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.strategy.adapter.web.StrategyDetailHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.application.GetStrategyQuery;
import java.util.List;

public final class GetStrategyHttpHandler extends BaseHandler<StrategyDetailHttpResponse> {

    private static final int EMPTY_VERSION = 0;

    private final StrategyRestAdapter strategyHttp;

    public GetStrategyHttpHandler(GetStrategyHttpHandlerDeps deps) {
        this.strategyHttp = deps.strategyHttp();
    }

    @Override
    protected StrategyDetailHttpResponse execute(HttpRequest request) {
        OwnerStrategyRouteParams.OwnerStrategyRoute route = OwnerStrategyRouteParams.from(request);
        return strategyHttp.get(new GetStrategyQuery(route.ownerId(), route.strategyId()));
    }

    @Override
    protected StrategyDetailHttpResponse badRequestBody() {
        return new StrategyDetailHttpResponse(
                STATUS_BAD_REQUEST,
                OwnerStrategyRouteParams.EMPTY,
                OwnerStrategyRouteParams.EMPTY,
                OwnerStrategyRouteParams.EMPTY,
                EMPTY_VERSION,
                List.of());
    }

    public record GetStrategyHttpHandlerDeps(StrategyRestAdapter strategyHttp) {}
}
