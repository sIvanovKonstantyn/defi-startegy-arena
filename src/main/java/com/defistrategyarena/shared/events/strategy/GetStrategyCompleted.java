package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;
import java.util.List;

public record GetStrategyCompleted(
        String correlationId,
        String strategyId,
        String name,
        String privacy,
        int versionNumber,
        List<CreateStrategyRequested.RulePayload> rules)
        implements DomainEvent {

    public GetStrategyCompleted {
        rules = List.copyOf(rules);
    }
}
