package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;

public record ListStrategiesRequested(
        String correlationId,
        String ownerId,
        int page,
        int size,
        String sort,
        String order)
        implements DomainEvent {}
