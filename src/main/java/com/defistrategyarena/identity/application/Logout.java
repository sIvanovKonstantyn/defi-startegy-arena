package com.defistrategyarena.identity.application;

import java.util.Objects;

public final class Logout {

    private final ActiveSessionLookup sessions;
    private final SessionRepository sessionRepository;

    public Logout(LogoutDeps deps) {
        this.sessionRepository = deps.sessions();
        this.sessions =
                new ActiveSessionLookup(
                        new ActiveSessionLookup.ActiveSessionLookupDeps(
                                deps.sessions(), deps.tokens(), deps.clock()));
    }

    public void execute(AccessTokenQuery query) {
        Objects.requireNonNull(query);
        sessionRepository.delete(sessions.requireActive(query).id());
    }

    public record LogoutDeps(
            SessionRepository sessions, SessionTokenFactory tokens, java.time.Clock clock) {}
}
