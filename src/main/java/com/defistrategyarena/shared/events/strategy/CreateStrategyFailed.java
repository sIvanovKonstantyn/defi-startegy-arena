package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;

public record CreateStrategyFailed(String correlationId, String ownerId, String reasonCode)
        implements DomainEvent {}
