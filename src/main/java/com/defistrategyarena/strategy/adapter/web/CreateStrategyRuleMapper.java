package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.strategy.adapter.dsl.StrategyActionWire;
import com.defistrategyarena.strategy.adapter.dsl.StrategyConditionWire;
import com.defistrategyarena.strategy.adapter.dsl.StrategyDslWireMapper;
import com.defistrategyarena.strategy.adapter.dsl.StrategyRuleWire;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.ArrayList;
import java.util.List;

public final class CreateStrategyRuleMapper {

    private CreateStrategyRuleMapper() {}

    public static StrategyDefinition.Rule toRule(CreateStrategyHttpRequest.RuleBody body) {
        return StrategyDslWireMapper.toRule(
                new StrategyRuleWire(body.id(), toConditionWire(body.when()), toActionWire(body.then())));
    }

    private static StrategyConditionWire toConditionWire(CreateStrategyHttpRequest.ConditionBody body) {
        List<StrategyConditionWire> children = new ArrayList<>();
        for (CreateStrategyHttpRequest.ConditionBody child : body.children()) {
            children.add(toConditionWire(child));
        }
        return new StrategyConditionWire(
                body.type(),
                children,
                body.instrument(),
                body.indicator(),
                body.operator(),
                body.threshold(),
                body.parameters());
    }

    private static StrategyActionWire toActionWire(CreateStrategyHttpRequest.ActionBody body) {
        return new StrategyActionWire(
                body.type(),
                body.instrument(),
                body.instrumentPair(),
                body.allocationPercent(),
                body.yearlyFeePercent());
    }
}
