package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;
import java.util.List;

public record GetStrategyCompleted(
        String correlationId,
        String ownerId,
        String strategyId,
        String name,
        String description,
        String privacy,
        int versionNumber,
        List<CreateStrategyRequested.RulePayload> rules,
        String pnl,
        String drawdown)
        implements DomainEvent {

    public static final String PENDING_METRIC = "";

    public GetStrategyCompleted {
        rules = List.copyOf(rules);
        if (description == null) {
            description = "";
        }
        if (pnl == null) {
            pnl = PENDING_METRIC;
        }
        if (drawdown == null) {
            drawdown = PENDING_METRIC;
        }
    }
}
