package com.defistrategyarena.shared.infra.http;

import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface WebSocketAccessTokenAuth {

    Optional<WebSocketPrincipal> authenticate(AccessTokenCommand command);

    record AccessTokenCommand(String accessToken) {}

    record WebSocketPrincipal(String userId) {}
}
