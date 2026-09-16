package com.defistrategyarena.shared.infra.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.Test;

class JooqOutboxInboxIT {

    private static final String EVENT_TYPE = "test.event";
    private static final String PAYLOAD = "{\"ok\":true}";
    private static final String CORRELATION = "c-1";
    private static final String DB_NAME = "outbox_inbox_it";
    private static final int SINGLE = 1;
    private static final int EMPTY = 0;

    @Test
    void outbox_append_find_mark_and_inbox_claim() {
        try (HikariDataSource dataSource =
                H2PostgresModeSupport.dataSource(new H2PostgresModeSupport.DatabaseName(DB_NAME))) {
            DSLContext dsl = H2PostgresModeSupport.migratedDsl(dataSource);
            JooqOutboxStore outbox =
                    new JooqOutboxStore(new JooqOutboxStore.JooqOutboxStoreDeps(dsl));
            JooqInboxStore inbox = new JooqInboxStore(new JooqInboxStore.JooqInboxStoreDeps(dsl));
            UUID id = UUID.randomUUID();
            outbox.append(
                    new OutboxStore.OutboxAppendCommand(
                            id, EVENT_TYPE, PAYLOAD, Optional.of(CORRELATION)));
            List<OutboxRecord> pending =
                    outbox.findPending(new OutboxStore.PendingLimit(SINGLE));
            assertEquals(SINGLE, pending.size());
            assertEquals(OutboxRecord.OutboxStatus.PENDING, pending.get(EMPTY).status());
            assertTrue(
                    inbox.tryClaim(
                            new InboxStore.InboxClaim(
                                    id, EVENT_TYPE, PAYLOAD, Optional.of(CORRELATION))));
            assertFalse(
                    inbox.tryClaim(
                            new InboxStore.InboxClaim(
                                    id, EVENT_TYPE, PAYLOAD, Optional.of(CORRELATION))));
            outbox.markPublished(new OutboxStore.OutboxId(id));
            assertTrue(outbox.findPending(new OutboxStore.PendingLimit(SINGLE)).isEmpty());
            outbox.markFailed(new OutboxStore.OutboxFailure(UUID.randomUUID(), "ignored"));
            dsl.execute("drop table messaging_inbox");
            assertThrows(
                    org.jooq.exception.DataAccessException.class,
                    () ->
                            inbox.tryClaim(
                                    new InboxStore.InboxClaim(
                                            UUID.randomUUID(),
                                            EVENT_TYPE,
                                            PAYLOAD,
                                            Optional.of(CORRELATION))));
        }
    }
}
