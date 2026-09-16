package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.shared.http.JsonHttpResult;

public record DeleteStrategyHttpResponse(int status, String strategyId) implements JsonHttpResult {

    public static DeleteStrategyHttpResponse create(DeleteStrategyHttpResponse draft) {
        return new DeleteStrategyHttpResponse(draft.status(), draft.strategyId());
    }
}
