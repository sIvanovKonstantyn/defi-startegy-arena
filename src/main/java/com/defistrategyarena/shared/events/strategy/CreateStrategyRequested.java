package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;
import java.util.List;

public record CreateStrategyRequested(
        String correlationId, String ownerId, String name, List<RulePayload> rules)
        implements DomainEvent {

    public CreateStrategyRequested {
        rules = List.copyOf(rules);
    }

    public record RulePayload(
            String id,
            String conditionType,
            String actionType,
            String instrument,
            String indicator,
            String threshold,
            String allocationPercent) {}
}
