package com.defistrategyarena.strategy.adapter.web;

import java.util.List;

public record UpdateStrategyHttpRequest(List<CreateStrategyHttpRequest.RuleBody> rules) {

    private static final String RULES_REQUIRED = "rules must not be null";

    public UpdateStrategyHttpRequest {
        if (rules == null) {
            throw new IllegalArgumentException(RULES_REQUIRED);
        }
        rules = List.copyOf(rules);
    }

    public static UpdateStrategyHttpRequest create(UpdateStrategyHttpRequest draft) {
        return new UpdateStrategyHttpRequest(draft.rules());
    }
}
