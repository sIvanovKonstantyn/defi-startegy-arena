package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;
import java.util.List;

public record UpdateStrategyRequested(
        String correlationId,
        String ownerId,
        String strategyId,
        String description,
        List<CreateStrategyRequested.RulePayload> rules)
        implements DomainEvent {

    public UpdateStrategyRequested {
        rules = List.copyOf(rules);
        if (description == null) {
            description = "";
        }
    }
}
