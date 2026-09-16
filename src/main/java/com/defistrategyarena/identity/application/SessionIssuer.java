package com.defistrategyarena.identity.application;

import com.defistrategyarena.identity.domain.Session;
import com.defistrategyarena.identity.domain.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class SessionIssuer {

    private static final String DEPS_REQUIRED = "session issuer deps must not be null";
    private static final String USER_REQUIRED = "user id must not be null";

    private final SessionRepository sessions;
    private final SessionTokenFactory tokens;
    private final Clock clock;
    private final long sessionTtlSeconds;

    public SessionIssuer(SessionIssuerDeps deps) {
        Objects.requireNonNull(deps, DEPS_REQUIRED);
        this.sessions = deps.sessions();
        this.tokens = deps.tokens();
        this.clock = deps.clock();
        this.sessionTtlSeconds = deps.sessionTtlSeconds();
    }

    public AuthSessionResult issue(UserId userId) {
        Objects.requireNonNull(userId, USER_REQUIRED);
        SessionTokenFactory.IssuedToken issued = tokens.issue();
        Instant expiresAt = clock.instant().plusSeconds(sessionTtlSeconds);
        Session session =
                Session.create(new Session.CreateSessionData(userId, issued.tokenHash(), expiresAt));
        sessions.save(session);
        return new AuthSessionResult(issued.rawToken());
    }

    public record SessionIssuerDeps(
            SessionRepository sessions,
            SessionTokenFactory tokens,
            Clock clock,
            long sessionTtlSeconds) {}
}
