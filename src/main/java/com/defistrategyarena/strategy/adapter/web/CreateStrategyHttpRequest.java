package com.defistrategyarena.strategy.adapter.web;

import java.util.List;

public record CreateStrategyHttpRequest(String ownerId, String name, List<RuleBody> rules) {

    private static final String RULES_REQUIRED = "rules must not be null";

    public CreateStrategyHttpRequest {
        if (rules == null) {
            throw new IllegalArgumentException(RULES_REQUIRED);
        }
        rules = List.copyOf(rules);
    }

    public static CreateStrategyHttpRequest create(CreateStrategyHttpRequest draft) {
        return new CreateStrategyHttpRequest(draft.ownerId(), draft.name(), draft.rules());
    }

    public record RuleBody(String id, String type, String instrument, String threshold) {}
}
