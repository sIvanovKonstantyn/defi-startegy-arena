package com.defistrategyarena.shared.messaging;

import java.util.List;
import java.util.Objects;

public final class OutboxRelay {

    private static final String DEPS_REQUIRED = "outbox relay deps must not be null";
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_DRAIN_ROUNDS = 32;
    private static final int EMPTY = 0;

    private final OutboxStore outbox;
    private final InboxStore inbox;
    private final DomainEventCodec codec;
    private final DomainEventListenerRegistry registry;
    private final TransactionRunner transactions;

    public OutboxRelay(OutboxRelayDeps deps) {
        Objects.requireNonNull(deps, DEPS_REQUIRED);
        this.outbox = deps.outbox();
        this.inbox = deps.inbox();
        this.codec = deps.codec();
        this.registry = deps.registry();
        this.transactions = deps.transactions();
    }

    public void drain() {
        OutboxStore.PendingLimit limit = new OutboxStore.PendingLimit(DEFAULT_BATCH_SIZE);
        int round = EMPTY;
        while (round < MAX_DRAIN_ROUNDS) {
            List<OutboxRecord> pending = outbox.findPending(limit);
            if (pending.isEmpty()) {
                return;
            }
            for (OutboxRecord record : pending) {
                deliver(record);
            }
            round++;
        }
    }

    private void deliver(OutboxRecord record) {
        try {
            transactions.run(
                    () -> {
                        DomainEvent event =
                                codec.decode(
                                        new DomainEventCodec.EncodedEvent(
                                                record.eventType(),
                                                record.payloadJson(),
                                                record.correlationId()));
                        boolean claimed =
                                inbox.tryClaim(
                                        new InboxStore.InboxClaim(
                                                record.outboxId(),
                                                record.eventType(),
                                                record.payloadJson(),
                                                record.correlationId()));
                        if (claimed) {
                            registry.dispatch(event);
                        }
                        outbox.markPublished(new OutboxStore.OutboxId(record.outboxId()));
                    });
        } catch (RuntimeException exception) {
            outbox.markFailed(
                    new OutboxStore.OutboxFailure(record.outboxId(), exception.getMessage()));
        }
    }

    public record OutboxRelayDeps(
            OutboxStore outbox,
            InboxStore inbox,
            DomainEventCodec codec,
            DomainEventListenerRegistry registry,
            TransactionRunner transactions) {}
}
