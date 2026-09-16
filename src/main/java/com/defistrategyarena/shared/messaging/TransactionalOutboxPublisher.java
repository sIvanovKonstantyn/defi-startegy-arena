package com.defistrategyarena.shared.messaging;

import java.util.Objects;
import java.util.UUID;

public final class TransactionalOutboxPublisher implements DomainEventPublisher {

    private static final String EVENT_REQUIRED = "domain event must not be null";
    private static final String DEPS_REQUIRED = "outbox publisher deps must not be null";

    private final OutboxStore outbox;
    private final DomainEventCodec codec;

    public TransactionalOutboxPublisher(TransactionalOutboxPublisherDeps deps) {
        Objects.requireNonNull(deps, DEPS_REQUIRED);
        this.outbox = deps.outbox();
        this.codec = deps.codec();
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, EVENT_REQUIRED);
        DomainEventCodec.EncodedEvent encoded = codec.encode(event);
        outbox.append(
                new OutboxStore.OutboxAppendCommand(
                        UUID.randomUUID(),
                        encoded.eventType(),
                        encoded.payloadJson(),
                        encoded.correlationId()));
    }

    public record TransactionalOutboxPublisherDeps(OutboxStore outbox, DomainEventCodec codec) {}
}
