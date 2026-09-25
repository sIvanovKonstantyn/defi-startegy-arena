package com.defistrategyarena.shared.unit.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.events.strategy.CreateStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.CreateStrategyRequested;
import com.defistrategyarena.shared.events.strategy.StrategyEventTypes;
import com.defistrategyarena.shared.events.strategy.StrategyVersionPublished;
import com.defistrategyarena.shared.infra.messaging.JacksonDomainEventCodec;
import com.defistrategyarena.shared.messaging.DomainEvent;
import com.defistrategyarena.shared.messaging.DomainEventCodec;
import com.defistrategyarena.shared.messaging.DomainEventListenerRegistry;
import com.defistrategyarena.shared.messaging.ImmediateTransactionRunner;
import com.defistrategyarena.shared.messaging.InMemoryInboxStore;
import com.defistrategyarena.shared.messaging.InMemoryOutboxStore;
import com.defistrategyarena.shared.messaging.InboxStore;
import com.defistrategyarena.shared.messaging.OutboxRecord;
import com.defistrategyarena.shared.messaging.OutboxRelay;
import com.defistrategyarena.shared.messaging.OutboxStore;
import com.defistrategyarena.shared.messaging.TransactionalOutboxPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OutboxRelayTest {

    private static final String CORRELATION = "corr-1";
    private static final String OWNER = "owner-1";
    private static final String NAME = "alpha";
    private static final String DESCRIPTION = "alpha description";
    private static final String EMPTY_TEXT = "";
    private static final String BAD_PAYLOAD = "{";
    private static final String UNKNOWN_TYPE = "unknown.event";
    private static final int EMPTY = 0;
    private static final int SINGLE = 1;
    private static final int TWO = 2;
    private static final int BATCH = 10;
    private static final int VERSION = 1;

    @Test
    void publish_drain_delivers_request_and_response() {
        InMemoryOutboxStore outbox = new InMemoryOutboxStore();
        InMemoryInboxStore inbox = new InMemoryInboxStore();
        JacksonDomainEventCodec codec =
                new JacksonDomainEventCodec(
                        new JacksonDomainEventCodec.JacksonDomainEventCodecDeps(
                                new ObjectMapper(), StrategyEventTypes.catalog()));
        DomainEventListenerRegistry registry = new DomainEventListenerRegistry();
        AtomicInteger delivered = new AtomicInteger(EMPTY);
        List<String> completed = new ArrayList<>();
        TransactionalOutboxPublisher publisher =
                new TransactionalOutboxPublisher(
                        new TransactionalOutboxPublisher.TransactionalOutboxPublisherDeps(
                                outbox, codec));
        registry.register(
                new DomainEventListenerRegistry.ListenerRegistration<>(
                        CreateStrategyRequested.class,
                        event -> {
                            delivered.incrementAndGet();
                            publisher.publish(
                                    new CreateStrategyCompleted(
                                            event.correlationId(), event.ownerId(), "strategy-1"));
                        }));
        registry.register(
                new DomainEventListenerRegistry.ListenerRegistration<>(
                        CreateStrategyCompleted.class,
                        event -> completed.add(event.correlationId())));
        OutboxRelay relay =
                new OutboxRelay(
                        new OutboxRelay.OutboxRelayDeps(
                                outbox,
                                inbox,
                                codec,
                                registry,
                                new ImmediateTransactionRunner()));
        publisher.publish(new CreateStrategyRequested(CORRELATION, OWNER, NAME, DESCRIPTION, List.of()));
        relay.drain();
        assertEquals(SINGLE, delivered.get());
        assertEquals(List.of(CORRELATION), completed);
        assertTrue(outbox.findPending(new OutboxStore.PendingLimit(BATCH)).isEmpty());
        assertEquals(TWO, outbox.size());
        assertEquals(TWO, inbox.size());
    }

    @Test
    void drain_marks_failed_when_decode_breaks_and_stops_after_max_rounds() {
        InMemoryOutboxStore outbox = new InMemoryOutboxStore();
        UUID id = UUID.randomUUID();
        outbox.append(
                new OutboxStore.OutboxAppendCommand(
                        id, CreateStrategyRequested.class.getName(), BAD_PAYLOAD, Optional.of(CORRELATION)));
        OutboxRelay relay =
                new OutboxRelay(
                        new OutboxRelay.OutboxRelayDeps(
                                outbox,
                                new InMemoryInboxStore(),
                                new JacksonDomainEventCodec(
                                        new JacksonDomainEventCodec.JacksonDomainEventCodecDeps(
                                                new ObjectMapper(), StrategyEventTypes.catalog())),
                                new DomainEventListenerRegistry(),
                                new ImmediateTransactionRunner()));
        relay.drain();
        Optional<OutboxRecord> failed = outbox.findById(new OutboxStore.OutboxId(id));
        assertTrue(failed.isPresent());
        assertEquals(OutboxRecord.OutboxStatus.FAILED, failed.get().status());
    }

    @Test
    void mark_failed_and_missing_id_are_safe() {
        InMemoryOutboxStore outbox = new InMemoryOutboxStore();
        UUID missing = UUID.randomUUID();
        outbox.markFailed(new OutboxStore.OutboxFailure(missing, "gone"));
        assertTrue(outbox.findById(new OutboxStore.OutboxId(missing)).isEmpty());
    }

    @Test
    void drain_skips_dispatch_when_inbox_already_claimed() {
        InMemoryOutboxStore outbox = new InMemoryOutboxStore();
        InMemoryInboxStore inbox = new InMemoryInboxStore();
        JacksonDomainEventCodec codec =
                new JacksonDomainEventCodec(
                        new JacksonDomainEventCodec.JacksonDomainEventCodecDeps(
                                new ObjectMapper(), StrategyEventTypes.catalog()));
        UUID id = UUID.randomUUID();
        DomainEvent event = new CreateStrategyRequested(CORRELATION, OWNER, NAME, DESCRIPTION, List.of());
        DomainEventCodec.EncodedEvent encoded = codec.encode(event);
        outbox.append(
                new OutboxStore.OutboxAppendCommand(
                        id, encoded.eventType(), encoded.payloadJson(), encoded.correlationId()));
        inbox.tryClaim(
                new InboxStore.InboxClaim(
                        id, encoded.eventType(), encoded.payloadJson(), encoded.correlationId()));
        AtomicInteger delivered = new AtomicInteger(EMPTY);
        DomainEventListenerRegistry registry = new DomainEventListenerRegistry();
        registry.register(
                new DomainEventListenerRegistry.ListenerRegistration<>(
                        CreateStrategyRequested.class, ignored -> delivered.incrementAndGet()));
        new OutboxRelay(
                        new OutboxRelay.OutboxRelayDeps(
                                outbox,
                                inbox,
                                codec,
                                registry,
                                new ImmediateTransactionRunner()))
                .drain();
        assertEquals(EMPTY, delivered.get());
        assertTrue(outbox.findPending(new OutboxStore.PendingLimit(BATCH)).isEmpty());
    }

    @Test
    void drain_stops_after_max_rounds_when_pending_never_clears() {
        OutboxRecord sticky =
                new OutboxRecord(
                        UUID.randomUUID(),
                        CreateStrategyRequested.class.getName(),
                        BAD_PAYLOAD,
                        Optional.of(CORRELATION),
                        OutboxRecord.OutboxStatus.PENDING);
        OutboxStore stickyOutbox =
                new OutboxStore() {
                    @Override
                    public void append(OutboxAppendCommand command) {}

                    @Override
                    public List<OutboxRecord> findPending(PendingLimit limit) {
                        return List.of(sticky);
                    }

                    @Override
                    public void markPublished(OutboxId id) {}

                    @Override
                    public void markFailed(OutboxFailure failure) {}
                };
        new OutboxRelay(
                        new OutboxRelay.OutboxRelayDeps(
                                stickyOutbox,
                                new InMemoryInboxStore(),
                                new JacksonDomainEventCodec(
                                        new JacksonDomainEventCodec.JacksonDomainEventCodecDeps(
                                                new ObjectMapper(), StrategyEventTypes.catalog())),
                                new DomainEventListenerRegistry(),
                                new ImmediateTransactionRunner()))
                .drain();
    }

    @Test
    void codec_covers_unknown_type_bad_json_encode_failure_and_blank_correlation()
            throws JsonProcessingException {
        ObjectMapper failingMapper =
                new ObjectMapper() {
                    @Override
                    public String writeValueAsString(Object value) throws JsonProcessingException {
                        throw new JsonProcessingException("boom") {};
                    }
                };
        JacksonDomainEventCodec failingCodec =
                new JacksonDomainEventCodec(
                        new JacksonDomainEventCodec.JacksonDomainEventCodecDeps(
                                failingMapper, StrategyEventTypes.catalog()));
        assertThrows(
                IllegalStateException.class,
                () -> failingCodec.encode(new CreateStrategyRequested(CORRELATION, OWNER, NAME, DESCRIPTION, List.of())));

        JacksonDomainEventCodec codec =
                new JacksonDomainEventCodec(
                        new JacksonDomainEventCodec.JacksonDomainEventCodecDeps(
                                new ObjectMapper(), StrategyEventTypes.catalog()));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        codec.decode(
                                new DomainEventCodec.EncodedEvent(
                                        UNKNOWN_TYPE, "{}", Optional.empty())));
        assertThrows(
                IllegalStateException.class,
                () ->
                        codec.decode(
                                new DomainEventCodec.EncodedEvent(
                                        CreateStrategyRequested.class.getName(),
                                        BAD_PAYLOAD,
                                        Optional.empty())));
        DomainEventCodec.EncodedEvent blankCorrelation =
                codec.encode(new CreateStrategyRequested(EMPTY_TEXT, OWNER, NAME, DESCRIPTION, List.of()));
        assertTrue(blankCorrelation.correlationId().isEmpty());
        DomainEventCodec.EncodedEvent noAccessor =
                codec.encode(new StrategyVersionPublished("s1", VERSION, OWNER));
        assertTrue(noAccessor.correlationId().isEmpty());
        DomainEventCodec.EncodedEvent nonString =
                codec.encode(new NonStringCorrelationEvent(VERSION));
        assertTrue(nonString.correlationId().isEmpty());
    }

    private record NonStringCorrelationEvent(Integer correlationId) implements DomainEvent {}
}
