package com.defistrategyarena.strategy.domain;

import java.util.List;

public enum NonEmptyRuleList {
    ;

    private static final String RULES_REQUIRED = "rules must not be null";
    private static final String RULES_EMPTY = "rules must not be empty";
    private static final int EMPTY_RULES = 0;

    public static List<StrategyDefinition.Rule> copyRequired(List<StrategyDefinition.Rule> rules) {
        requirePresent(rules);
        requireNonEmpty(rules);
        return List.copyOf(rules);
    }

    private static void requirePresent(List<StrategyDefinition.Rule> rules) {
        if (rules == null) {
            throw new IllegalArgumentException(RULES_REQUIRED);
        }
    }

    private static void requireNonEmpty(List<StrategyDefinition.Rule> rules) {
        if (rules.size() == EMPTY_RULES) {
            throw new IllegalArgumentException(RULES_EMPTY);
        }
    }
}
