package com.defistrategyarena.strategy.domain;

import com.defistrategyarena.shared.indicators.IndicatorCatalog;
import com.defistrategyarena.shared.indicators.IndicatorId;
import java.util.List;
import java.util.Map;

public record StrategyDefinition(String name, String description, List<Rule> rules) {

    private static final String EMPTY = "";
    private static final String NAME_REQUIRED = "strategy name must not be blank";
    private static final String DESCRIPTION_REQUIRED = "strategy description must not be null";
    private static final String RULES_REQUIRED = "strategy rules must not be null";
    private static final String INSTRUMENT_REQUIRED = "instrument must not be blank";
    private static final String THRESHOLD_REQUIRED = "threshold must not be blank";
    private static final String ALLOCATION_REQUIRED = "allocation percent must not be blank";
    private static final String INSTRUMENT_PAIR_REQUIRED = "instrument pair must not be blank";
    private static final String YEARLY_FEE_REQUIRED = "yearly fee percent must not be blank";
    private static final String CHILDREN_REQUIRED = "condition children must not be empty";
    private static final String OPERATOR_REQUIRED = "compare operator must not be null";
    private static final String PARAMETERS_REQUIRED = "indicator parameters must not be null";
    private static final String INDICATOR_REQUIRED = "indicator id must not be null";

    public StrategyDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(NAME_REQUIRED);
        }
        if (description == null) {
            throw new IllegalArgumentException(DESCRIPTION_REQUIRED);
        }
        if (rules == null) {
            throw new IllegalArgumentException(RULES_REQUIRED);
        }
        rules = List.copyOf(rules);
    }

    public static StrategyDefinition create(StrategyDefinition draft) {
        return new StrategyDefinition(draft.name(), draft.description(), draft.rules());
    }

    public record Rule(String id, Condition when, Action then) {
        private static final String WHEN_REQUIRED = "rule when condition must not be null";
        private static final String THEN_REQUIRED = "rule then action must not be null";

        public Rule {
            if (when == null) {
                throw new IllegalArgumentException(WHEN_REQUIRED);
            }
            if (then == null) {
                throw new IllegalArgumentException(THEN_REQUIRED);
            }
        }
    }

    public sealed interface Condition permits And, Or, PriceCompare, IndicatorCompare {}

    public static final class And implements Condition {
        private final List<Condition> children;

        public And(List<Condition> children) {
            this.children = requireChildren(children);
        }

        public List<Condition> children() {
            return List.copyOf(children);
        }
    }

    public static final class Or implements Condition {
        private final List<Condition> children;

        public Or(List<Condition> children) {
            this.children = requireChildren(children);
        }

        public List<Condition> children() {
            return List.copyOf(children);
        }
    }

    public record PriceCompare(String instrument, CompareOperator operator, String threshold)
            implements Condition {
        public PriceCompare {
            NonBlankText.require(new NonBlankText(instrument, INSTRUMENT_REQUIRED));
            if (operator == null) {
                throw new IllegalArgumentException(OPERATOR_REQUIRED);
            }
            NonBlankText.require(new NonBlankText(threshold, THRESHOLD_REQUIRED));
        }
    }

    public record IndicatorCompare(
            IndicatorId indicatorId,
            Map<String, String> parameters,
            CompareOperator operator,
            String threshold)
            implements Condition {
        public IndicatorCompare {
            if (indicatorId == null) {
                throw new IllegalArgumentException(INDICATOR_REQUIRED);
            }
            if (parameters == null) {
                throw new IllegalArgumentException(PARAMETERS_REQUIRED);
            }
            parameters = Map.copyOf(parameters);
            if (operator == null) {
                throw new IllegalArgumentException(OPERATOR_REQUIRED);
            }
            NonBlankText.require(new NonBlankText(threshold, THRESHOLD_REQUIRED));
            IndicatorCatalog.require(indicatorId);
            IndicatorCatalog.validateParameters(
                    new IndicatorCatalog.ValidateParametersCommand(indicatorId, parameters));
        }
    }

    public sealed interface Action permits Hold, Buy, Sell, OpenLp {}

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

    public record OpenLp(String instrumentPair, String allocationPercent, String yearlyFeePercent)
            implements Action {
        public OpenLp {
            NonBlankText.require(new NonBlankText(instrumentPair, INSTRUMENT_PAIR_REQUIRED));
            NonBlankText.require(new NonBlankText(allocationPercent, ALLOCATION_REQUIRED));
            NonBlankText.require(new NonBlankText(yearlyFeePercent, YEARLY_FEE_REQUIRED));
        }
    }

    private static List<Condition> requireChildren(List<Condition> children) {
        if (children == null || children.isEmpty()) {
            throw new IllegalArgumentException(CHILDREN_REQUIRED);
        }
        return List.copyOf(children);
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
