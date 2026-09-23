package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;

public record DeleteStrategyFailed(String correlationId, String ownerId, String reasonCode)
        implements DomainEvent {}
