package com.defistrategyarena.shared.infra.messaging;

import com.defistrategyarena.shared.messaging.InboxStore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

public final class JooqInboxStore implements InboxStore {

    private static final String DEPS_REQUIRED = "jooq inbox store deps must not be null";
    private static final String CLAIM_REQUIRED = "inbox claim must not be null";
    private static final String TABLE_NAME = "messaging_inbox";
    private static final String COL_EVENT_ID = "event_id";
    private static final String COL_PROCESSED_AT = "processed_at";
    private static final String SQLSTATE_UNIQUE = "23505";
    private static final Table<Record> INBOX = DSL.table(DSL.name(TABLE_NAME));
    private static final Field<UUID> EVENT_ID =
            MessagingEventColumns.uuidColumn(new MessagingEventColumns.ColumnName(COL_EVENT_ID));
    private static final Field<OffsetDateTime> PROCESSED_AT =
            DSL.field(DSL.name(COL_PROCESSED_AT), OffsetDateTime.class);

    private final DSLContext dsl;

    public JooqInboxStore(JooqInboxStoreDeps deps) {
        Objects.requireNonNull(deps, DEPS_REQUIRED);
        this.dsl = deps.dsl();
    }

    @Override
    public boolean tryClaim(InboxClaim claim) {
        Objects.requireNonNull(claim, CLAIM_REQUIRED);
        try {
            dsl.insertInto(INBOX)
                    .set(EVENT_ID, claim.eventId())
                    .set(MessagingEventColumns.EVENT_TYPE, claim.eventType())
                    .set(MessagingEventColumns.PAYLOAD_JSON, claim.payloadJson())
                    .set(MessagingEventColumns.CORRELATION_ID, claim.correlationId().orElse(null))
                    .set(PROCESSED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                    .execute();
            return true;
        } catch (DataAccessException exception) {
            if (isUniqueViolation(exception)) {
                return false;
            }
            throw exception;
        }
    }

    private static boolean isUniqueViolation(DataAccessException exception) {
        return SQLSTATE_UNIQUE.equals(exception.sqlState());
    }

    public record JooqInboxStoreDeps(DSLContext dsl) {}
}
