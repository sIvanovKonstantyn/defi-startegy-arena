package com.defistrategyarena.strategy.adapter.web;

public record CreateStrategyHttpResponse(int status, String strategyId) {

    public static CreateStrategyHttpResponse create(CreateStrategyHttpResponse draft) {
        return new CreateStrategyHttpResponse(draft.status(), draft.strategyId());
    }
}
