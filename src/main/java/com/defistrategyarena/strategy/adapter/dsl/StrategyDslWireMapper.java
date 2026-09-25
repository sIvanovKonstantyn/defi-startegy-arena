package com.defistrategyarena.strategy.adapter.dsl;

import com.defistrategyarena.strategy.domain.StrategyDefinition;

public final class StrategyDslWireMapper {

    private StrategyDslWireMapper() {}

    public static StrategyRuleWire toWire(StrategyDefinition.Rule rule) {
        return StrategyRuleWireEncoder.encode(rule);
    }

    public static StrategyDefinition.Rule toRule(StrategyRuleWire body) {
        return new StrategyDefinition.Rule(
                body.id(),
                StrategyConditionFactory.fromWire(body.when()),
                StrategyActionFactory.fromWire(body.then()));
    }
}
