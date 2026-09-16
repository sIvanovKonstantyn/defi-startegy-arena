package com.defistrategyarena.shared.infra.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.messaging.InboxStore;
import com.defistrategyarena.shared.messaging.OutboxRecord;
import com.defistrategyarena.shared.messaging.OutboxStore;
import com.defistrategyarena.strategy.integration.H2PostgresModeSupport;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JooqOutboxStoreIT {

    private static final String DB_NAME = "messaging_outbox_it";
    private static final String EVENT_TYPE = "test.event";
    private static final String PAYLOAD = "{\"ok\":true}";
    private static final String CORRELATION_ID = "corr-it";
    private static final int PENDING_LIMIT = 10;
    private static final int SINGLE = 1;
    private static final int EMPTY = 0;

    private HikariDataSource dataSource;
    private JooqOutboxStore outbox;
    private JooqInboxStore inbox;

    @BeforeEach
    void setUp() {
        dataSource = H2PostgresModeSupport.dataSource(new H2PostgresModeSupport.DatabaseName(DB_NAME));
        DSLContext dsl = H2PostgresModeSupport.migratedDsl(dataSource);
        outbox = new JooqOutboxStore(new JooqOutboxStore.JooqOutboxStoreDeps(dsl));
        inbox = new JooqInboxStore(new JooqInboxStore.JooqInboxStoreDeps(dsl));
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void append_find_pending_mark_published_and_inbox_duplicate_claim() {
        UUID outboxId = UUID.randomUUID();
        outbox.append(
                new OutboxStore.OutboxAppendCommand(
                        outboxId, EVENT_TYPE, PAYLOAD, Optional.of(CORRELATION_ID)));

        List<OutboxRecord> pending =
                outbox.findPending(new OutboxStore.PendingLimit(PENDING_LIMIT));
        assertEquals(SINGLE, pending.size());
        OutboxRecord row = pending.getFirst();
        assertEquals(outboxId, row.outboxId());
        assertEquals(EVENT_TYPE, row.eventType());
        assertEquals(PAYLOAD, row.payloadJson());
        assertEquals(Optional.of(CORRELATION_ID), row.correlationId());
        assertEquals(OutboxRecord.OutboxStatus.PENDING, row.status());

        outbox.markPublished(new OutboxStore.OutboxId(outboxId));
        assertEquals(EMPTY, outbox.findPending(new OutboxStore.PendingLimit(PENDING_LIMIT)).size());

        InboxStore.InboxClaim claim =
                new InboxStore.InboxClaim(
                        outboxId, EVENT_TYPE, PAYLOAD, Optional.of(CORRELATION_ID));
        assertTrue(inbox.tryClaim(claim));
        assertFalse(inbox.tryClaim(claim));
    }

    @Test
    void append_with_blank_correlation_maps_to_empty_optional() {
        UUID outboxId = UUID.randomUUID();
        outbox.append(
                new OutboxStore.OutboxAppendCommand(
                        outboxId, EVENT_TYPE, PAYLOAD, Optional.of(" ")));
        List<OutboxRecord> pending =
                outbox.findPending(new OutboxStore.PendingLimit(PENDING_LIMIT));
        assertEquals(SINGLE, pending.size());
        assertTrue(pending.getFirst().correlationId().isEmpty());
    }
}
