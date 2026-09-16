package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.strategy.adapter.dsl.StrategyDslWireMapper;
import com.defistrategyarena.strategy.adapter.dsl.StrategyRuleWire;
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
        StrategyRuleWire wire = StrategyDslWireMapper.toWire(rule);
        return new CreateStrategyHttpRequest.RuleBody(
                wire.id(),
                wire.conditionType(),
                wire.actionType(),
                wire.instrument(),
                wire.indicator(),
                wire.threshold(),
                wire.allocationPercent());
    }
}
