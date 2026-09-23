package com.defistrategyarena.identity.adapter.persistence;

import static com.defistrategyarena.identity.adapter.persistence.jooq.tables.AuthSessions.AUTH_SESSIONS;

import com.defistrategyarena.identity.application.SessionRepository;
import com.defistrategyarena.identity.domain.Session;
import com.defistrategyarena.identity.domain.SessionId;
import com.defistrategyarena.identity.domain.UserId;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;

public final class JooqSessionRepository implements SessionRepository {

    private static final String DSL_REQUIRED = "dsl context must not be null";

    private final DSLContext dsl;

    public JooqSessionRepository(DSLContext dsl) {
        if (dsl == null) {
            throw new IllegalArgumentException(DSL_REQUIRED);
        }
        this.dsl = dsl;
    }

    @Override
    public void save(Session session) {
        dsl.insertInto(AUTH_SESSIONS)
                .set(AUTH_SESSIONS.SESSION_ID, uuid(session.id()))
                .set(AUTH_SESSIONS.USER_ID, uuid(session.userId()))
                .set(AUTH_SESSIONS.TOKEN_HASH, session.tokenHash())
                .set(AUTH_SESSIONS.EXPIRES_AT, toOffset(session.expiresAt()))
                .set(AUTH_SESSIONS.CREATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .execute();
    }

    @Override
    public void delete(SessionId id) {
        dsl.deleteFrom(AUTH_SESSIONS).where(AUTH_SESSIONS.SESSION_ID.eq(uuid(id))).execute();
    }

    @Override
    public Optional<Session> findByTokenHash(TokenHashLookup lookup) {
        return dsl.selectFrom(AUTH_SESSIONS)
                .where(AUTH_SESSIONS.TOKEN_HASH.eq(lookup.tokenHash()))
                .fetchOptional()
                .map(JooqSessionRepository::toSession);
    }

    private static Session toSession(Record record) {
        return Session.rehydrate(
                new Session.RehydrateSessionData(
                        new SessionId(record.get(AUTH_SESSIONS.SESSION_ID).toString()),
                        new UserId(record.get(AUTH_SESSIONS.USER_ID).toString()),
                        record.get(AUTH_SESSIONS.TOKEN_HASH),
                        record.get(AUTH_SESSIONS.EXPIRES_AT).toInstant()));
    }

    private static UUID uuid(SessionId id) {
        return UUID.fromString(id.value());
    }

    private static UUID uuid(UserId id) {
        return UUID.fromString(id.value());
    }

    private static OffsetDateTime toOffset(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
