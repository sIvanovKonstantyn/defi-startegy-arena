package com.defistrategyarena.strategy.adapter.messaging;

import com.defistrategyarena.shared.events.strategy.CreateStrategyRequested;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyRuleMapper;
import com.defistrategyarena.strategy.adapter.web.StrategyRuleHttpMapper;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
public enum StrategyEventRuleMapper {
    ;

    public static List<StrategyDefinition.Rule> toRules(RulePayloadList payload) {
        List<StrategyDefinition.Rule> rules = new ArrayList<>();
        for (CreateStrategyRequested.RulePayload body : payload.rules()) {
            rules.add(CreateStrategyRuleMapper.toRule(CreateStrategyHttpRequest.RuleBody.fromEvent(body)));
        }
        return List.copyOf(rules);
    }

    public static List<CreateStrategyRequested.RulePayload> toPayloads(RuleDomainList rules) {
        List<CreateStrategyRequested.RulePayload> payloads = new ArrayList<>();
        for (CreateStrategyHttpRequest.RuleBody body :
                StrategyRuleHttpMapper.toBodies(rules.rules())) {
            payloads.add(body.toEvent());
        }
        return List.copyOf(payloads);
    }

    public record RulePayloadList(List<CreateStrategyRequested.RulePayload> rules) {
        public RulePayloadList {
            rules = List.copyOf(rules);
        }
    }

    public record RuleDomainList(List<StrategyDefinition.Rule> rules) {
        public RuleDomainList {
            rules = List.copyOf(rules);
        }
    }
}
