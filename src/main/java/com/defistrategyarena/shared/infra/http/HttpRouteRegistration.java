package com.defistrategyarena.shared.infra.http;

public record HttpRouteRegistration(String method, String path, HttpHandler handler) {

    private static final String HANDLER_REQUIRED = "handler must not be null";

    public HttpRouteRegistration {
        ValidatedHttpRouteKey routeKey = new ValidatedHttpRouteKey(method, path);
        method = routeKey.method();
        path = routeKey.path();
        if (handler == null) {
            throw new IllegalArgumentException(HANDLER_REQUIRED);
        }
    }

    public static HttpRouteRegistration create(HttpRouteRegistration draft) {
        return new HttpRouteRegistration(draft.method(), draft.path(), draft.handler());
    }
}
