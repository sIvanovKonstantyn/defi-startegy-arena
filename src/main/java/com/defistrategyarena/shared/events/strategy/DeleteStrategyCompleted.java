package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;

public record DeleteStrategyCompleted(String correlationId, String ownerId, String strategyId)
        implements DomainEvent {}
