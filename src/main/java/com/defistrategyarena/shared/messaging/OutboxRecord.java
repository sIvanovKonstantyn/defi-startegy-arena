package com.defistrategyarena.shared.messaging;

import java.util.Optional;
import java.util.UUID;

public record OutboxRecord(
        UUID outboxId,
        String eventType,
        String payloadJson,
        Optional<String> correlationId,
        OutboxStatus status) {

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }
}
