package com.defistrategyarena.identity.application;

import com.defistrategyarena.identity.domain.User;
import java.util.Objects;

public final class GetCurrentUser {

    private final ActiveSessionLookup sessions;
    private final UserRepository users;

    public GetCurrentUser(GetCurrentUserDeps deps) {
        this.sessions =
                new ActiveSessionLookup(
                        new ActiveSessionLookup.ActiveSessionLookupDeps(
                                deps.sessions(), deps.tokens(), deps.clock()));
        this.users = deps.users();
    }

    public User execute(AccessTokenQuery query) {
        Objects.requireNonNull(query);
        return users.findById(sessions.requireActive(query).userId())
                .orElseThrow(UnauthorizedException::new);
    }

    public record GetCurrentUserDeps(
            SessionRepository sessions,
            UserRepository users,
            SessionTokenFactory tokens,
            java.time.Clock clock) {}
}
