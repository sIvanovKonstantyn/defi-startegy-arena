package com.defistrategyarena.shared.messaging;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
