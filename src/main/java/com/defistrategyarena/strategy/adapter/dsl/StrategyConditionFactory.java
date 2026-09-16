package com.defistrategyarena.strategy.adapter.dsl;

import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.function.BiFunction;

enum StrategyConditionFactory {
    ;

    static StrategyDefinition.Condition priceAbove(StrategyRuleWire body) {
        return build(
                new ConditionBuild(
                        body,
                        FieldPair.INSTRUMENT_THRESHOLD,
                        (left, right) -> new StrategyDefinition.PriceAbove(left, right)));
    }

    static StrategyDefinition.Condition priceUnder(StrategyRuleWire body) {
        return build(
                new ConditionBuild(
                        body,
                        FieldPair.INSTRUMENT_THRESHOLD,
                        (left, right) -> new StrategyDefinition.PriceUnder(left, right)));
    }

    static StrategyDefinition.Condition indicatorBelow(StrategyRuleWire body) {
        return build(
                new ConditionBuild(
                        body,
                        FieldPair.INDICATOR_THRESHOLD,
                        (left, right) -> new StrategyDefinition.IndicatorBelow(left, right)));
    }

    static StrategyDefinition.Condition indicatorAbove(StrategyRuleWire body) {
        return build(
                new ConditionBuild(
                        body,
                        FieldPair.INDICATOR_THRESHOLD,
                        (left, right) -> new StrategyDefinition.IndicatorAbove(left, right)));
    }

    private static StrategyDefinition.Condition build(ConditionBuild build) {
        String left =
                build.fields() == FieldPair.INSTRUMENT_THRESHOLD
                        ? text(new StrategyWireText.TextValue(build.body().instrument()))
                        : text(new StrategyWireText.TextValue(build.body().indicator()));
        String right = text(new StrategyWireText.TextValue(build.body().threshold()));
        return build.factory().apply(left, right);
    }

    private static String text(StrategyWireText.TextValue value) {
        return StrategyWireText.orEmpty(value);
    }

    private enum FieldPair {
        INSTRUMENT_THRESHOLD,
        INDICATOR_THRESHOLD
    }

    private record ConditionBuild(
            StrategyRuleWire body,
            FieldPair fields,
            BiFunction<String, String, StrategyDefinition.Condition> factory) {}
}
