package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;

public record DeleteStrategyFailed(String correlationId, String reasonCode) implements DomainEvent {}
