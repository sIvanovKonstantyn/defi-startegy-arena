package com.defistrategyarena.shared.infra.http;

public record HttpRequest(String method, String path, String body) {

    private static final String BODY_REQUIRED = "body must not be null";

    public HttpRequest {
        ValidatedHttpRouteKey routeKey = new ValidatedHttpRouteKey(method, path);
        method = routeKey.method();
        path = routeKey.path();
        if (body == null) {
            throw new IllegalArgumentException(BODY_REQUIRED);
        }
    }

    public static HttpRequest create(HttpRequest draft) {
        return new HttpRequest(draft.method(), draft.path(), draft.body());
    }
}
