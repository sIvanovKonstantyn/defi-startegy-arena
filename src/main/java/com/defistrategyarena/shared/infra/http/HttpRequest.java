package com.defistrategyarena.shared.infra.http;

import java.util.Map;

public record HttpRequest(
        String method,
        String path,
        String body,
        Map<String, String> query,
        Map<String, String> pathVariables) {

    private static final String BODY_REQUIRED = "body must not be null";
    private static final String QUERY_REQUIRED = "query must not be null";
    private static final String PATH_VARIABLES_REQUIRED = "path variables must not be null";

    public HttpRequest(String method, String path, String body) {
        this(method, path, body, Map.of(), Map.of());
    }

    public HttpRequest {
        ValidatedHttpRouteKey routeKey = new ValidatedHttpRouteKey(method, path);
        method = routeKey.method();
        path = routeKey.path();
        if (body == null) {
            throw new IllegalArgumentException(BODY_REQUIRED);
        }
        if (query == null) {
            throw new IllegalArgumentException(QUERY_REQUIRED);
        }
        if (pathVariables == null) {
            throw new IllegalArgumentException(PATH_VARIABLES_REQUIRED);
        }
        query = Map.copyOf(query);
        pathVariables = Map.copyOf(pathVariables);
    }

    public static HttpRequest create(HttpRequest draft) {
        return new HttpRequest(
                draft.method(), draft.path(), draft.body(), draft.query(), draft.pathVariables());
    }
}
