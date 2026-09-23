package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;

public record UpdateStrategyCompleted(
        String correlationId, String ownerId, String strategyId, int versionNumber)
        implements DomainEvent {}
