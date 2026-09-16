package com.defistrategyarena.strategy.adapter.dsl;

import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.Objects;

enum StrategyActionFactory {
    ;

    private static final String ALLOCATION_REQUIRED = "allocation percent must not be blank";
    private static final String INSTRUMENT_REQUIRED = "instrument must not be blank";

    static StrategyDefinition.Action hold(StrategyRuleWire body) {
        Objects.requireNonNull(body);
        return new StrategyDefinition.Hold();
    }

    static StrategyDefinition.Action buy(StrategyRuleWire body) {
        return new StrategyDefinition.Buy(requireInstrument(body), requireAllocation(body));
    }

    static StrategyDefinition.Action sell(StrategyRuleWire body) {
        return new StrategyDefinition.Sell(requireInstrument(body), requireAllocation(body));
    }

    private static String requireInstrument(StrategyRuleWire body) {
        String instrument =
                StrategyWireText.orEmpty(new StrategyWireText.TextValue(body.instrument()));
        if (instrument.isBlank()) {
            throw new IllegalArgumentException(INSTRUMENT_REQUIRED);
        }
        return instrument;
    }

    private static String requireAllocation(StrategyRuleWire body) {
        String allocation =
                StrategyWireText.orEmpty(new StrategyWireText.TextValue(body.allocationPercent()));
        if (allocation.isBlank()) {
            throw new IllegalArgumentException(ALLOCATION_REQUIRED);
        }
        return allocation;
    }
}
