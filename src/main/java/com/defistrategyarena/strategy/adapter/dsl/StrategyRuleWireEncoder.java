package com.defistrategyarena.strategy.adapter.dsl;

import com.defistrategyarena.strategy.domain.StrategyDefinition;

enum StrategyRuleWireEncoder {
    ;

    static StrategyRuleWire encode(StrategyDefinition.Rule rule) {
        ConditionParts condition = conditionParts(rule.when());
        ActionParts action = actionParts(rule.then());
        String instrument =
                action.instrument().isBlank() ? condition.instrument() : action.instrument();
        return new StrategyRuleWire(
                rule.id(),
                condition.conditionType(),
                action.actionType(),
                instrument,
                condition.indicator(),
                condition.threshold(),
                action.allocationPercent());
    }

    private static ConditionParts conditionParts(StrategyDefinition.Condition condition) {
        return switch (condition) {
            case StrategyDefinition.PriceAbove priceAbove ->
                    new ConditionParts(
                            StrategyDslWireNames.CONDITION_PRICE_ABOVE,
                            priceAbove.instrument(),
                            StrategyDslWireNames.EMPTY,
                            priceAbove.threshold());
            case StrategyDefinition.PriceUnder priceUnder ->
                    new ConditionParts(
                            StrategyDslWireNames.CONDITION_PRICE_UNDER,
                            priceUnder.instrument(),
                            StrategyDslWireNames.EMPTY,
                            priceUnder.threshold());
            case StrategyDefinition.IndicatorBelow indicatorBelow ->
                    new ConditionParts(
                            StrategyDslWireNames.CONDITION_INDICATOR_BELOW,
                            StrategyDslWireNames.EMPTY,
                            indicatorBelow.indicator(),
                            indicatorBelow.threshold());
            case StrategyDefinition.IndicatorAbove indicatorAbove ->
                    new ConditionParts(
                            StrategyDslWireNames.CONDITION_INDICATOR_ABOVE,
                            StrategyDslWireNames.EMPTY,
                            indicatorAbove.indicator(),
                            indicatorAbove.threshold());
        };
    }

    private static ActionParts actionParts(StrategyDefinition.Action action) {
        return switch (action) {
            case StrategyDefinition.Hold ignored ->
                    new ActionParts(
                            StrategyDslWireNames.ACTION_HOLD,
                            StrategyDslWireNames.EMPTY,
                            StrategyDslWireNames.EMPTY);
            case StrategyDefinition.Buy buy ->
                    new ActionParts(
                            StrategyDslWireNames.ACTION_BUY, buy.instrument(), buy.allocationPercent());
            case StrategyDefinition.Sell sell ->
                    new ActionParts(
                            StrategyDslWireNames.ACTION_SELL,
                            sell.instrument(),
                            sell.allocationPercent());
        };
    }

    private record ConditionParts(
            String conditionType, String instrument, String indicator, String threshold) {}

    private record ActionParts(String actionType, String instrument, String allocationPercent) {}
}
