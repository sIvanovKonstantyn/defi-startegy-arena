package com.defistrategyarena.shared.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class DomainEventListenerRegistry {

    private static final String TYPE_REQUIRED = "event type must not be null";
    private static final String LISTENER_REQUIRED = "listener must not be null";

    private final Map<Class<? extends DomainEvent>, List<Consumer<DomainEvent>>> listeners =
            new ConcurrentHashMap<>();

    public <E extends DomainEvent> void register(ListenerRegistration<E> registration) {
        Objects.requireNonNull(registration.type(), TYPE_REQUIRED);
        Objects.requireNonNull(registration.listener(), LISTENER_REQUIRED);
        listeners
                .computeIfAbsent(registration.type(), ignored -> new ArrayList<>())
                .add(event -> registration.listener().on(registration.type().cast(event)));
    }

    public void dispatch(DomainEvent event) {
        Objects.requireNonNull(event);
        List<Consumer<DomainEvent>> matched = listeners.getOrDefault(event.getClass(), List.of());
        for (Consumer<DomainEvent> listener : matched) {
            listener.accept(event);
        }
    }

    public record ListenerRegistration<E extends DomainEvent>(
            Class<E> type, DomainEventListener<E> listener) {}
}
