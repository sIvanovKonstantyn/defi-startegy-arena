package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.strategy.adapter.web.StrategyDetailHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.application.GetStrategyQuery;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.List;
import java.util.Map;

public final class GetStrategyHttpHandler extends BaseHandler<StrategyDetailHttpResponse> {

    private static final String OWNER_ID = "ownerId";
    private static final String STRATEGY_ID = "strategyId";
    private static final String EMPTY = "";
    private static final int EMPTY_VERSION = 0;

    private final StrategyRestAdapter strategyHttp;

    public GetStrategyHttpHandler(GetStrategyHttpHandlerDeps deps) {
        this.strategyHttp = deps.strategyHttp();
    }

    @Override
    protected StrategyDetailHttpResponse execute(HttpRequest request) {
        ParamMaps maps = new ParamMaps(request.query(), request.pathVariables());
        return strategyHttp.get(
                new GetStrategyQuery(
                        maps.text(new NamedMap(maps.query(), OWNER_ID)),
                        new StrategyId(maps.text(new NamedMap(maps.path(), STRATEGY_ID)))));
    }

    @Override
    protected StrategyDetailHttpResponse badRequestBody() {
        return new StrategyDetailHttpResponse(
                STATUS_BAD_REQUEST, EMPTY, EMPTY, EMPTY, EMPTY_VERSION, List.of());
    }

    private record ParamMaps(Map<String, String> query, Map<String, String> path) {
        private String text(NamedMap lookup) {
            String value = lookup.values().get(lookup.key());
            if (value == null) {
                return EMPTY;
            }
            return value;
        }
    }

    private record NamedMap(Map<String, String> values, String key) {}

    public record GetStrategyHttpHandlerDeps(StrategyRestAdapter strategyHttp) {}
}
