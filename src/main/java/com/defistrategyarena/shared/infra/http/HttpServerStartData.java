package com.defistrategyarena.shared.infra.http;

import java.util.Optional;

public record HttpServerStartData(
        HttpServerConfig config, HttpRouteRegistry routes, Optional<WebSocketBinding> webSocket) {

    private static final String CONFIG_REQUIRED = "config must not be null";
    private static final String ROUTES_REQUIRED = "routes must not be null";
    private static final String WEB_SOCKET_REQUIRED = "websocket binding optional must not be null";

    public HttpServerStartData(HttpServerConfig config, HttpRouteRegistry routes) {
        this(config, routes, Optional.empty());
    }

    public HttpServerStartData {
        if (config == null) {
            throw new IllegalArgumentException(CONFIG_REQUIRED);
        }
        if (routes == null) {
            throw new IllegalArgumentException(ROUTES_REQUIRED);
        }
        if (webSocket == null) {
            throw new IllegalArgumentException(WEB_SOCKET_REQUIRED);
        }
    }

    public static HttpServerStartData create(HttpServerStartData draft) {
        return new HttpServerStartData(draft.config(), draft.routes(), draft.webSocket());
    }
}
