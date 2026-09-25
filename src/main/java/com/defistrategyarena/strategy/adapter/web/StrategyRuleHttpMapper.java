package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.strategy.adapter.dsl.StrategyActionWire;
import com.defistrategyarena.strategy.adapter.dsl.StrategyConditionWire;
import com.defistrategyarena.strategy.adapter.dsl.StrategyDslWireMapper;
import com.defistrategyarena.strategy.adapter.dsl.StrategyRuleWire;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.ArrayList;
import java.util.List;

public final class StrategyRuleHttpMapper {

    private StrategyRuleHttpMapper() {}

    public static List<CreateStrategyHttpRequest.RuleBody> toBodies(List<StrategyDefinition.Rule> rules) {
        List<CreateStrategyHttpRequest.RuleBody> bodies = new ArrayList<>();
        for (StrategyDefinition.Rule rule : rules) {
            StrategyRuleWire wire = StrategyDslWireMapper.toWire(rule);
            bodies.add(
                    new CreateStrategyHttpRequest.RuleBody(
                            wire.id(), toConditionBody(wire.when()), toActionBody(wire.then())));
        }
        return List.copyOf(bodies);
    }

    private static CreateStrategyHttpRequest.ConditionBody toConditionBody(StrategyConditionWire wire) {
        List<CreateStrategyHttpRequest.ConditionBody> children = new ArrayList<>();
        for (StrategyConditionWire child : wire.children()) {
            children.add(toConditionBody(child));
        }
        return new CreateStrategyHttpRequest.ConditionBody(
                wire.type(),
                children,
                wire.instrument(),
                wire.indicator(),
                wire.operator(),
                wire.threshold(),
                wire.parameters());
    }

    private static CreateStrategyHttpRequest.ActionBody toActionBody(StrategyActionWire wire) {
        return new CreateStrategyHttpRequest.ActionBody(
                wire.type(),
                wire.instrument(),
                wire.instrumentPair(),
                wire.allocationPercent(),
                wire.yearlyFeePercent());
    }
}
