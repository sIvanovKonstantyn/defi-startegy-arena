package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;

public record DeleteStrategyRequested(String correlationId, String ownerId, String strategyId)
        implements DomainEvent {}
