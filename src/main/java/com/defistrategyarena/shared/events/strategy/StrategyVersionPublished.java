package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;

public record StrategyVersionPublished(String strategyId, int versionNumber, String ownerId)
        implements DomainEvent {

    public static StrategyVersionPublished create(StrategyVersionPublished draft) {
        return new StrategyVersionPublished(draft.strategyId(), draft.versionNumber(), draft.ownerId());
    }
}
