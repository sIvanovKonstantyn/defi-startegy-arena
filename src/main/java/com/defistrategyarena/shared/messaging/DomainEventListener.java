package com.defistrategyarena.shared.messaging;

public interface DomainEventListener<E extends DomainEvent> {

    void on(E event);
}
