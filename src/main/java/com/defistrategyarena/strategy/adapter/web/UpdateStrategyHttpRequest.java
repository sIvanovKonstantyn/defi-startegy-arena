package com.defistrategyarena.strategy.adapter.web;

import java.util.List;

public record UpdateStrategyHttpRequest(String description, List<CreateStrategyHttpRequest.RuleBody> rules) {

    private static final String RULES_REQUIRED = "rules must not be null";
    private static final String EMPTY = "";

    public UpdateStrategyHttpRequest {
        if (rules == null) {
            throw new IllegalArgumentException(RULES_REQUIRED);
        }
        rules = List.copyOf(rules);
        if (description == null) {
            description = EMPTY;
        }
    }

    public static UpdateStrategyHttpRequest create(UpdateStrategyHttpRequest draft) {
        return new UpdateStrategyHttpRequest(draft.description(), draft.rules());
    }
}
