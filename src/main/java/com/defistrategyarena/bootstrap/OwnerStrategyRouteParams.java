package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.Map;

enum OwnerStrategyRouteParams {
    ;

    static final String OWNER_ID = "ownerId";
    static final String STRATEGY_ID = "strategyId";
    static final String EMPTY = "";

    static OwnerStrategyRoute from(HttpRequest request) {
        ParamMaps maps = new ParamMaps(request.query(), request.pathVariables());
        return new OwnerStrategyRoute(
                maps.text(new NamedMap(maps.query(), OWNER_ID)),
                new StrategyId(maps.text(new NamedMap(maps.path(), STRATEGY_ID))));
    }

    record OwnerStrategyRoute(String ownerId, StrategyId strategyId) {}

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
}
