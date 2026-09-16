package com.defistrategyarena.identity.application;

import com.defistrategyarena.identity.domain.Session;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

final class ActiveSessionLookup {

    private static final String QUERY_REQUIRED = "access token query must not be null";

    private final SessionRepository sessions;
    private final SessionTokenFactory tokens;
    private final Clock clock;

    ActiveSessionLookup(ActiveSessionLookupDeps deps) {
        this.sessions = deps.sessions();
        this.tokens = deps.tokens();
        this.clock = deps.clock();
    }

    Session requireActive(AccessTokenQuery query) {
        Objects.requireNonNull(query, QUERY_REQUIRED);
        return rejectIfExpired(loadSession(query));
    }

    private Session loadSession(AccessTokenQuery query) {
        String tokenHash = tokens.hash(new SessionTokenFactory.RawToken(query.accessToken()));
        Optional<Session> session =
                sessions.findByTokenHash(new SessionRepository.TokenHashLookup(tokenHash));
        if (session.isEmpty()) {
            throw new UnauthorizedException();
        }
        return session.get();
    }

    private Session rejectIfExpired(Session found) {
        if (found.isExpiredAt(new Session.InstantReference(clock.instant()))) {
            sessions.delete(found.id());
            throw new UnauthorizedException();
        }
        return found;
    }

    record ActiveSessionLookupDeps(
            SessionRepository sessions, SessionTokenFactory tokens, Clock clock) {}
}
