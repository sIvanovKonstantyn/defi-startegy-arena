package com.defistrategyarena.shared.infra.messaging;

import com.defistrategyarena.shared.messaging.OutboxRecord;
import com.defistrategyarena.shared.messaging.OutboxStore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

public final class JooqOutboxStore implements OutboxStore {

    private static final String DEPS_REQUIRED = "jooq outbox store deps must not be null";
    private static final String COMMAND_REQUIRED = "outbox append command must not be null";
    private static final String LIMIT_REQUIRED = "pending limit must not be null";
    private static final String ID_REQUIRED = "outbox id must not be null";
    private static final String FAILURE_REQUIRED = "outbox failure must not be null";
    private static final String TABLE_NAME = "messaging_outbox";
    private static final String COL_OUTBOX_ID = "outbox_id";
    private static final String COL_STATUS = "status";
    private static final String COL_CREATED_AT = "created_at";
    private static final String COL_PUBLISHED_AT = "published_at";
    private static final Table<Record> OUTBOX = DSL.table(DSL.name(TABLE_NAME));
    private static final Field<UUID> OUTBOX_ID =
            MessagingEventColumns.uuidColumn(new MessagingEventColumns.ColumnName(COL_OUTBOX_ID));
    private static final Field<String> STATUS = DSL.field(DSL.name(COL_STATUS), String.class);
    private static final Field<OffsetDateTime> CREATED_AT =
            DSL.field(DSL.name(COL_CREATED_AT), OffsetDateTime.class);
    private static final Field<OffsetDateTime> PUBLISHED_AT =
            DSL.field(DSL.name(COL_PUBLISHED_AT), OffsetDateTime.class);

    private final DSLContext dsl;

    public JooqOutboxStore(JooqOutboxStoreDeps deps) {
        Objects.requireNonNull(deps, DEPS_REQUIRED);
        this.dsl = deps.dsl();
    }

    @Override
    public void append(OutboxAppendCommand command) {
        Objects.requireNonNull(command, COMMAND_REQUIRED);
        dsl.insertInto(OUTBOX)
                .set(OUTBOX_ID, command.outboxId())
                .set(MessagingEventColumns.EVENT_TYPE, command.eventType())
                .set(MessagingEventColumns.PAYLOAD_JSON, command.payloadJson())
                .set(MessagingEventColumns.CORRELATION_ID, command.correlationId().orElse(null))
                .set(STATUS, OutboxRecord.OutboxStatus.PENDING.name())
                .set(CREATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .execute();
    }

    @Override
    public List<OutboxRecord> findPending(PendingLimit limit) {
        Objects.requireNonNull(limit, LIMIT_REQUIRED);
        return dsl.select(
                        OUTBOX_ID,
                        MessagingEventColumns.EVENT_TYPE,
                        MessagingEventColumns.PAYLOAD_JSON,
                        MessagingEventColumns.CORRELATION_ID,
                        STATUS,
                        CREATED_AT,
                        PUBLISHED_AT)
                .from(OUTBOX)
                .where(STATUS.eq(OutboxRecord.OutboxStatus.PENDING.name()))
                .orderBy(CREATED_AT.asc(), OUTBOX_ID.asc())
                .limit(limit.maxRows())
                .fetch(JooqOutboxStore::toRecord);
    }

    @Override
    public void markPublished(OutboxId id) {
        Objects.requireNonNull(id, ID_REQUIRED);
        dsl.update(OUTBOX)
                .set(STATUS, OutboxRecord.OutboxStatus.PUBLISHED.name())
                .set(PUBLISHED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .where(OUTBOX_ID.eq(id.value()))
                .execute();
    }

    @Override
    public void markFailed(OutboxFailure failure) {
        Objects.requireNonNull(failure, FAILURE_REQUIRED);
        dsl.update(OUTBOX)
                .set(STATUS, OutboxRecord.OutboxStatus.FAILED.name())
                .set(PUBLISHED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .where(OUTBOX_ID.eq(failure.outboxId()))
                .execute();
    }

    private static OutboxRecord toRecord(Record row) {
        String correlation = row.get(MessagingEventColumns.CORRELATION_ID);
        return new OutboxRecord(
                row.get(OUTBOX_ID),
                row.get(MessagingEventColumns.EVENT_TYPE),
                row.get(MessagingEventColumns.PAYLOAD_JSON),
                Optional.ofNullable(correlation).filter(text -> !text.isBlank()),
                OutboxRecord.OutboxStatus.valueOf(row.get(STATUS)));
    }

    public record JooqOutboxStoreDeps(DSLContext dsl) {}
}
