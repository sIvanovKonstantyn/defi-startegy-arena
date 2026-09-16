package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;
import java.util.List;

public record UpdateStrategyRequested(
        String correlationId,
        String ownerId,
        String strategyId,
        List<CreateStrategyRequested.RulePayload> rules)
        implements DomainEvent {

    public UpdateStrategyRequested {
        rules = List.copyOf(rules);
    }
}
