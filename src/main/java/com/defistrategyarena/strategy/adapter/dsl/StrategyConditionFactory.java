package com.defistrategyarena.strategy.adapter.dsl;

import com.defistrategyarena.shared.indicators.IndicatorId;
import com.defistrategyarena.strategy.domain.CompareOperator;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class StrategyConditionFactory {

    private static final String UNKNOWN_CONDITION_TYPE = "unknown condition type";

    private static final Map<String, Function<StrategyConditionWire, StrategyDefinition.Condition>>
            NODES =
                    Map.of(
                            StrategyDslWireNames.CONDITION_AND, StrategyConditionFactory::andNode,
                            StrategyDslWireNames.CONDITION_OR, StrategyConditionFactory::orNode,
                            StrategyDslWireNames.CONDITION_PRICE_COMPARE,
                                    StrategyConditionFactory::priceCompare,
                            StrategyDslWireNames.CONDITION_INDICATOR_COMPARE,
                                    StrategyConditionFactory::indicatorCompare);

    private StrategyConditionFactory() {}

    public static StrategyDefinition.Condition fromWire(StrategyConditionWire wire) {
        Function<StrategyConditionWire, StrategyDefinition.Condition> mapper = NODES.get(wire.type());
        if (mapper == null) {
            throw new IllegalArgumentException(UNKNOWN_CONDITION_TYPE);
        }
        return mapper.apply(wire);
    }

    private static StrategyDefinition.Condition andNode(StrategyConditionWire wire) {
        return new StrategyDefinition.And(mapChildren(wire));
    }

    private static StrategyDefinition.Condition orNode(StrategyConditionWire wire) {
        return new StrategyDefinition.Or(mapChildren(wire));
    }

    private static StrategyDefinition.Condition priceCompare(StrategyConditionWire wire) {
        return new StrategyDefinition.PriceCompare(
                wire.instrument(),
                CompareOperator.fromWire(new CompareOperator.WireOperator(wire.operator())),
                wire.threshold());
    }

    private static StrategyDefinition.Condition indicatorCompare(StrategyConditionWire wire) {
        return new StrategyDefinition.IndicatorCompare(
                new IndicatorId(wire.indicator()),
                wire.parameters(),
                CompareOperator.fromWire(new CompareOperator.WireOperator(wire.operator())),
                wire.threshold());
    }

    private static List<StrategyDefinition.Condition> mapChildren(StrategyConditionWire wire) {
        List<StrategyDefinition.Condition> children = new ArrayList<>();
        wire.children().forEach(child -> children.add(fromWire(child)));
        return List.copyOf(children);
    }
}
