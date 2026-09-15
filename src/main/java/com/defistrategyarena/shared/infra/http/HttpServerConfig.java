package com.defistrategyarena.shared.infra.http;

public record HttpServerConfig(int port) {

    private static final int MIN_ALLOWED_PORT = 0;
    private static final String PORT_MUST_NOT_BE_NEGATIVE = "port must not be negative";

    public HttpServerConfig {
        if (port < MIN_ALLOWED_PORT) {
            throw new IllegalArgumentException(PORT_MUST_NOT_BE_NEGATIVE);
        }
    }

    public static HttpServerConfig create(HttpServerConfig draft) {
        return new HttpServerConfig(draft.port());
    }
}
