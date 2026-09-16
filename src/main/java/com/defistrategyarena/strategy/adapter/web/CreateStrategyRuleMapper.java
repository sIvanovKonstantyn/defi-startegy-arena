package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.strategy.adapter.dsl.StrategyDslWireMapper;
import com.defistrategyarena.strategy.adapter.dsl.StrategyRuleWire;
import com.defistrategyarena.strategy.domain.StrategyDefinition;

public enum CreateStrategyRuleMapper {
    ;

    public static StrategyDefinition.Rule toRule(CreateStrategyHttpRequest.RuleBody body) {
        return StrategyDslWireMapper.toRule(
                new StrategyRuleWire(
                        body.id(),
                        body.conditionType(),
                        body.actionType(),
                        body.instrument(),
                        body.indicator(),
                        body.threshold(),
                        body.allocationPercent()));
    }
}
