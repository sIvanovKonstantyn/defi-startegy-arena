package com.defistrategyarena.shared.infra.http;

public record HttpServerStartData(HttpServerConfig config, HttpRouteRegistry routes) {

    private static final String CONFIG_REQUIRED = "config must not be null";
    private static final String ROUTES_REQUIRED = "routes must not be null";

    public HttpServerStartData {
        if (config == null) {
            throw new IllegalArgumentException(CONFIG_REQUIRED);
        }
        if (routes == null) {
            throw new IllegalArgumentException(ROUTES_REQUIRED);
        }
    }

    public static HttpServerStartData create(HttpServerStartData draft) {
        return new HttpServerStartData(draft.config(), draft.routes());
    }
}
