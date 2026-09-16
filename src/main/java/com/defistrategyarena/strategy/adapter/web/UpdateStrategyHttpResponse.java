package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.shared.http.JsonHttpResult;

public record UpdateStrategyHttpResponse(int status, String strategyId, int versionNumber)
        implements JsonHttpResult {

    public static UpdateStrategyHttpResponse create(UpdateStrategyHttpResponse draft) {
        return new UpdateStrategyHttpResponse(
                draft.status(), draft.strategyId(), draft.versionNumber());
    }
}
