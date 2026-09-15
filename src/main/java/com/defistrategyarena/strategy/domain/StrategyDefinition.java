package com.defistrategyarena.strategy.domain;

import java.util.List;

public record StrategyDefinition(String name, List<Rule> rules) {

    private static final String NAME_REQUIRED = "strategy name must not be blank";
    private static final String RULES_REQUIRED = "strategy rules must not be null";

    public StrategyDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(NAME_REQUIRED);
        }
        if (rules == null) {
            throw new IllegalArgumentException(RULES_REQUIRED);
        }
        rules = List.copyOf(rules);
    }

    public static StrategyDefinition create(StrategyDefinition draft) {
        return new StrategyDefinition(draft.name(), draft.rules());
    }

    public record Rule(String id, Condition when, Action then) {}

    public sealed interface Condition permits PriceAbove {}

    public record PriceAbove(String instrument, String threshold) implements Condition {}

    public sealed interface Action permits Hold {}

    public record Hold() implements Action {}
}
