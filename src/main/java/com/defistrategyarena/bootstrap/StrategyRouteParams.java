package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.strategy.domain.StrategyId;

enum StrategyRouteParams {
    ;

    static final String STRATEGY_ID = "strategyId";
    static final String EMPTY = "";

    static StrategyRoute from(HttpRequest request) {
        String strategyId = request.pathVariables().get(STRATEGY_ID);
        if (strategyId == null) {
            strategyId = EMPTY;
        }
        return new StrategyRoute(new StrategyId(strategyId));
    }

    record StrategyRoute(StrategyId strategyId) {}
}
