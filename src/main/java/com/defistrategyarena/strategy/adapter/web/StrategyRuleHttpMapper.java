package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.ArrayList;
import java.util.List;

enum StrategyRuleHttpMapper {
    ;

    static List<CreateStrategyHttpRequest.RuleBody> toBodies(List<StrategyDefinition.Rule> rules) {
        List<CreateStrategyHttpRequest.RuleBody> bodies = new ArrayList<>();
        for (StrategyDefinition.Rule rule : rules) {
            bodies.add(toBody(rule));
        }
        return List.copyOf(bodies);
    }

    static CreateStrategyHttpRequest.RuleBody toBody(StrategyDefinition.Rule rule) {
        ConditionWire condition = conditionWire(rule.when());
        ActionWire action = actionWire(rule.then());
        String instrument =
                action.instrument().isBlank() ? condition.instrument() : action.instrument();
        return new CreateStrategyHttpRequest.RuleBody(
                rule.id(),
                condition.conditionType(),
                action.actionType(),
                instrument,
                condition.indicator(),
                condition.threshold(),
                action.allocationPercent());
    }

    private static ConditionWire conditionWire(StrategyDefinition.Condition condition) {
        return switch (condition) {
            case StrategyDefinition.PriceAbove priceAbove ->
                    new ConditionWire(
                            StrategyDslWireNames.CONDITION_PRICE_ABOVE,
                            priceAbove.instrument(),
                            StrategyDslWireNames.EMPTY,
                            priceAbove.threshold());
            case StrategyDefinition.PriceUnder priceUnder ->
                    new ConditionWire(
                            StrategyDslWireNames.CONDITION_PRICE_UNDER,
                            priceUnder.instrument(),
                            StrategyDslWireNames.EMPTY,
                            priceUnder.threshold());
            case StrategyDefinition.IndicatorBelow indicatorBelow ->
                    new ConditionWire(
                            StrategyDslWireNames.CONDITION_INDICATOR_BELOW,
                            StrategyDslWireNames.EMPTY,
                            indicatorBelow.indicator(),
                            indicatorBelow.threshold());
            case StrategyDefinition.IndicatorAbove indicatorAbove ->
                    new ConditionWire(
                            StrategyDslWireNames.CONDITION_INDICATOR_ABOVE,
                            StrategyDslWireNames.EMPTY,
                            indicatorAbove.indicator(),
                            indicatorAbove.threshold());
        };
    }

    private static ActionWire actionWire(StrategyDefinition.Action action) {
        return switch (action) {
            case StrategyDefinition.Hold ignored ->
                    new ActionWire(
                            StrategyDslWireNames.ACTION_HOLD,
                            StrategyDslWireNames.EMPTY,
                            StrategyDslWireNames.EMPTY);
            case StrategyDefinition.Buy buy ->
                    new ActionWire(
                            StrategyDslWireNames.ACTION_BUY, buy.instrument(), buy.allocationPercent());
            case StrategyDefinition.Sell sell ->
                    new ActionWire(
                            StrategyDslWireNames.ACTION_SELL,
                            sell.instrument(),
                            sell.allocationPercent());
        };
    }

    private record ConditionWire(
            String conditionType, String instrument, String indicator, String threshold) {}

    private record ActionWire(String actionType, String instrument, String allocationPercent) {}
}
