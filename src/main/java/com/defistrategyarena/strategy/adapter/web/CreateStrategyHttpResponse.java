package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.shared.http.JsonHttpResult;

public record CreateStrategyHttpResponse(int status, String strategyId) implements JsonHttpResult {

    public static CreateStrategyHttpResponse create(CreateStrategyHttpResponse draft) {
        return new CreateStrategyHttpResponse(draft.status(), draft.strategyId());
    }
}
