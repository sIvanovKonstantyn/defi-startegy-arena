package com.defistrategyarena.shared.infra.http;

public record HttpRequest(String method, String path, String body) {

    public HttpRequest {
        ValidatedHttpRouteKey routeKey = new ValidatedHttpRouteKey(method, path);
        method = routeKey.method();
        path = routeKey.path();
        if (body == null) {
            body = "";
        }
    }

    public static HttpRequest create(HttpRequest draft) {
        return new HttpRequest(draft.method(), draft.path(), draft.body());
    }
}
