package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.events.strategy.CreateStrategyRequested;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import java.util.ArrayList;
import java.util.List;

enum BootstrapRulePayloads {
    ;

    static List<CreateStrategyRequested.RulePayload> fromHttp(RuleBodies bodies) {
        List<CreateStrategyRequested.RulePayload> payloads = new ArrayList<>();
        for (CreateStrategyHttpRequest.RuleBody rule : bodies.rules()) {
            payloads.add(rule.toEvent());
        }
        return List.copyOf(payloads);
    }

    record RuleBodies(List<CreateStrategyHttpRequest.RuleBody> rules) {}
}
