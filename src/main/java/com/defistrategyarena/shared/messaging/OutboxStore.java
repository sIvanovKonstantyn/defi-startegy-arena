package com.defistrategyarena.shared.messaging;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxStore {

    void append(OutboxAppendCommand command);

    List<OutboxRecord> findPending(PendingLimit limit);

    void markPublished(OutboxId id);

    void markFailed(OutboxFailure failure);

    record OutboxAppendCommand(
            UUID outboxId, String eventType, String payloadJson, Optional<String> correlationId) {}

    record PendingLimit(int maxRows) {}

    record OutboxId(UUID value) {}

    record OutboxFailure(UUID outboxId, String reason) {}
}
