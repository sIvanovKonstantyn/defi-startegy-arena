package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;

public record UpdateStrategyFailed(String correlationId, String ownerId, String reasonCode)
        implements DomainEvent {}
