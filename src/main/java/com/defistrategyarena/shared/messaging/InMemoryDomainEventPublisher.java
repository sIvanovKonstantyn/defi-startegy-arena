package com.defistrategyarena.shared.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InMemoryDomainEventPublisher implements DomainEventPublisher {

    private static final String EVENT_REQUIRED = "domain event must not be null";

    private final List<DomainEvent> published = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, EVENT_REQUIRED);
        published.add(event);
    }

    public List<DomainEvent> published() {
        return List.copyOf(published);
    }
}
