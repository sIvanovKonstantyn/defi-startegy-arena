package com.defistrategyarena.strategy.adapter.dsl;

import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StrategyRuleWireEncoder {

    private StrategyRuleWireEncoder() {}

    public static StrategyRuleWire encode(StrategyDefinition.Rule rule) {
        return new StrategyRuleWire(rule.id(), encodeCondition(rule.when()), encodeAction(rule.then()));
    }

    private static StrategyConditionWire encodeCondition(StrategyDefinition.Condition condition) {
        return switch (condition) {
            case StrategyDefinition.And and ->
                    new StrategyConditionWire(
                            StrategyDslWireNames.CONDITION_AND,
                            encodeChildren(and.children()),
                            StrategyDslWireNames.EMPTY,
                            StrategyDslWireNames.EMPTY,
                            StrategyDslWireNames.EMPTY,
                            StrategyDslWireNames.EMPTY,
                            Map.of());
            case StrategyDefinition.Or or ->
                    new StrategyConditionWire(
                            StrategyDslWireNames.CONDITION_OR,
                            encodeChildren(or.children()),
                            StrategyDslWireNames.EMPTY,
                            StrategyDslWireNames.EMPTY,
                            StrategyDslWireNames.EMPTY,
                            StrategyDslWireNames.EMPTY,
                            Map.of());
            case StrategyDefinition.PriceCompare price ->
                    new StrategyConditionWire(
                            StrategyDslWireNames.CONDITION_PRICE_COMPARE,
                            List.of(),
                            price.instrument(),
                            StrategyDslWireNames.EMPTY,
                            price.operator().wireValue(),
                            price.threshold(),
                            Map.of());
            case StrategyDefinition.IndicatorCompare indicator ->
                    new StrategyConditionWire(
                            StrategyDslWireNames.CONDITION_INDICATOR_COMPARE,
                            List.of(),
                            StrategyDslWireNames.EMPTY,
                            indicator.indicatorId().value(),
                            indicator.operator().wireValue(),
                            indicator.threshold(),
                            indicator.parameters());
        };
    }

    private static List<StrategyConditionWire> encodeChildren(
            List<StrategyDefinition.Condition> children) {
        List<StrategyConditionWire> encoded = new ArrayList<>();
        for (StrategyDefinition.Condition child : children) {
            encoded.add(encodeCondition(child));
        }
        return List.copyOf(encoded);
    }

    private static StrategyActionWire encodeAction(StrategyDefinition.Action action) {
        return switch (action) {
            case StrategyDefinition.Hold ignored ->
                    new StrategyActionWire(
                            StrategyDslWireNames.ACTION_HOLD,
                            StrategyDslWireNames.EMPTY,
                            StrategyDslWireNames.EMPTY,
                            StrategyDslWireNames.EMPTY,
                            StrategyDslWireNames.EMPTY);
            case StrategyDefinition.Buy buy ->
                    new StrategyActionWire(
                            StrategyDslWireNames.ACTION_BUY,
                            buy.instrument(),
                            StrategyDslWireNames.EMPTY,
                            buy.allocationPercent(),
                            StrategyDslWireNames.EMPTY);
            case StrategyDefinition.Sell sell ->
                    new StrategyActionWire(
                            StrategyDslWireNames.ACTION_SELL,
                            sell.instrument(),
                            StrategyDslWireNames.EMPTY,
                            sell.allocationPercent(),
                            StrategyDslWireNames.EMPTY);
            case StrategyDefinition.OpenLp openLp ->
                    new StrategyActionWire(
                            StrategyDslWireNames.ACTION_OPEN_LP,
                            StrategyDslWireNames.EMPTY,
                            openLp.instrumentPair(),
                            openLp.allocationPercent(),
                            openLp.yearlyFeePercent());
        };
    }
}
