package com.defistrategyarena.shared.infra.http;

import com.defistrategyarena.shared.realtime.UserSessionHub;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record WebSocketBinding(WebSocketAccessTokenAuth auth, UserSessionHub hub) {

    private static final String AUTH_REQUIRED = "websocket auth must not be null";
    private static final String HUB_REQUIRED = "session hub must not be null";

    public WebSocketBinding {
        Objects.requireNonNull(auth, AUTH_REQUIRED);
        Objects.requireNonNull(hub, HUB_REQUIRED);
    }
}
