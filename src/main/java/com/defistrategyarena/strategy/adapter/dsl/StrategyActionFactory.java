package com.defistrategyarena.strategy.adapter.dsl;

import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.Map;
import java.util.function.Function;

public final class StrategyActionFactory {

    private static final String UNKNOWN_ACTION_TYPE = "unknown action type";

    private static final Map<String, Function<StrategyActionWire, StrategyDefinition.Action>> ACTIONS =
            Map.of(
                    StrategyDslWireNames.ACTION_HOLD, ignored -> new StrategyDefinition.Hold(),
                    StrategyDslWireNames.ACTION_BUY, StrategyActionFactory::buy,
                    StrategyDslWireNames.ACTION_SELL, StrategyActionFactory::sell,
                    StrategyDslWireNames.ACTION_OPEN_LP, StrategyActionFactory::openLp);

    private StrategyActionFactory() {}

    public static StrategyDefinition.Action fromWire(StrategyActionWire wire) {
        Function<StrategyActionWire, StrategyDefinition.Action> mapper = ACTIONS.get(wire.type());
        if (mapper == null) {
            throw new IllegalArgumentException(UNKNOWN_ACTION_TYPE);
        }
        return mapper.apply(wire);
    }

    private static StrategyDefinition.Action buy(StrategyActionWire wire) {
        return new StrategyDefinition.Buy(wire.instrument(), wire.allocationPercent());
    }

    private static StrategyDefinition.Action sell(StrategyActionWire wire) {
        return new StrategyDefinition.Sell(wire.instrument(), wire.allocationPercent());
    }

    private static StrategyDefinition.Action openLp(StrategyActionWire wire) {
        return new StrategyDefinition.OpenLp(
                wire.instrumentPair(), wire.allocationPercent(), wire.yearlyFeePercent());
    }
}
