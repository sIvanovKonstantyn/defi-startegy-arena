package com.defistrategyarena.strategy.adapter.dsl;

import java.util.List;
import java.util.Map;

public record StrategyConditionWire(
        String type,
        List<StrategyConditionWire> children,
        String instrument,
        String indicator,
        String operator,
        String threshold,
        Map<String, String> parameters) {

    public StrategyConditionWire {
        type = StrategyWireText.orEmpty(new StrategyWireText.TextValue(type));
        children = children == null ? List.of() : List.copyOf(children);
        instrument = StrategyWireText.orEmpty(new StrategyWireText.TextValue(instrument));
        indicator = StrategyWireText.orEmpty(new StrategyWireText.TextValue(indicator));
        operator = StrategyWireText.orEmpty(new StrategyWireText.TextValue(operator));
        threshold = StrategyWireText.orEmpty(new StrategyWireText.TextValue(threshold));
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
