package com.defistrategyarena.shared.messaging;

public interface DomainEventCodec {

    EncodedEvent encode(DomainEvent event);

    DomainEvent decode(EncodedEvent encoded);

    record EncodedEvent(String eventType, String payloadJson, java.util.Optional<String> correlationId) {}
}
