package com.defistrategyarena.bootstrap;

import com.defistrategyarena.identity.application.AccessTokenQuery;
import com.defistrategyarena.identity.application.GetCurrentUser;
import com.defistrategyarena.identity.application.UnauthorizedException;
import com.defistrategyarena.shared.infra.http.WebSocketAccessTokenAuth;
import java.util.Objects;
import java.util.Optional;

final class GetCurrentUserWebSocketAuth implements WebSocketAccessTokenAuth {

    private static final String DEPS_REQUIRED = "websocket auth deps must not be null";

    private final GetCurrentUser getCurrentUser;

    GetCurrentUserWebSocketAuth(GetCurrentUserWebSocketAuthDeps deps) {
        Objects.requireNonNull(deps, DEPS_REQUIRED);
        this.getCurrentUser = deps.getCurrentUser();
    }

    @Override
    public Optional<WebSocketPrincipal> authenticate(AccessTokenCommand command) {
        try {
            return Optional.of(
                    new WebSocketPrincipal(
                            getCurrentUser
                                    .execute(new AccessTokenQuery(command.accessToken()))
                                    .id()
                                    .value()));
        } catch (UnauthorizedException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    record GetCurrentUserWebSocketAuthDeps(GetCurrentUser getCurrentUser) {}
}
