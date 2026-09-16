package com.defistrategyarena.shared.messaging;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryOutboxStore implements OutboxStore {

    private static final String COMMAND_REQUIRED = "outbox append command must not be null";
    private static final String LIMIT_REQUIRED = "pending limit must not be null";
    private static final String ID_REQUIRED = "outbox id must not be null";
    private static final String FAILURE_REQUIRED = "outbox failure must not be null";

    private final Map<UUID, OutboxRecord> rows = new ConcurrentHashMap<>();

    @Override
    public void append(OutboxAppendCommand command) {
        Objects.requireNonNull(command, COMMAND_REQUIRED);
        rows.put(
                command.outboxId(),
                new OutboxRecord(
                        command.outboxId(),
                        command.eventType(),
                        command.payloadJson(),
                        command.correlationId(),
                        OutboxRecord.OutboxStatus.PENDING));
    }

    @Override
    public List<OutboxRecord> findPending(PendingLimit limit) {
        Objects.requireNonNull(limit, LIMIT_REQUIRED);
        List<OutboxRecord> pending = new ArrayList<>();
        rows.values().stream()
                .filter(row -> row.status() == OutboxRecord.OutboxStatus.PENDING)
                .sorted(Comparator.comparing(row -> row.outboxId().toString()))
                .limit(limit.maxRows())
                .forEach(pending::add);
        return List.copyOf(pending);
    }

    @Override
    public void markPublished(OutboxId id) {
        Objects.requireNonNull(id, ID_REQUIRED);
        updateStatus(new StatusUpdate(id.value(), OutboxRecord.OutboxStatus.PUBLISHED));
    }

    @Override
    public void markFailed(OutboxFailure failure) {
        Objects.requireNonNull(failure, FAILURE_REQUIRED);
        updateStatus(new StatusUpdate(failure.outboxId(), OutboxRecord.OutboxStatus.FAILED));
    }

    public int size() {
        return rows.size();
    }

    public Optional<OutboxRecord> findById(OutboxId id) {
        return Optional.ofNullable(rows.get(id.value()));
    }

    private void updateStatus(StatusUpdate update) {
        OutboxRecord existing = rows.get(update.id());
        if (existing == null) {
            return;
        }
        rows.put(
                update.id(),
                new OutboxRecord(
                        existing.outboxId(),
                        existing.eventType(),
                        existing.payloadJson(),
                        existing.correlationId(),
                        update.status()));
    }

    private record StatusUpdate(UUID id, OutboxRecord.OutboxStatus status) {}
}
