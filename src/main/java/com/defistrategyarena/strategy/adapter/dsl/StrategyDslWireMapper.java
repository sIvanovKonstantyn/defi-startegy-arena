package com.defistrategyarena.strategy.adapter.dsl;

import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.Map;
import java.util.function.Function;

public final class StrategyDslWireMapper {

    private static final String UNKNOWN_CONDITION_TYPE = "unknown condition type";
    private static final String UNKNOWN_ACTION_TYPE = "unknown action type";

    private static final Map<String, Function<StrategyRuleWire, StrategyDefinition.Condition>>
            CONDITIONS =
                    Map.of(
                            StrategyDslWireNames.CONDITION_PRICE_ABOVE,
                                    StrategyConditionFactory::priceAbove,
                            StrategyDslWireNames.CONDITION_PRICE_UNDER,
                                    StrategyConditionFactory::priceUnder,
                            StrategyDslWireNames.CONDITION_INDICATOR_BELOW,
                                    StrategyConditionFactory::indicatorBelow,
                            StrategyDslWireNames.CONDITION_INDICATOR_ABOVE,
                                    StrategyConditionFactory::indicatorAbove);

    private static final Map<String, Function<StrategyRuleWire, StrategyDefinition.Action>> ACTIONS =
            Map.of(
                    StrategyDslWireNames.ACTION_HOLD, StrategyActionFactory::hold,
                    StrategyDslWireNames.ACTION_BUY, StrategyActionFactory::buy,
                    StrategyDslWireNames.ACTION_SELL, StrategyActionFactory::sell);

    private StrategyDslWireMapper() {}

    public static StrategyRuleWire toWire(StrategyDefinition.Rule rule) {
        return StrategyRuleWireEncoder.encode(rule);
    }

    public static StrategyDefinition.Rule toRule(StrategyRuleWire body) {
        return new StrategyDefinition.Rule(body.id(), toCondition(body), toAction(body));
    }

    private static StrategyDefinition.Condition toCondition(StrategyRuleWire body) {
        Function<StrategyRuleWire, StrategyDefinition.Condition> mapper =
                CONDITIONS.get(StrategyWireText.orEmpty(new StrategyWireText.TextValue(body.conditionType())));
        if (mapper == null) {
            throw new IllegalArgumentException(UNKNOWN_CONDITION_TYPE);
        }
        return mapper.apply(body);
    }

    private static StrategyDefinition.Action toAction(StrategyRuleWire body) {
        Function<StrategyRuleWire, StrategyDefinition.Action> mapper =
                ACTIONS.get(StrategyWireText.orEmpty(new StrategyWireText.TextValue(body.actionType())));
        if (mapper == null) {
            throw new IllegalArgumentException(UNKNOWN_ACTION_TYPE);
        }
        return mapper.apply(body);
    }
}
