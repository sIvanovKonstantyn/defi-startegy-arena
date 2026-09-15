package com.defistrategyarena.strategy.domain;

import java.util.List;

public record StrategyDefinition(String name, List<Rule> rules) {

    private static final String EMPTY = "";
    private static final String NAME_REQUIRED = "strategy name must not be blank";
    private static final String RULES_REQUIRED = "strategy rules must not be null";
    private static final String INSTRUMENT_REQUIRED = "instrument must not be blank";
    private static final String ALLOCATION_REQUIRED = "allocation percent must not be blank";

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

    public sealed interface Condition permits PriceAbove, PriceUnder, IndicatorBelow, IndicatorAbove {}

    public record PriceAbove(String instrument, String threshold) implements Condition {}

    public record PriceUnder(String instrument, String threshold) implements Condition {}

    public record IndicatorBelow(String indicator, String threshold) implements Condition {}

    public record IndicatorAbove(String indicator, String threshold) implements Condition {}

    public sealed interface Action permits Hold, Buy, Sell {}

    public record Hold() implements Action {}

    public record Buy(String instrument, String allocationPercent) implements Action {
        public Buy {
            NonBlankText.require(new NonBlankText(instrument, INSTRUMENT_REQUIRED));
            NonBlankText.require(new NonBlankText(allocationPercent, ALLOCATION_REQUIRED));
        }
    }

    public record Sell(String instrument, String allocationPercent) implements Action {
        public Sell {
            NonBlankText.require(new NonBlankText(instrument, INSTRUMENT_REQUIRED));
            NonBlankText.require(new NonBlankText(allocationPercent, ALLOCATION_REQUIRED));
        }
    }

    private record NonBlankText(String value, String message) {
        private static void require(NonBlankText text) {
            String normalized = text.value() == null ? EMPTY : text.value();
            if (normalized.isBlank()) {
                throw new IllegalArgumentException(text.message());
            }
        }
    }
}
