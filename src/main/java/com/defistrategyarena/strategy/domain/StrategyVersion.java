package com.defistrategyarena.strategy.domain;

public record StrategyVersion(int number, StrategyDefinition definition) {

    private static final int INITIAL_VERSION = 1;
    private static final String DEFINITION_REQUIRED = "strategy definition must not be null";

    public StrategyVersion {
        if (definition == null) {
            throw new IllegalArgumentException(DEFINITION_REQUIRED);
        }
    }

    public static StrategyVersion create(StrategyVersion draft) {
        return new StrategyVersion(draft.number(), draft.definition());
    }

    public static StrategyVersion initial(StrategyDefinition definition) {
        return new StrategyVersion(INITIAL_VERSION, definition);
    }
}
